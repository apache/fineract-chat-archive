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
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ChatArchiveApp {

    static final String SLACK_TOKEN_ENV = "SLACK_TOKEN";

    private static final Logger LOG = Logger.getLogger(ChatArchiveApp.class.getName());

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
        LOG.info("Loaded config for " + archiveConfig.channelAllowlist().size() + " channel(s).");

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
    }

    static Optional<String> readEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value);
    }
}

