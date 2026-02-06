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

import java.util.List;
import org.junit.jupiter.api.Test;

class ChannelResolverTest {

    @Test
    void resolveMatchesAllowlistIgnoringCase() {
        List<String> allowlist = List.of("fineract", "Dev");
        List<SlackApiClient.SlackChannel> channels = List.of(
                new SlackApiClient.SlackChannel("C1", "fineract"),
                new SlackApiClient.SlackChannel("C2", "dev"));

        ChannelResolver.ChannelResolution resolution = ChannelResolver.resolve(allowlist,
                channels);

        assertEquals(2, resolution.resolved().size());
        assertTrue(resolution.missing().isEmpty());
    }

    @Test
    void resolveReportsMissingChannels() {
        List<String> allowlist = List.of("fineract", "unknown");
        List<SlackApiClient.SlackChannel> channels = List.of(
                new SlackApiClient.SlackChannel("C1", "fineract"));

        ChannelResolver.ChannelResolution resolution = ChannelResolver.resolve(allowlist,
                channels);

        assertEquals(1, resolution.resolved().size());
        assertEquals(List.of("unknown"), resolution.missing());
    }
}
