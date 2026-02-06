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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Locale;

final class SlackTimestamp {

    private SlackTimestamp() {}

    static Instant toInstant(String ts) {
        if (ts == null || ts.isBlank()) {
            return Instant.EPOCH;
        }
        BigDecimal value = new BigDecimal(ts);
        long seconds = value.longValue();
        BigDecimal fractional = value.subtract(BigDecimal.valueOf(seconds));
        int nanos = fractional.movePointRight(9).setScale(0, RoundingMode.DOWN).intValue();
        return Instant.ofEpochSecond(seconds, nanos);
    }

    static int compare(String first, String second) {
        if (first == null) {
            return second == null ? 0 : -1;
        }
        if (second == null) {
            return 1;
        }
        return new BigDecimal(first).compareTo(new BigDecimal(second));
    }

    static String formatEpochSecond(long epochSecond) {
        return String.format(Locale.ROOT, "%d.000000", epochSecond);
    }
}
