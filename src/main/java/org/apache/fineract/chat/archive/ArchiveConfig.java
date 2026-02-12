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

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

final class ArchiveConfig {
    static final String SLACK_TOKEN_ENV = "SLACK_TOKEN";

    static final String CHANNELS_ALLOWLIST_ENV = "CHANNELS_ALLOWLIST";
    static final String OUTPUT_DIR_ENV = "OUTPUT_DIR";
    static final String STATE_DIR_ENV = "STATE_DIR";
    static final String LOOKBACK_DAYS_ENV = "LOOKBACK_DAYS";
    static final String SITE_BASE_URL_ENV = "SITE_BASE_URL";
    static final String LOG_LEVEL_ENV = "LOG_LEVEL";

    static final String DEFAULT_OUTPUT_DIR = "docs";
    static final String DEFAULT_STATE_DIR = "state";
    static final int DEFAULT_LOOKBACK_DAYS = 1;

    private final String slackToken;
    private final List<String> channelAllowlist;
    private final Path outputDir;
    private final Path stateDir;
    private final int lookbackDays;
    private final String siteBaseUrl;

    private ArchiveConfig(String slackToken, List<String> channelAllowlist, Path outputDir, Path stateDir,
            int lookbackDays, String siteBaseUrl) {
        this.slackToken = slackToken;
        this.channelAllowlist = List.copyOf(channelAllowlist);
        this.outputDir = outputDir;
        this.stateDir = stateDir;
        this.lookbackDays = lookbackDays;
        this.siteBaseUrl = siteBaseUrl;
    }

    static ArchiveConfig fromEnv() {
        return fromValues(System.getenv(SLACK_TOKEN_ENV), System.getenv(CHANNELS_ALLOWLIST_ENV), System.getenv(OUTPUT_DIR_ENV),
                System.getenv(STATE_DIR_ENV), System.getenv(LOOKBACK_DAYS_ENV), System.getenv(SITE_BASE_URL_ENV));
    }

    static ArchiveConfig fromValues(String slackTokenValue, String allowlist, String outputDirValue,
            String stateDirValue, String lookbackDaysValue) {
        return fromValues(slackTokenValue, allowlist, outputDirValue, stateDirValue, lookbackDaysValue, null);
    }

    static ArchiveConfig fromValues(String slackTokenValue, String allowlist, String outputDirValue,
            String stateDirValue, String lookbackDaysValue, String siteBaseUrlValue) {
        String slackToken = slackTokenValue != null ? slackTokenValue.trim() : "";
        List<String> channels = parseAllowlist(allowlist);
        Path outputDir = Path.of(outputDirValue != null ? outputDirValue.trim() : DEFAULT_OUTPUT_DIR);
        Path stateDir = Path.of(stateDirValue != null ? stateDirValue.trim() : DEFAULT_STATE_DIR);
        int lookbackDays = parseLookbackDays(lookbackDaysValue);
        String siteBaseUrl = normalizeSiteBaseUrl(siteBaseUrlValue);
        return new ArchiveConfig(slackToken, channels, outputDir, stateDir, lookbackDays, siteBaseUrl);
    }

    String slackToken() {
        return slackToken;
    }

    List<String> channelAllowlist() {
        return channelAllowlist;
    }

    Path outputDir() {
        return outputDir;
    }

    Path stateDir() {
        return stateDir;
    }

    int lookbackDays() {
        return lookbackDays;
    }

    String siteBaseUrl() {
        return siteBaseUrl;
    }

    private static List<String> parseAllowlist(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .map(ArchiveConfig::stripLeadingHash)
                .filter(token -> !token.isEmpty())
                .toList();
    }

    private static String stripLeadingHash(String value) {
        if (value.startsWith("#")) {
            return value.substring(1).trim();
        }
        return value;
    }

    private static int parseLookbackDays(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_LOOKBACK_DAYS;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : DEFAULT_LOOKBACK_DAYS;
        } catch (NumberFormatException ex) {
            return DEFAULT_LOOKBACK_DAYS;
        }
    }

    private static String normalizeSiteBaseUrl(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}

