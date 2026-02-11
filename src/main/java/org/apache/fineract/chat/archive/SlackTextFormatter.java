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

import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SlackTextFormatter {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("<([^>]+)>");
    private static final Pattern EMOJI_PATTERN = Pattern.compile(":([a-zA-Z0-9_+\\-]+):");
    private static final Map<String, String> EMOJI_MAP = Map.ofEntries(
            Map.entry("wave", "\uD83D\uDC4B"),
            Map.entry("thumbsup", "\uD83D\uDC4D"),
            Map.entry("+1", "\uD83D\uDC4D"),
            Map.entry("thumbsdown", "\uD83D\uDC4E"),
            Map.entry("-1", "\uD83D\uDC4E"),
            Map.entry("smile", "\uD83D\uDE04"),
            Map.entry("grin", "\uD83D\uDE01"),
            Map.entry("joy", "\uD83D\uDE02"),
            Map.entry("sob", "\uD83D\uDE2D"),
            Map.entry("heart", "\u2764\uFE0F"),
            Map.entry("tada", "\uD83C\uDF89"),
            Map.entry("clap", "\uD83D\uDC4F"),
            Map.entry("pray", "\uD83D\uDE4F"),
            Map.entry("fire", "\uD83D\uDD25"),
            Map.entry("eyes", "\uD83D\uDC40"),
            Map.entry("white_check_mark", "\u2705"));

    private SlackTextFormatter() {}

    static String format(String text, Function<String, String> userResolver) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String withTokens = replaceTokens(text, userResolver);
        return replaceEmoji(withTokens);
    }

    private static String replaceTokens(String text, Function<String, String> userResolver) {
        Matcher matcher = TOKEN_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String token = matcher.group(1);
            String replacement = formatToken(token, userResolver);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String formatToken(String token, Function<String, String> userResolver) {
        if (token.startsWith("@")) {
            return formatUser(token.substring(1), userResolver);
        }
        if (token.startsWith("#")) {
            return formatChannel(token.substring(1));
        }
        if (token.startsWith("!")) {
            return formatSpecial(token.substring(1));
        }
        return formatLink(token);
    }

    private static String formatUser(String token, Function<String, String> userResolver) {
        String[] parts = token.split("\\|", 2);
        String userId = parts[0];
        String label = parts.length > 1 ? parts[1] : null;
        if (label == null || label.isBlank()) {
            label = userResolver.apply(userId);
        }
        if (label == null || label.isBlank()) {
            label = userId;
        }
        if (label.startsWith("@")) {
            return label;
        }
        return "@" + label;
    }

    private static String formatChannel(String token) {
        String[] parts = token.split("\\|", 2);
        String label = parts.length > 1 ? parts[1] : parts[0];
        if (label.startsWith("#")) {
            return label;
        }
        return "#" + label;
    }

    private static String formatSpecial(String token) {
        String[] parts = token.split("\\|", 2);
        String label = parts.length > 1 ? parts[1] : parts[0];
        if (label.startsWith("@")) {
            return label;
        }
        return "@" + label;
    }

    private static String formatLink(String token) {
        String[] parts = token.split("\\|", 2);
        String url = parts[0];
        if (parts.length == 1) {
            return url;
        }
        String label = parts[1];
        return "[" + label + "](" + url + ")";
    }

    private static String replaceEmoji(String text) {
        Matcher matcher = EMOJI_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String code = matcher.group(1);
            String emoji = EMOJI_MAP.get(code);
            if (emoji == null) {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group(0)));
            } else {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(emoji));
            }
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
