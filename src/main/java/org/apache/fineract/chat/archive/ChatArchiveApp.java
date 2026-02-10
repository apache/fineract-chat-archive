/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.chat.archive;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ChatArchiveApp {

    private static final Logger LOG = Logger.getLogger(ChatArchiveApp.class.getName());
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter
            .ofPattern("HH:mm:ss");

    public static void main(String[] args) {
        ArchiveConfig config = ArchiveConfig.fromEnv();

        String slackToken = config.slackToken();
        if (slackToken.isEmpty()) {
            LOG.severe("Missing/invalid required env var: " + ArchiveConfig.SLACK_TOKEN_ENV);
            System.exit(1);
        }

        if (config.channelAllowlist().isEmpty()) {
            LOG.severe("Missing/invalid required env var: " + ArchiveConfig.CHANNELS_ALLOWLIST_ENV);
            System.exit(1);
        }

        LOG.info("Using state dir [" + config.stateDir() + "]");
        LOG.info("Using output dir [" + config.outputDir() + "]");
        LOG.info("Loaded config for " + config.channelAllowlist().size() + " channel(s).");
        LOG.info("Will fetch messages for the past " + config.lookbackDays() + " day(s).");

        SlackApiClient slackApiClient = new SlackApiClient();
        SlackApiClient.AuthTestResponse authResponse;
        try {
            authResponse = slackApiClient.authTest(config.slackToken());
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Slack auth.test call failed.", ex);
            System.exit(1);
            return; // or authResponse might not be initialized
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOG.log(Level.SEVERE, "Slack auth.test call interrupted.", ex);
            System.exit(1);
            return; // or authResponse might not be initialized
        }

        if (!authResponse.ok()) {
            LOG.severe("Slack auth.test not ok: " + authResponse.error());
            System.exit(1);
        }

        LOG.info("Slack auth.test succeeded for team " + authResponse.team() + ".");

        SlackApiClient.ConversationsListResponse channelsResponse;
        try {
            channelsResponse = slackApiClient.listPublicChannels(config.slackToken());
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Slack conversations.list call failed.", ex);
            System.exit(1);
            return; // or channelsResponse might not be initialized
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOG.log(Level.SEVERE, "Slack conversations.list call interrupted.", ex);
            System.exit(1);
            return; // or channelsResponse might not be initialized
        }

        if (!channelsResponse.ok()) {
            LOG.severe("Slack conversations.list not ok: " + channelsResponse.error());
            System.exit(1);
        }

        List<SlackApiClient.SlackChannel> channels = channelsResponse.channels();
        ChannelResolver.ChannelResolution resolution = ChannelResolver.resolve(
                config.channelAllowlist(), channels);

        if (!resolution.missing().isEmpty()) {
            LOG.warning("Allowlisted channel(s) not found: "
                    + String.join(", ", resolution.missing()));
        }

        if (resolution.resolved().isEmpty()) {
            LOG.severe("No allowlisted channels resolved. Skipping archive update.");
            System.exit(1);
        }

        LOG.info("Resolved " + resolution.resolved().size() + " channel(s).");

        Instant windowStart = Instant.now()
                .minus(Duration.ofDays(config.lookbackDays()));
        String windowOldest = SlackTimestamp.formatEpochSecond(windowStart.getEpochSecond());

        CursorStore cursorStore = new CursorStore(config.stateDir());
        CursorStore.CursorState cursorState = loadCursorState(cursorStore);
        Map<String, String> cursors = new HashMap<>(cursorState.channels());

        Path dailyRoot = config.outputDir().resolve("daily");
        Map<String, String> permalinkCache = new HashMap<>();
        Map<String, String> userCache = new HashMap<>();
        Map<String, List<SlackMessage>> threadRepliesCache = new HashMap<>();
        boolean anyRendered = false;

        for (SlackApiClient.SlackChannel channel : resolution.resolved()) {
            String channelId = channel.id();
            String oldest = determineOldestTs(windowStart, windowOldest,
                    cursors.get(channelId));
            SlackApiClient.ConversationsHistoryResponse historyResponse;
            try {
                historyResponse = slackApiClient.listChannelMessages(config.slackToken(), channelId,
                        oldest);
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, "Slack conversations.history call failed for channel "
                        + channel.name() + ".", ex);
                continue;
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                LOG.log(Level.SEVERE, "Slack conversations.history call interrupted for channel "
                        + channel.name() + ".", ex);
                continue;
            }

            if (!historyResponse.ok()) {
                LOG.warning("Slack conversations.history not ok for channel " + channel.name()
                        + ": " + historyResponse.error());
                continue;
            }

            List<SlackMessage> messages = new ArrayList<>(historyResponse.messages());
            messages.sort((left, right) -> SlackTimestamp.compare(left.ts(), right.ts()));
            String latestTs = updateCursor(cursors.get(channelId), messages);
            if (latestTs != null) {
                cursors.put(channelId, latestTs);
            }

            Map<LocalDate, List<SlackMessage>> grouped = groupByDate(messages);
            for (Map.Entry<LocalDate, List<SlackMessage>> entry : grouped.entrySet()) {
                LocalDate date = entry.getKey();
                List<MarkdownRenderer.Row> rows = toRows(entry.getValue(), channelId,
                        config.slackToken(), slackApiClient, permalinkCache, userCache,
                        threadRepliesCache);
                String page = MarkdownRenderer.renderDailyPage(channel.name(), date, rows);
                Path pagePath = dailyRoot.resolve(channel.name()).resolve(date + ".md");
                try {
                    boolean changed = FileWriterUtil.writeIfChanged(pagePath, page);
                    anyRendered = anyRendered || changed;
                } catch (IOException ex) {
                    LOG.log(Level.SEVERE, "Failed to write archive for channel "
                            + channel.name() + " on " + date + ".", ex);
                }
            }
        }

        if (saveCursorState(cursorStore, cursors)) {
            anyRendered = true;
        }

        if (renderIndexes(dailyRoot)) {
            anyRendered = true;
        }

        if (!anyRendered) {
            LOG.info("No changes detected. Archive output unchanged.");
        }
    }

    private static CursorStore.CursorState loadCursorState(CursorStore cursorStore) {
        try {
            return cursorStore.load().orElseGet(CursorStore.CursorState::empty);
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Failed to read cursor state. Starting fresh.", ex);
            return CursorStore.CursorState.empty();
        }
    }

    private static boolean saveCursorState(CursorStore cursorStore, Map<String, String> cursors) {
        try {
            cursorStore.save(new CursorStore.CursorState(Map.copyOf(cursors)));
            return true;
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Failed to write cursor state.", ex);
            return false;
        }
    }

    private static String determineOldestTs(Instant windowStart, String windowOldest,
            String cursorTs) {
        if (cursorTs == null || cursorTs.isBlank()) {
            return windowOldest;
        }
        Instant cursorInstant = SlackTimestamp.toInstant(cursorTs);
        if (cursorInstant.isBefore(windowStart)) {
            return cursorTs;
        }
        return windowOldest;
    }

    private static String updateCursor(String current, List<SlackMessage> messages) {
        String latest = current;
        for (SlackMessage message : messages) {
            if (message.ts() == null) {
                continue;
            }
            if (latest == null || SlackTimestamp.compare(message.ts(), latest) > 0) {
                latest = message.ts();
            }
        }
        return latest;
    }

    private static Map<LocalDate, List<SlackMessage>> groupByDate(List<SlackMessage> messages) {
        Map<LocalDate, List<SlackMessage>> grouped = new TreeMap<>();
        Set<String> parentSet = new HashSet<>();
        for (SlackMessage message : messages) {
            if (message.ts() == null) {
                continue;
            }
            if (message.threadTs() == null || message.threadTs().equals(message.ts())) {
                parentSet.add(message.ts());
            }
        }
        for (SlackMessage message : messages) {
            if (message.ts() == null) {
                continue;
            }
            if (isReply(message) && parentSet.contains(message.threadTs())) {
                continue;
            }
            Instant instant = SlackTimestamp.toInstant(message.ts());
            LocalDate date = instant.atZone(ZoneOffset.UTC).toLocalDate();
            grouped.computeIfAbsent(date, key -> new ArrayList<>()).add(message);
        }
        return grouped;
    }

    private static List<MarkdownRenderer.Row> toRows(List<SlackMessage> messages, String channelId,
            String token, SlackApiClient slackApiClient, Map<String, String> permalinkCache,
            Map<String, String> userCache, Map<String, List<SlackMessage>> threadRepliesCache) {
        List<MarkdownRenderer.Row> rows = new ArrayList<>();
        Map<String, List<SlackMessage>> repliesByParent = collectReplies(messages);
        Set<String> parentSet = collectParentIds(messages);
        for (SlackMessage message : messages) {
            if (message.ts() == null) {
                continue;
            }
            if (isReply(message)) {
                if (parentSet.contains(message.threadTs())) {
                    continue;
                }
                rows.add(toRow(message, channelId, token, slackApiClient, permalinkCache,
                        userCache, "-> "));
                continue;
            }
            rows.add(toRow(message, channelId, token, slackApiClient, permalinkCache, userCache,
                    ""));
            if (message.threadTs() != null && message.threadTs().equals(message.ts())) {
                List<SlackMessage> replies = resolveThreadReplies(channelId, message.threadTs(),
                        repliesByParent, threadRepliesCache, slackApiClient, token);
                for (SlackMessage reply : replies) {
                    rows.add(toRow(reply, channelId, token, slackApiClient, permalinkCache,
                            userCache, "-> "));
                }
            }
        }
        return rows;
    }

    private static MarkdownRenderer.Row toRow(SlackMessage message, String channelId, String token,
            SlackApiClient slackApiClient, Map<String, String> permalinkCache,
            Map<String, String> userCache, String prefix) {
        Instant instant = SlackTimestamp.toInstant(message.ts());
        String time = TIME_FORMATTER.format(instant.atZone(ZoneOffset.UTC));
        String user = resolveUser(message, token, slackApiClient, userCache);
        String text = message.text() == null ? "" : message.text();
        String permalink = resolvePermalink(channelId, message.ts(), token, slackApiClient,
                permalinkCache);
        return new MarkdownRenderer.Row(time, user, prefix + text, permalink);
    }

    private static boolean isReply(SlackMessage message) {
        return message.threadTs() != null && !message.threadTs().equals(message.ts());
    }

    private static Set<String> collectParentIds(List<SlackMessage> messages) {
        Set<String> parentSet = new HashSet<>();
        for (SlackMessage message : messages) {
            if (message.ts() == null) {
                continue;
            }
            if (message.threadTs() == null || message.threadTs().equals(message.ts())) {
                parentSet.add(message.ts());
            }
        }
        return parentSet;
    }

    private static Map<String, List<SlackMessage>> collectReplies(List<SlackMessage> messages) {
        Map<String, List<SlackMessage>> repliesByParent = new HashMap<>();
        for (SlackMessage message : messages) {
            if (message.ts() == null || message.threadTs() == null
                    || message.threadTs().equals(message.ts())) {
                continue;
            }
            repliesByParent.computeIfAbsent(message.threadTs(), key -> new ArrayList<>())
                    .add(message);
        }
        return repliesByParent;
    }

    private static List<SlackMessage> resolveThreadReplies(String channelId, String threadTs,
            Map<String, List<SlackMessage>> repliesByParent,
            Map<String, List<SlackMessage>> repliesCache, SlackApiClient slackApiClient,
            String token) {
        if (repliesCache.containsKey(threadTs)) {
            return repliesCache.get(threadTs);
        }

        List<SlackMessage> replies = new ArrayList<>(repliesByParent.getOrDefault(threadTs,
                List.of()));
        Set<String> replyIds = new HashSet<>();
        for (SlackMessage message : replies) {
            if (message.ts() != null) {
                replyIds.add(message.ts());
            }
        }

        SlackApiClient.ConversationsRepliesResponse response;
        try {
            response = slackApiClient.listThreadReplies(token, channelId, threadTs);
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Slack conversations.replies call failed.", ex);
            repliesCache.put(threadTs, List.copyOf(replies));
            return replies;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOG.log(Level.WARNING, "Slack conversations.replies call interrupted.", ex);
            repliesCache.put(threadTs, List.copyOf(replies));
            return replies;
        }

        if (!response.ok()) {
            LOG.warning("Slack conversations.replies not ok: " + response.error());
            repliesCache.put(threadTs, List.copyOf(replies));
            return replies;
        }

        for (SlackMessage message : response.messages()) {
            if (message.ts() == null || message.ts().equals(threadTs)) {
                continue;
            }
            if (replyIds.add(message.ts())) {
                replies.add(message);
            }
        }
        replies.sort((left, right) -> SlackTimestamp.compare(left.ts(), right.ts()));
        List<SlackMessage> merged = List.copyOf(replies);
        repliesCache.put(threadTs, merged);
        return merged;
    }

    private static String resolveUser(SlackMessage message, String token,
            SlackApiClient slackApiClient, Map<String, String> userCache) {
        if (message.user() != null && !message.user().isBlank()) {
            return resolveUserDisplayName(message.user(), token, slackApiClient, userCache);
        }
        if (message.botId() != null && !message.botId().isBlank()) {
            return "bot:" + message.botId();
        }
        return "unknown";
    }

    private static String resolveUserDisplayName(String userId, String token,
            SlackApiClient slackApiClient, Map<String, String> userCache) {
        if (userCache.containsKey(userId)) {
            return userCache.get(userId);
        }
        SlackApiClient.UserInfoResponse response;
        try {
            response = slackApiClient.getUserInfo(token, userId);
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Slack users.info call failed.", ex);
            userCache.put(userId, userId);
            return userId;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOG.log(Level.WARNING, "Slack users.info call interrupted.", ex);
            userCache.put(userId, userId);
            return userId;
        }

        if (!response.ok() || response.user() == null) {
            userCache.put(userId, userId);
            return userId;
        }

        String displayName = UserDisplayNameResolver.resolve(response.user());
        userCache.put(userId, displayName);
        return displayName;
    }

    private static String resolvePermalink(String channelId, String messageTs, String token,
            SlackApiClient slackApiClient, Map<String, String> permalinkCache) {
        if (messageTs == null || messageTs.isBlank()) {
            return null;
        }
        String cacheKey = channelId + ":" + messageTs;
        if (permalinkCache.containsKey(cacheKey)) {
            return permalinkCache.get(cacheKey);
        }
        SlackApiClient.PermalinkResponse response;
        try {
            response = slackApiClient.getPermalink(token, channelId, messageTs);
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Slack chat.getPermalink call failed.", ex);
            permalinkCache.put(cacheKey, null);
            return null;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOG.log(Level.WARNING, "Slack chat.getPermalink call interrupted.", ex);
            permalinkCache.put(cacheKey, null);
            return null;
        }
        if (!response.ok()) {
            LOG.warning("Slack chat.getPermalink not ok: " + response.error());
            permalinkCache.put(cacheKey, null);
            return null;
        }
        permalinkCache.put(cacheKey, response.permalink());
        return response.permalink();
    }

    private static boolean renderIndexes(Path dailyRoot) {
        boolean changed = false;
        try {
            List<String> channels = IndexRenderer.listChannels(dailyRoot);
            for (String channel : channels) {
                List<LocalDate> dates = IndexRenderer.listDates(dailyRoot.resolve(channel));
                String index = MarkdownRenderer.renderChannelIndex(channel, dates);
                Path indexPath = dailyRoot.resolve(channel).resolve("index.md");
                changed = FileWriterUtil.writeIfChanged(indexPath, index) || changed;
            }
            String globalIndex = MarkdownRenderer.renderGlobalIndex(channels);
            Path globalPath = dailyRoot.getParent().resolve("index.md");
            changed = FileWriterUtil.writeIfChanged(globalPath, globalIndex) || changed;
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Failed to write index files.", ex);
        }
        return changed;
    }
}

