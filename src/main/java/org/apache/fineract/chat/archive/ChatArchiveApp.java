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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ChatArchiveApp {

    static final String SLACK_TOKEN_ENV = "SLACK_TOKEN";

    private static final Logger LOG = Logger.getLogger(ChatArchiveApp.class.getName());
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter
            .ofPattern("HH:mm:ss");

    private ChatArchiveApp() {}

    public static void main(String[] args) {
        Optional<String> slackToken = readEnv(SLACK_TOKEN_ENV);
        if (slackToken.isEmpty()) {
            LOG.info("SLACK_TOKEN is not set. Skipping archive update.");
            return;
        }

        Path configPath = Path.of("config", "archive.properties");
        Optional<ArchiveConfig> config;
        try {
            config = ArchiveConfig.load(configPath);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Failed to read config at " + configPath + ".", ex);
            return;
        }

        if (config.isEmpty()) {
            LOG.info("Config file missing or channel allowlist empty. Skipping archive update.");
            return;
        }

        ArchiveConfig archiveConfig = config.get();
        LOG.info("Loaded config for " + archiveConfig.channelAllowlist().size()
                + " channel(s).");

        SlackApiClient slackApiClient = new SlackApiClient();
        SlackApiClient.AuthTestResponse authResponse;
        try {
            authResponse = slackApiClient.authTest(slackToken.get());
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Slack auth.test call failed.", ex);
            return;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOG.log(Level.SEVERE, "Slack auth.test call interrupted.", ex);
            return;
        }

        if (!authResponse.ok()) {
            LOG.warning("Slack auth.test failed: " + authResponse.error());
            return;
        }

        LOG.info("Slack auth.test succeeded for team " + authResponse.team() + ".");

        SlackApiClient.ConversationsListResponse channelsResponse;
        try {
            channelsResponse = slackApiClient.listPublicChannels(slackToken.get());
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Slack conversations.list call failed.", ex);
            return;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOG.log(Level.SEVERE, "Slack conversations.list call interrupted.", ex);
            return;
        }

        if (!channelsResponse.ok()) {
            LOG.warning("Slack conversations.list failed: " + channelsResponse.error());
            return;
        }

        List<SlackApiClient.SlackChannel> channels = channelsResponse.channels();
        ChannelResolver.ChannelResolution resolution = ChannelResolver.resolve(
                archiveConfig.channelAllowlist(), channels);

        if (!resolution.missing().isEmpty()) {
            LOG.warning("Allowlisted channel(s) not found: "
                    + String.join(", ", resolution.missing()));
        }

        if (resolution.resolved().isEmpty()) {
            LOG.warning("No allowlisted channels resolved. Skipping archive update.");
            return;
        }

        LOG.info("Resolved " + resolution.resolved().size() + " channel(s).");

        Instant windowStart = Instant.now()
                .minus(Duration.ofDays(archiveConfig.lookbackDays()));
        String windowOldest = SlackTimestamp.formatEpochSecond(windowStart.getEpochSecond());

        CursorStore cursorStore = new CursorStore(archiveConfig.stateDir());
        CursorStore.CursorState cursorState = loadCursorState(cursorStore);
        Map<String, String> cursors = new HashMap<>(cursorState.channels());

        Path dailyRoot = archiveConfig.outputDir().resolve("daily");
        Map<String, String> permalinkCache = new HashMap<>();
        boolean anyRendered = false;

        for (SlackApiClient.SlackChannel channel : resolution.resolved()) {
            String channelId = channel.id();
            String oldest = determineOldestTs(windowStart, windowOldest,
                    cursors.get(channelId));
            SlackApiClient.ConversationsHistoryResponse historyResponse;
            try {
                historyResponse = slackApiClient.listChannelMessages(slackToken.get(), channelId,
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
                LOG.warning("Slack conversations.history failed for channel " + channel.name()
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
                        slackToken.get(), slackApiClient, permalinkCache);
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

    static Optional<String> readEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value);
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
        for (SlackMessage message : messages) {
            if (message.ts() == null) {
                continue;
            }
            Instant instant = SlackTimestamp.toInstant(message.ts());
            LocalDate date = instant.atZone(ZoneOffset.UTC).toLocalDate();
            grouped.computeIfAbsent(date, key -> new ArrayList<>()).add(message);
        }
        return grouped;
    }

    private static List<MarkdownRenderer.Row> toRows(List<SlackMessage> messages, String channelId,
            String token, SlackApiClient slackApiClient, Map<String, String> permalinkCache) {
        List<MarkdownRenderer.Row> rows = new ArrayList<>();
        for (SlackMessage message : messages) {
            Instant instant = SlackTimestamp.toInstant(message.ts());
            String time = TIME_FORMATTER.format(instant.atZone(ZoneOffset.UTC));
            String user = resolveUser(message);
            String text = message.text() == null ? "" : message.text();
            String permalink = resolvePermalink(channelId, message.ts(), token, slackApiClient,
                    permalinkCache);
            rows.add(new MarkdownRenderer.Row(time, user, text, permalink));
        }
        return rows;
    }

    private static String resolveUser(SlackMessage message) {
        if (message.user() != null && !message.user().isBlank()) {
            return message.user();
        }
        if (message.botId() != null && !message.botId().isBlank()) {
            return message.botId();
        }
        return "unknown";
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
            LOG.warning("Slack chat.getPermalink failed: " + response.error());
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

