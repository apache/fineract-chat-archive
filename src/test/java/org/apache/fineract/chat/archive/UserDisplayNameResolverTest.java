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

import org.junit.jupiter.api.Test;

class UserDisplayNameResolverTest {

    @Test
    void resolvePrefersDisplayName() {
        SlackApiClient.SlackProfile profile = new SlackApiClient.SlackProfile("Ada", "Ada L");
        SlackApiClient.SlackUser user = new SlackApiClient.SlackUser("U1", "ada", profile);

        assertEquals("Ada", UserDisplayNameResolver.resolve(user));
    }

    @Test
    void resolveFallsBackToRealName() {
        SlackApiClient.SlackProfile profile = new SlackApiClient.SlackProfile(" ", "Ada Lovelace");
        SlackApiClient.SlackUser user = new SlackApiClient.SlackUser("U2", "ada", profile);

        assertEquals("Ada Lovelace", UserDisplayNameResolver.resolve(user));
    }

    @Test
    void resolveFallsBackToUserName() {
        SlackApiClient.SlackProfile profile = new SlackApiClient.SlackProfile(null, null);
        SlackApiClient.SlackUser user = new SlackApiClient.SlackUser("U3", "ada", profile);

        assertEquals("ada", UserDisplayNameResolver.resolve(user));
    }
}
