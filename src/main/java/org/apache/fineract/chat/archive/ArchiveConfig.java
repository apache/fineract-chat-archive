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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

final class ArchiveConfig {

    static final String CHANNELS_ALLOWLIST_KEY = "channels.allowlist";
    static final String OUTPUT_DIR_KEY = "output.dir";

    private final List<String> channelAllowlist;
    private final Path outputDir;

    private ArchiveConfig(List<String> channelAllowlist, Path outputDir) {
        this.channelAllowlist = List.copyOf(channelAllowlist);
        this.outputDir = outputDir;
    }

    static Optional<ArchiveConfig> load(Path configPath) throws IOException {
        if (!Files.exists(configPath)) {
            return Optional.empty();
        }

        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(configPath)) {
            properties.load(inputStream);
        }

        String allowlist = properties.getProperty(CHANNELS_ALLOWLIST_KEY, "");
        List<String> channels = parseAllowlist(allowlist);
        if (channels.isEmpty()) {
            return Optional.empty();
        }

        String outputDirValue = properties.getProperty(OUTPUT_DIR_KEY, "docs").trim();
        Path outputDir = Path.of(outputDirValue);
        return Optional.of(new ArchiveConfig(channels, outputDir));
    }

    List<String> channelAllowlist() {
        return channelAllowlist;
    }

    Path outputDir() {
        return outputDir;
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
}

