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
package org.apache.fineract.chat.archive.slack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ChannelResolver {

    private ChannelResolver() {}

    public static ChannelResolution resolve(List<String> allowlist,
            List<SlackApiClient.SlackChannel> channels) {
        Map<String, SlackApiClient.SlackChannel> channelsByName = new HashMap<>();
        for (SlackApiClient.SlackChannel channel : channels) {
            String normalizedName = normalize(channel.name());
            if (!normalizedName.isEmpty()) {
                channelsByName.putIfAbsent(normalizedName, channel);
            }
        }

        List<SlackApiClient.SlackChannel> resolved = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String allowed : allowlist) {
            String normalizedAllowed = normalize(allowed);
            SlackApiClient.SlackChannel channel = channelsByName.get(normalizedAllowed);
            if (channel == null) {
                missing.add(allowed);
            } else {
                resolved.add(channel);
            }
        }

        return new ChannelResolution(List.copyOf(resolved), List.copyOf(missing));
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public record ChannelResolution(List<SlackApiClient.SlackChannel> resolved,
            List<String> missing) {
    }
}
