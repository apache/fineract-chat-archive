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

class MarkdownRenderer {

    private static final String ROOT_STYLESHEET_PATH = "assets/chat-archive.css";
    private static final String DAILY_STYLESHEET_PATH = "../../assets/chat-archive.css";

    private MarkdownRenderer() {}

    static String renderDailyPage(String channelName, LocalDate date, List<Row> rows) {
        StringBuilder builder = new StringBuilder();
        builder.append("---\n");
        builder.append("title: \"#").append(channelName).append(" ").append(date).append("\"\n");
        builder.append("date: ").append(date).append('\n');
        builder.append("channel: ").append(channelName).append('\n');
        builder.append("permalink: /daily/").append(channelName).append("/").append(date).append("/\n");
        builder.append("---\n\n");
        appendStylesheetLink(builder, DAILY_STYLESHEET_PATH);
        builder.append('\n');
        builder.append("<section class=\"archive-log\">\n");
        for (Row row : rows) {
            builder.append(renderMessageRow(row)).append('\n');
        }
        builder.append("</section>\n");
        return builder.toString();
    }

    static String renderChannelIndex(String channelName, List<LocalDate> dates) {
        StringBuilder builder = new StringBuilder();
        builder.append("---\n");
        builder.append("title: \"#").append(channelName).append("\"\n");
        builder.append("channel: ").append(channelName).append('\n');
        builder.append("permalink: /daily/").append(channelName).append("/\n");
        builder.append("---\n\n");
        appendStylesheetLink(builder, DAILY_STYLESHEET_PATH);
        builder.append('\n');
        builder.append("<section class=\"archive-index\">\n");
        builder.append("<h2>Days</h2>\n");
        builder.append("<ul class=\"archive-day-list\">\n");
        for (LocalDate date : dates) {
            builder.append("<li><a href=\"").append(date).append("/\">")
                    .append(date)
                    .append("</a></li>\n");
        }
        builder.append("</ul>\n");
        builder.append("</section>\n");
        return builder.toString();
    }

    static String renderGlobalIndex(List<String> channels) {
        StringBuilder builder = new StringBuilder();
        builder.append("---\n");
        builder.append("title: \"Chat Archive\"\n");
        builder.append("permalink: /\n");
        builder.append("---\n\n");
        appendStylesheetLink(builder, ROOT_STYLESHEET_PATH);
        builder.append('\n');
        builder.append("<section class=\"archive-index\">\n");
        builder.append("<h2>Channels</h2>\n");
        builder.append("<ul class=\"archive-channel-list\">\n");
        for (String channel : channels) {
            builder.append("<li><a href=\"daily/")
                    .append(escapeHtml(channel))
                    .append("/\">#")
                    .append(escapeHtml(channel))
                    .append("</a></li>\n");
        }
        builder.append("</ul>\n");
        builder.append("</section>\n");
        return builder.toString();
    }

    private static String renderMessageRow(Row row) {
        StringBuilder builder = new StringBuilder();
        builder.append("<div class=\"archive-message");
        if (row.isReply()) {
            builder.append(" archive-message-reply");
        }
        builder.append("\">");
        if (row.isReply()) {
            builder.append("<span class=\"archive-reply-indicator\" aria-hidden=\"true\">-&gt;</span>");
        }
        builder.append(formatTimeCell(row));
        builder.append("<span class=\"archive-user\">")
                .append(escapeHtml(normalize(row.user())))
                .append("</span>");
        builder.append("<span class=\"archive-text\">")
                .append(formatMessage(row.message()))
                .append("</span>");
        builder.append("</div>");
        return builder.toString();
    }

    private static void appendStylesheetLink(StringBuilder builder, String stylesheetPath) {
        builder.append("<link rel=\"stylesheet\" href=\"")
                .append(stylesheetPath)
                .append("\">\n");
    }

    private static String formatMessage(String value) {
        String normalized = normalize(value);
        if (normalized.isEmpty()) {
            return "";
        }
        return normalized.replace("\n", "<br>\n");
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

    record Row(boolean isReply, String timeAbbrev, String rfcDatetime, String user, String message, String permalink) {
    }
}
