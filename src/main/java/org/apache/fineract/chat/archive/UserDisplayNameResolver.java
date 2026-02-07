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

final class UserDisplayNameResolver {

    private UserDisplayNameResolver() {}

    static String resolve(SlackApiClient.SlackUser user) {
        if (user == null) {
            return "unknown";
        }
        SlackApiClient.SlackProfile profile = user.profile();
        if (profile != null) {
            String displayName = clean(profile.displayName());
            if (!displayName.isEmpty()) {
                return displayName;
            }
            String realName = clean(profile.realName());
            if (!realName.isEmpty()) {
                return realName;
            }
        }
        String name = clean(user.name());
        if (!name.isEmpty()) {
            return name;
        }
        String id = clean(user.id());
        return id.isEmpty() ? "unknown" : id;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
