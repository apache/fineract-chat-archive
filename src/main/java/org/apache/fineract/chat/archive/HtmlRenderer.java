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

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class HtmlRenderer {

    private static final String ROOT_STYLESHEET_PATH = "assets/chat-archive.css";
    private static final String CHANNEL_STYLESHEET_PATH = "../../assets/chat-archive.css";
    private static final String DAILY_STYLESHEET_PATH = "../../../assets/chat-archive.css";
    private static final Pattern UNORDERED_LIST_PATTERN = Pattern.compile("^\\s*(?:[-*]|\\u2022)\\s+(.+)$");
    private static final Pattern ORDERED_LIST_PATTERN = Pattern.compile("^\\s*\\d+\\.\\s+(.+)$");
    private static final Pattern ANCHOR_PATTERN = Pattern.compile("(<a\\b[^>]*>.*?</a>)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern BOLD_PATTERN = Pattern.compile("(?<!\\w)\\*(\\S(?:.*?\\S)?)\\*(?!\\w)");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("(?<!\\w)_(\\S(?:.*?\\S)?)_(?!\\w)");
    private static final Pattern STRIKE_PATTERN = Pattern.compile("(?<!\\w)~(\\S(?:.*?\\S)?)~(?!\\w)");

    private HtmlRenderer() {}

    static String renderDailyPage(String channelName, LocalDate date, List<Row> rows) {
        String safeChannel = escapeHtml(normalize(channelName));
        String safeDate = escapeHtml(date.toString());

        StringBuilder body = new StringBuilder();
        body.append("<header class=\"archive-header\">\n")
                .append("<p class=\"archive-breadcrumb\">")
                .append("<a href=\"../../../\">Channels</a> / ")
                .append("<a href=\"../\">#").append(safeChannel).append("</a> / ")
                .append(safeDate)
                .append("</p>")
                .append("<h1>#").append(safeChannel).append(" ").append(safeDate).append("</h1>")
                .append("</header>");
        body.append("<section class=\"archive-log\">");
        for (int index = 0; index < rows.size(); index++) {
            Row row = rows.get(index);
            if (row.isReply()) {
                body.append("<section class=\"archive-thread\" aria-label=\"Thread replies\">");
                while (index < rows.size() && rows.get(index).isReply()) {
                    body.append(renderMessageRow(rows.get(index)));
                    index++;
                }
                body.append("</section>");
                index--;
                continue;
            }

            body.append(renderMessageRow(row));
            int replyIndex = index + 1;
            if (replyIndex < rows.size() && rows.get(replyIndex).isReply()) {
                body.append("<section class=\"archive-thread\" aria-label=\"Thread replies\">\n");
                while (replyIndex < rows.size() && rows.get(replyIndex).isReply()) {
                    body.append(renderMessageRow(rows.get(replyIndex)));
                    replyIndex++;
                }
                body.append("</section>");
                index = replyIndex - 1;
            }
        }
        body.append("</section>");

        return renderDocument("#" + normalize(channelName) + " " + date,
                DAILY_STYLESHEET_PATH, body.toString());
    }

    static String renderChannelIndex(String channelName, List<LocalDate> dates) {
        String safeChannel = escapeHtml(normalize(channelName));

        StringBuilder body = new StringBuilder();
        body.append("<header class=\"archive-header\">\n")
                .append("<p class=\"archive-breadcrumb\">\n")
                .append("<a href=\"../../\">Channels</a> / #").append(safeChannel)
                .append("</p>\n")
                .append("<h1>#").append(safeChannel).append("</h1>\n")
                .append("</header>\n");
        body.append("<section class=\"archive-index\">\n")
                .append("<h2>Days</h2>\n")
                .append("<ul class=\"archive-day-list\">\n");
        for (LocalDate date : dates) {
            String safeDate = escapeHtml(date.toString());
            body.append("<li><a href=\"").append(safeDate).append("/\">\n")
                    .append(safeDate)
                    .append("</a></li>\n");
        }
        body.append("</ul>\n")
                .append("</section>");

        return renderDocument("#" + normalize(channelName), CHANNEL_STYLESHEET_PATH,
                body.toString());
    }

    static String renderGlobalIndex(List<String> channels) {
        StringBuilder body = new StringBuilder();
        body.append("<header class=\"archive-header\">\n")
                .append("<h1>Chat Archive</h1>\n")
                .append("</header>\n");
        body.append("<section class=\"archive-index\">\n")
                .append("<h2>Channels</h2>\n")
                .append("<ul class=\"archive-channel-list\">\n");
        for (String channel : channels) {
            String safeChannel = escapeHtml(normalize(channel));
            body.append("<li><a href=\"daily/")
                    .append(safeChannel)
                    .append("/\">#")
                    .append(safeChannel)
                    .append("</a></li>\n");
        }
        body.append("</ul>\n")
                .append("</section>");

        return renderDocument("Chat Archive", ROOT_STYLESHEET_PATH, body.toString());
    }

    private static String renderDocument(String title, String stylesheetPath, String bodyContent) {
        StringBuilder builder = new StringBuilder();
        builder.append("<!doctype html>\n");
        builder.append("<html lang=\"en\">\n");
        builder.append("<head>\n");
        builder.append("  <meta charset=\"utf-8\">\n");
        builder.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
        builder.append("  <title>").append(escapeHtml(normalize(title))).append("</title>\n");
        builder.append("  <link rel=\"stylesheet\" href=\"")
                .append(stylesheetPath)
                .append("\">\n");
        builder.append("</head>\n");
        builder.append("<body>\n");
        builder.append("  <main class=\"archive-page\">\n");
        builder.append(bodyContent).append('\n');
        builder.append("  </main>\n");
        builder.append("</body>\n");
        builder.append("</html>\n");
        return builder.toString();
    }

    private static String renderMessageRow(Row row) {
        StringBuilder builder = new StringBuilder();
        builder.append("<article class=\"archive-message");
        if (row.isReply()) {
            builder.append(" archive-message-reply");
        }
        builder.append("\">\n");
        builder.append("<div class=\"archive-meta\">\n");
        if (row.isReply()) {
            builder.append("<span class=\"archive-reply-indicator\" aria-hidden=\"true\">&rarr;</span>\n");
            builder.append("<span class=\"archive-reply-label\">reply</span>\n");
        }
        builder.append(formatTimeCell(row));
        builder.append("<span class=\"archive-user\">\n")
                .append(escapeHtml(normalize(row.user())))
                .append("</span>\n");
        builder.append("</div>\n");
        builder.append("<div class=\"archive-text\">\n")
                .append(formatMessage(row.message()))
                .append("</div>\n");
        if (row.reactions() != null && !row.reactions().isEmpty()) {
            builder.append("<div class=\"archive-reactions\">");
            for (String reaction : row.reactions()) {
                builder.append("<span class=\"archive-reaction\">")
                        .append(escapeHtml(normalize(reaction)))
                        .append("</span>\n");
            }
            builder.append("</div>\n");
        }
        builder.append("</article>");
        return builder.toString();
    }

    private static String formatMessage(String value) {
        String normalized = normalize(value);
        if (normalized.isEmpty()) {
            return "";
        }

        String[] lines = normalized.split("\n", -1);
        StringBuilder builder = new StringBuilder();
        ListType listType = ListType.NONE;

        for (int index = 0; index < lines.length; index++) {
            String line = lines[index];
            Matcher unorderedMatcher = UNORDERED_LIST_PATTERN.matcher(line);
            Matcher orderedMatcher = ORDERED_LIST_PATTERN.matcher(line);
            if (unorderedMatcher.matches()) {
                listType = switchToList(builder, listType, ListType.UNORDERED);
                builder.append("<li>")
                        .append(applyInlineMarkup(unorderedMatcher.group(1).trim()))
                        .append("</li>\n");
                continue;
            }
            if (orderedMatcher.matches()) {
                listType = switchToList(builder, listType, ListType.ORDERED);
                builder.append("<li>")
                        .append(applyInlineMarkup(orderedMatcher.group(1).trim()))
                        .append("</li>\n");
                continue;
            }

            if (listType != ListType.NONE) {
                closeList(builder, listType);
                listType = ListType.NONE;
            }

            if (line.isBlank()) {
                builder.append("<br>\n");
                continue;
            }

            builder.append("<span class=\"archive-line\">\n")
                    .append(applyInlineMarkup(line))
                    .append("</span>\n");
            if (index < lines.length - 1) {
                builder.append("<br>");
            }
        }

        if (listType != ListType.NONE) {
            closeList(builder, listType);
        }
        return builder.toString();
    }

    private static String applyInlineMarkup(String line) {
        Matcher matcher = ANCHOR_PATTERN.matcher(line);
        StringBuilder builder = new StringBuilder();
        int cursor = 0;
        while (matcher.find()) {
            builder.append(applyInlineMarkupToText(line.substring(cursor, matcher.start())));
            builder.append(matcher.group(1));
            cursor = matcher.end();
        }
        builder.append(applyInlineMarkupToText(line.substring(cursor)));
        return builder.toString();
    }

    private static String applyInlineMarkupToText(String text) {
        String formatted = replaceInlinePattern(text, BOLD_PATTERN, "strong");
        formatted = replaceInlinePattern(formatted, ITALIC_PATTERN, "em");
        formatted = replaceInlinePattern(formatted, STRIKE_PATTERN, "del");
        return formatted;
    }

    private static String replaceInlinePattern(String input, Pattern pattern, String tagName) {
        Matcher matcher = pattern.matcher(input);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String inner = matcher.group(1);
            matcher.appendReplacement(buffer, "<" + tagName + ">"
                    + Matcher.quoteReplacement(inner) + "</" + tagName + ">");
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static ListType switchToList(StringBuilder builder, ListType current,
            ListType target) {
        if (current == target) {
            return current;
        }
        if (current != ListType.NONE) {
            closeList(builder, current);
        }
        if (target == ListType.UNORDERED) {
            builder.append("<ul class=\"archive-list\">\n");
        } else if (target == ListType.ORDERED) {
            builder.append("<ol class=\"archive-list archive-list-numbered\">\n");
        }
        return target;
    }

    private static void closeList(StringBuilder builder, ListType type) {
        if (type == ListType.UNORDERED) {
            builder.append("</ul>\n");
        } else if (type == ListType.ORDERED) {
            builder.append("</ol>\n");
        }
    }

    private static String formatTimeCell(Row row) {
        String label = escapeHtml(normalize(row.timeAbbrev()));
        if (row.permalink() == null || row.permalink().isBlank()) {
            return "<span class=\"archive-time\">" + label + "</span>";
        }
        String href = escapeHtmlAttribute(normalize(row.permalink()));
        String title = escapeHtmlAttribute(normalize(row.rfcDatetime()));
        return "<a class=\"archive-time archive-time-link\" href=\"" + href + "\" title=\""
                + title + "\">" + label + "</a>";
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\r\n", "\n").replace("\r", "\n");
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

    private enum ListType {
        NONE,
        UNORDERED,
        ORDERED
    }

    record Row(boolean isReply, String timeAbbrev, String rfcDatetime, String user, String message, String permalink,
            List<String> reactions) {
    }
}
