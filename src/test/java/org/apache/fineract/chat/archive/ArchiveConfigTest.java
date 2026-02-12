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

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ArchiveConfigTest {

    @Test
    void nominalConfig() {

        ArchiveConfig config = ArchiveConfig.fromValues("dummy", "#fineract, dev", "docs",
                "state", "3");

        assertEquals(2, config.channelAllowlist().size());
        assertEquals("fineract", config.channelAllowlist().get(0));
        assertEquals("dev", config.channelAllowlist().get(1));
        assertEquals(Path.of("docs"), config.outputDir());
        assertEquals(Path.of("state"), config.stateDir());
        assertEquals(3, config.lookbackDays());
    }

    @Test
    void missingSlackToken() {
        ArchiveConfig config = ArchiveConfig.fromValues(" ",null, "docs", "state", "1");

        assertTrue(config.slackToken().isEmpty());
    }

    @Test
    void emptyAllowList() {
        ArchiveConfig config = ArchiveConfig.fromValues("dummy","", "docs", "state", "1");

        assertTrue(config.channelAllowlist().isEmpty());
    }

    @Test
    void fromValuesUsesDefaultLookbackDaysWhenInvalid() {
        ArchiveConfig config = ArchiveConfig.fromValues("dummy","#fineract", "docs", "state", "0");

        assertEquals(ArchiveConfig.DEFAULT_LOOKBACK_DAYS, config.lookbackDays());
    }

    @Test
    void fromValuesUsesDefaultsWhenOptionalValuesNull() {
        ArchiveConfig config = ArchiveConfig.fromValues("dummy", "#fineract", null, null, null);

        assertEquals(Path.of(ArchiveConfig.DEFAULT_OUTPUT_DIR), config.outputDir());
        assertEquals(Path.of(ArchiveConfig.DEFAULT_STATE_DIR), config.stateDir());
        assertEquals(ArchiveConfig.DEFAULT_LOOKBACK_DAYS, config.lookbackDays());
        assertEquals("", config.siteBaseUrl());
    }

    @Test
    void fromValuesNormalizesSiteBaseUrl() {
        ArchiveConfig config = ArchiveConfig.fromValues("dummy", "#fineract", "docs", "state", "1",
                "https://example.com/archive/");

        assertEquals("https://example.com/archive", config.siteBaseUrl());
    }
}
