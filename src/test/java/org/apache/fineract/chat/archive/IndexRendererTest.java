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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class IndexRendererTest {

    @TempDir
    Path tempDir;

    @Test
    void listChannelsReturnsSortedDirectoryNames() throws IOException {
        Path dailyRoot = tempDir.resolve("daily");
        Files.createDirectories(dailyRoot.resolve("zeta"));
        Files.createDirectories(dailyRoot.resolve("alpha"));

        List<String> channels = IndexRenderer.listChannels(dailyRoot);

        assertEquals(List.of("alpha", "zeta"), channels);
    }

    @Test
    void listDatesUsesDateDirectoriesInDescendingOrder() throws IOException {
        Path channelDir = tempDir.resolve("daily").resolve("fineract");
        Files.createDirectories(channelDir.resolve("2026-02-05"));
        Files.createDirectories(channelDir.resolve("2026-02-12"));
        Files.createDirectories(channelDir.resolve("not-a-date"));
        Files.writeString(channelDir.resolve("README.txt"), "ignore");

        List<LocalDate> dates = IndexRenderer.listDates(channelDir);

        assertEquals(List.of(LocalDate.parse("2026-02-12"), LocalDate.parse("2026-02-05")),
                dates);
    }
}
