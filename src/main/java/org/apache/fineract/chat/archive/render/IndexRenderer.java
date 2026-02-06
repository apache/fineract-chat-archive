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
package org.apache.fineract.chat.archive.render;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class IndexRenderer {

    private IndexRenderer() {}

    public static List<String> listChannels(Path dailyRoot) throws IOException {
        if (!Files.exists(dailyRoot)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(dailyRoot)) {
            return stream.filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        }
    }

    public static List<LocalDate> listDates(Path channelDir) throws IOException {
        if (!Files.exists(channelDir)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(channelDir)) {
            return stream.filter(path -> path.getFileName().toString().endsWith(".md"))
                    .filter(path -> !path.getFileName().toString().equalsIgnoreCase("index.md"))
                    .map(path -> path.getFileName().toString().replace(".md", ""))
                    .map(IndexRenderer::parseDate)
                    .filter(date -> date != null)
                    .sorted(Comparator.reverseOrder())
                    .collect(Collectors.toList());
        }
    }

    private static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (Exception ex) {
            return null;
        }
    }
}
