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
import java.util.*;
import org.junit.jupiter.api.Test;

class ChatArchiveAppTest {

    @Test
    void syncWithHistoryIsIdempotent() {
        String timestamp = "1741500000.0001";
        SlackMessage msg1 = new SlackMessage(timestamp, null, "U123", null, null, "Hello", null, null);

        System.out.println("DEBUG: Timestamp value is -> " + msg1.ts());
        System.out.println("DEBUG: Timestamp class is -> " + (msg1.ts() == null ? "null" : msg1.ts().getClass().getName()));

        Map<String, SlackMessage> historyMap = new HashMap<>();
        historyMap.put(msg1.ts(), msg1);
        historyMap.put(timestamp, msg1); // Use the raw string as well

        System.out.println("DEBUG: Map size after same-key puts -> " + historyMap.size());

        assertEquals(1, historyMap.size(), "If this is 2, then msg1.ts() is NOT equal to the string we passed in.");
    }
}