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

import java.util.Map;
import org.junit.jupiter.api.Test;

class SlackTextFormatterTest {

    @Test
    void formatsSlackLinksAndMentions() {
        String input = "See <https://example.com|this> and ping <@U1> in <#C1|general>.";
        String formatted = SlackTextFormatter.format(input, Map.of("U1", "alex")::get);
        assertEquals("See <a class=\"archive-link\" href=\"https://example.com\">this</a> and ping @alex in #general.",
                formatted);
    }

    @Test
    void formatsSpecialMentionsAndEmoji() {
        String input = "Hi <!here> :wave:";
        String formatted = SlackTextFormatter.format(input, id -> id);
        assertEquals("Hi @here \uD83D\uDC4B", formatted);
    }

    @Test
    void escapesNonTokenMarkup() {
        String input = "Ignore <javascript:alert(1)|click> and visit <https://example.com>.";
        String formatted = SlackTextFormatter.format(input, id -> id);
        assertEquals("Ignore click and visit "
                + "<a class=\"archive-link\" href=\"https://example.com\">https://example.com</a>.", formatted);
    }
}
