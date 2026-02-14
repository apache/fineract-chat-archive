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

class HtmlRendererTest {

    @Test
    void dailyPageUsesClassBasedHtmlRows() {
        List<HtmlRenderer.Row> rows = List.of(
                new HtmlRenderer.Row(false, "Thu 09:15", "Thu, 12 Feb 2026 09:15:00 GMT",
                        "alex", "Thread root with <a class=\"archive-link\" href=\"https://example.org\">link</a>",
                        "https://slack.example/permalink", List.of("\uD83D\uDC4D 1")),
                new HtmlRenderer.Row(true, "Thu 09:16", "Thu, 12 Feb 2026 09:16:00 GMT",
                        "sam", "_Okay_\n*Passed ?*\n~Maybe~\n- Check\n- LFG",
                        "https://slack.example/permalink2", List.of("\uD83D\uDC4B 2")),
                new HtmlRenderer.Row(true, "Thu 09:18", "Thu, 12 Feb 2026 09:18:00 GMT",
                        "jo", "Looks good \uD83D\uDC4D", "https://slack.example/permalink3",
                        List.of()));

        String page = HtmlRenderer.renderDailyPage("fineract", LocalDate.parse("2026-02-12"), rows);

        assertTrue(page.contains("<!doctype html>"));
        assertTrue(page.contains("class=\"archive-message\""));
        assertTrue(page.contains("class=\"archive-message archive-message-reply\""));
        assertTrue(page.contains("class=\"archive-thread\""));
        assertTrue(page.contains("class=\"archive-meta\""));
        assertTrue(page.contains("class=\"archive-reply-label\">reply</span>"));
        assertTrue(page.contains("class=\"archive-user\""));
        assertTrue(page.contains("class=\"archive-text\""));
        assertTrue(page.contains("<em>Okay</em>"));
        assertTrue(page.contains("<strong>Passed ?</strong>"));
        assertTrue(page.contains("<del>Maybe</del>"));
        assertTrue(page.contains("<ul class=\"archive-list\">"));
        assertTrue(page.contains("<li>Check</li>"));
        assertTrue(page.contains("class=\"archive-reactions\""));
        assertTrue(page.contains("class=\"archive-reaction\">\uD83D\uDC4D 1</span>"));
        assertTrue(page.contains("href=\"../../../assets/chat-archive.css\""));
        assertFalse(page.contains("permalink:"));
        assertFalse(page.contains("---"));
    }

    @Test
    void indexesUseExtensionlessLinks() {
        String channel = HtmlRenderer.renderChannelIndex("fineract",
                List.of(LocalDate.parse("2026-02-12")));
        String global = HtmlRenderer.renderGlobalIndex(List.of("fineract"));

        assertTrue(channel.contains("href=\"2026-02-12/\""));
        assertTrue(global.contains("href=\"daily/fineract/\""));
        assertTrue(channel.contains("href=\"../../assets/chat-archive.css\""));
        assertTrue(global.contains("href=\"assets/chat-archive.css\""));
        assertFalse(channel.contains(".md"));
        assertFalse(global.contains(".md"));
    }
}
