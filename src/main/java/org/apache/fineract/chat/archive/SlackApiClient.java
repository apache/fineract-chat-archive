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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class SlackApiClient {

    private static final URI AUTH_TEST_URI = URI.create("https://slack.com/api/auth.test");
    private static final String CONVERSATIONS_LIST_URL = "https://slack.com/api/conversations.list";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);
    private static final int CONVERSATIONS_PAGE_SIZE = 200;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    SlackApiClient() {
        this.httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        this.objectMapper = new ObjectMapper();
    }

    AuthTestResponse authTest(String token) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(AUTH_TEST_URI)
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            return AuthTestResponse.httpError(response.statusCode());
        }

        return objectMapper.readValue(response.body(), AuthTestResponse.class);
    }

    ConversationsListResponse listPublicChannels(String token)
            throws IOException, InterruptedException {
        List<SlackChannel> channels = new ArrayList<>();
        String cursor = null;

        do {
            URI uri = buildConversationsListUri(cursor);
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(REQUEST_TIMEOUT)
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return ConversationsListResponse.httpError(response.statusCode());
            }

            ConversationsListResponse payload = objectMapper.readValue(response.body(),
                    ConversationsListResponse.class);
            if (!payload.ok()) {
                return new ConversationsListResponse(false, payload.error(), List.of(), null);
            }

            if (payload.channels() != null) {
                channels.addAll(payload.channels());
            }
            cursor = payload.nextCursor();
        } while (cursor != null && !cursor.isBlank());

        return new ConversationsListResponse(true, null, List.copyOf(channels), null);
    }

    private static URI buildConversationsListUri(String cursor) {
        StringBuilder query = new StringBuilder();
        query.append("limit=").append(CONVERSATIONS_PAGE_SIZE);
        query.append("&exclude_archived=true");
        query.append("&types=public_channel");
        if (cursor != null && !cursor.isBlank()) {
            query.append("&cursor=")
                    .append(URLEncoder.encode(cursor, StandardCharsets.UTF_8));
        }
        return URI.create(CONVERSATIONS_LIST_URL + "?" + query);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AuthTestResponse(boolean ok, String error, String team, String user) {
        static AuthTestResponse httpError(int statusCode) {
            return new AuthTestResponse(false, "http_status_" + statusCode, null, null);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ConversationsListResponse(boolean ok, String error, List<SlackChannel> channels,
            @JsonProperty("response_metadata") ResponseMetadata responseMetadata) {
        static ConversationsListResponse httpError(int statusCode) {
            return new ConversationsListResponse(false, "http_status_" + statusCode, List.of(),
                    null);
        }

        String nextCursor() {
            if (responseMetadata == null) {
                return null;
            }
            return responseMetadata.nextCursor();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ResponseMetadata(@JsonProperty("next_cursor") String nextCursor) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SlackChannel(String id, String name) {
    }
}

