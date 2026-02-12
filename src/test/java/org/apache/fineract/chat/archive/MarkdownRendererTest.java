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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class MarkdownRendererTest {

    @Test
    void dailyPageUsesClassBasedHtmlRows() {
        List<MarkdownRenderer.Row> rows = List.of(
                new MarkdownRenderer.Row(false, "Thu 09:15", "Thu, 12 Feb 2026 09:15:00 GMT",
                        "alex", "hello <a class=\"archive-link\" href=\"https://example.org\">world</a>",
                        "https://slack.example/permalink"),
                new MarkdownRenderer.Row(true, "Thu 09:16", "Thu, 12 Feb 2026 09:16:00 GMT",
                        "sam", "reply", "https://slack.example/permalink2"));

        String page = MarkdownRenderer.renderDailyPage("fineract", LocalDate.parse("2026-02-12"), rows);

        assertFalse(page.contains("**"));
        assertTrue(page.contains("class=\"archive-message\""));
        assertTrue(page.contains("class=\"archive-message archive-message-reply\""));
        assertTrue(page.contains("class=\"archive-user\""));
        assertTrue(page.contains("class=\"archive-text\""));
        assertTrue(page.contains("href=\"../../assets/chat-archive.css\""));
        assertTrue(page.contains("permalink: /daily/fineract/2026-02-12/"));
    }

    @Test
    void indexesUseExtensionlessLinks() {
        String channel = MarkdownRenderer.renderChannelIndex("fineract",
                List.of(LocalDate.parse("2026-02-12")));
        String global = MarkdownRenderer.renderGlobalIndex(List.of("fineract"));

        assertTrue(channel.contains("href=\"2026-02-12/\""));
        assertTrue(global.contains("href=\"daily/fineract/\""));
        assertFalse(channel.contains(".md"));
        assertFalse(global.contains(".md"));
    }
}
