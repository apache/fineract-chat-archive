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

final class MarkdownRenderer {

    private MarkdownRenderer() {}

    static String renderDailyPage(String channelName, LocalDate date, List<Row> rows) {
        StringBuilder builder = new StringBuilder();
        builder.append("---\n");
        builder.append("title: \"#").append(channelName).append(" ").append(date).append("\"\n");
        builder.append("date: ").append(date).append('\n');
        builder.append("channel: ").append(channelName).append('\n');
        builder.append("---\n\n");

        builder.append("| Time (UTC) | User | Message |\n");
        builder.append("| --- | --- | --- |\n");
        for (Row row : rows) {
            builder.append("| ").append(formatTimeCell(row)).append(" | ")
                    .append(escape(row.user())).append(" | ")
                    .append(escape(row.message())).append(" |\n");
        }
        return builder.toString();
    }

    static String renderChannelIndex(String channelName, List<LocalDate> dates) {
        StringBuilder builder = new StringBuilder();
        builder.append("---\n");
        builder.append("title: \"#").append(channelName).append("\"\n");
        builder.append("channel: ").append(channelName).append('\n');
        builder.append("---\n\n");
        builder.append("## Days\n\n");
        for (LocalDate date : dates) {
            builder.append("- [").append(date).append("](").append(date).append(".md)\n");
        }
        return builder.toString();
    }

    static String renderGlobalIndex(List<String> channels) {
        StringBuilder builder = new StringBuilder();
        builder.append("---\n");
        builder.append("title: \"Fineract Chat Archive\"\n");
        builder.append("---\n\n");
        builder.append("## Channels\n\n");
        for (String channel : channels) {
            builder.append("- [#").append(channel).append("](daily/")
                    .append(channel).append("/index.md)\n");
        }
        return builder.toString();
    }

    private static String formatTimeCell(Row row) {
        if (row.permalink() == null || row.permalink().isBlank()) {
            return row.time();
        }
        return "[" + row.time() + "](" + row.permalink() + ")";
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("|", "\\|");
        return escaped.replace("\r\n", "<br>").replace("\n", "<br>");
    }

    record Row(String time, String user, String message, String permalink) {
    }
}
