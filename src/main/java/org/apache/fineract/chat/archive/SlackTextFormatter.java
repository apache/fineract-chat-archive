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

import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SlackTextFormatter {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("<([^>]+)>");
    private static final Pattern EMOJI_PATTERN = Pattern.compile(":([a-zA-Z0-9_+\\-]+):");
    private static final Map<String, String> EMOJI_MAP = Map.ofEntries(
            Map.entry("wave", "👋"),
            Map.entry("thumbsup", "👍"),
            Map.entry("+1", "👍"),
            Map.entry("thumbsdown", "👎"),
            Map.entry("-1", "👎"),
            Map.entry("slightly_smiling_face", "🙂"),
            Map.entry("smile", "😄"),
            Map.entry("grin", "😁"),
            Map.entry("wink", "😉"),
            Map.entry("face_with_monocle", "🧐"),
            Map.entry("joy", "😂"),
            Map.entry("sweat_smile", "😅"),
            Map.entry("sob", "😭"),
            Map.entry("heart", "♥️"),
            Map.entry("tada", "🎉"),
            Map.entry("clap", "👏"),
            Map.entry("pray", "🙏"),
            Map.entry("fire", "🔥"),
            Map.entry("eyes", "👀"),
            Map.entry("white_check_mark", "✅"),
            Map.entry("open_mouth", "😮"),
            Map.entry("saluting_face", "🫡"),
            Map.entry("dancer", "💃"),
            Map.entry("raised_hands", "🙌"));

    private SlackTextFormatter() {}

    static String format(String text, Function<String, String> userResolver) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String withTokens = replaceTokens(text, userResolver);
        return replaceEmoji(withTokens);
    }

    static String resolveEmoji(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return EMOJI_MAP.get(code);
    }

    private static String replaceTokens(String text, Function<String, String> userResolver) {
        Matcher matcher = TOKEN_PATTERN.matcher(text);
        StringBuilder builder = new StringBuilder();
        int cursor = 0;
        while (matcher.find()) {
            builder.append(escapeHtml(text.substring(cursor, matcher.start())));
            String token = matcher.group(1);
            builder.append(formatToken(token, userResolver));
            cursor = matcher.end();
        }
        builder.append(escapeHtml(text.substring(cursor)));
        return builder.toString();
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
        if (!label.startsWith("@")) {
            label = "@" + label;
        }
        return escapeHtml(label);
    }

    private static String formatChannel(String token) {
        String[] parts = token.split("\\|", 2);
        String label = parts.length > 1 ? parts[1] : parts[0];
        if (!label.startsWith("#")) {
            label = "#" + label;
        }
        return escapeHtml(label);
    }

    private static String formatSpecial(String token) {
        String[] parts = token.split("\\|", 2);
        String label = parts.length > 1 ? parts[1] : parts[0];
        if (!label.startsWith("@")) {
            label = "@" + label;
        }
        return escapeHtml(label);
    }

    private static String formatLink(String token) {
        String[] parts = token.split("\\|", 2);
        String url = parts[0].trim();
        String label = parts.length > 1 ? parts[1] : parts[0];
        if (label == null || label.isBlank()) {
            label = url;
        }
        if (!isSupportedHref(url)) {
            return escapeHtml(label);
        }
        return "<a class=\"archive-link\" href=\"" + escapeHtmlAttribute(url) + "\">"
                + escapeHtml(label) + "</a>";
    }

    private static boolean isSupportedHref(String href) {
        if (href == null || href.isBlank()) {
            return false;
        }
        String normalized = href.toLowerCase(Locale.ROOT);
        return normalized.startsWith("https://")
                || normalized.startsWith("http://")
                || normalized.startsWith("mailto:");
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

    private static String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String escapeHtmlAttribute(String value) {
        return escapeHtml(value);
    }
}
