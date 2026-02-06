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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ArchiveConfigTest {

    @Test
    void loadReturnsConfigWhenAllowlistPresent() throws Exception {
        Path tempFile = Files.createTempFile("archive", ".properties");
        String content = String.join("\n",
                "channels.allowlist=#fineract, dev",
                "output.dir=docs",
                "");
        Files.writeString(tempFile, content, StandardCharsets.UTF_8);

        Optional<ArchiveConfig> config = ArchiveConfig.load(tempFile);

        assertTrue(config.isPresent());
        assertEquals(2, config.get().channelAllowlist().size());
        assertEquals("fineract", config.get().channelAllowlist().get(0));
        assertEquals("dev", config.get().channelAllowlist().get(1));
        assertEquals(Path.of("docs"), config.get().outputDir());
    }

    @Test
    void loadReturnsEmptyWhenFileMissing() throws Exception {
        Path missingFile = Path.of("config",
                "missing-" + System.nanoTime() + ".properties");

        Optional<ArchiveConfig> config = ArchiveConfig.load(missingFile);

        assertTrue(config.isEmpty());
    }

    @Test
    void loadReturnsEmptyWhenAllowlistIsEmpty() throws Exception {
        Path tempFile = Files.createTempFile("archive-empty", ".properties");
        String content = String.join("\n",
                "channels.allowlist=",
                "output.dir=docs",
                "");
        Files.writeString(tempFile, content, StandardCharsets.UTF_8);

        Optional<ArchiveConfig> config = ArchiveConfig.load(tempFile);

        assertTrue(config.isEmpty());
    }
}

