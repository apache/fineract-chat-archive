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
import java.util.Optional;

final class SlackApiClient {

    private static final URI AUTH_TEST_URI = URI.create("https://slack.com/api/auth.test");
    private static final String CONVERSATIONS_LIST_URL = "https://slack.com/api/conversations.list";
    private static final String CONVERSATIONS_HISTORY_URL =
            "https://slack.com/api/conversations.history";
    private static final String CHAT_PERMALINK_URL = "https://slack.com/api/chat.getPermalink";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);
    private static final int CONVERSATIONS_PAGE_SIZE = 200;
    private static final int HISTORY_PAGE_SIZE = 200;

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

        HttpResponse<String> response = sendWithRetry(request);
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

            HttpResponse<String> response = sendWithRetry(request);
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

    ConversationsHistoryResponse listChannelMessages(String token, String channelId,
            String oldestTs) throws IOException, InterruptedException {
        List<SlackMessage> messages = new ArrayList<>();
        String cursor = null;

        do {
            URI uri = buildConversationsHistoryUri(channelId, oldestTs, cursor);
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(REQUEST_TIMEOUT)
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();

            HttpResponse<String> response = sendWithRetry(request);
            if (response.statusCode() != 200) {
                return ConversationsHistoryResponse.httpError(response.statusCode());
            }

            ConversationsHistoryResponse payload = objectMapper.readValue(response.body(),
                    ConversationsHistoryResponse.class);
            if (!payload.ok()) {
                return new ConversationsHistoryResponse(false, payload.error(), List.of(), null);
            }

            if (payload.messages() != null) {
                messages.addAll(payload.messages());
            }
            cursor = payload.nextCursor();
        } while (cursor != null && !cursor.isBlank());

        return new ConversationsHistoryResponse(true, null, List.copyOf(messages), null);
    }

    PermalinkResponse getPermalink(String token, String channelId, String messageTs)
            throws IOException, InterruptedException {
        URI uri = buildPermalinkUri(channelId, messageTs);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> response = sendWithRetry(request);
        if (response.statusCode() != 200) {
            return PermalinkResponse.httpError(response.statusCode());
        }
        return objectMapper.readValue(response.body(), PermalinkResponse.class);
    }

    private HttpResponse<String> sendWithRetry(HttpRequest request)
            throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 429) {
            return response;
        }
        Optional<Duration> retryAfter = parseRetryAfter(response);
        if (retryAfter.isEmpty()) {
            return response;
        }
        Thread.sleep(retryAfter.get().toMillis());
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private Optional<Duration> parseRetryAfter(HttpResponse<String> response) {
        Optional<String> header = response.headers().firstValue("Retry-After");
        if (header.isEmpty()) {
            return Optional.empty();
        }
        try {
            long seconds = Long.parseLong(header.get().trim());
            return Optional.of(Duration.ofSeconds(seconds));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
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

    private static URI buildConversationsHistoryUri(String channelId, String oldestTs,
            String cursor) {
        StringBuilder query = new StringBuilder();
        query.append("channel=")
                .append(URLEncoder.encode(channelId, StandardCharsets.UTF_8));
        query.append("&limit=").append(HISTORY_PAGE_SIZE);
        query.append("&inclusive=true");
        if (oldestTs != null && !oldestTs.isBlank()) {
            query.append("&oldest=")
                    .append(URLEncoder.encode(oldestTs, StandardCharsets.UTF_8));
        }
        if (cursor != null && !cursor.isBlank()) {
            query.append("&cursor=")
                    .append(URLEncoder.encode(cursor, StandardCharsets.UTF_8));
        }
        return URI.create(CONVERSATIONS_HISTORY_URL + "?" + query);
    }

    private static URI buildPermalinkUri(String channelId, String messageTs) {
        StringBuilder query = new StringBuilder();
        query.append("channel=")
                .append(URLEncoder.encode(channelId, StandardCharsets.UTF_8));
        query.append("&message_ts=")
                .append(URLEncoder.encode(messageTs, StandardCharsets.UTF_8));
        return URI.create(CHAT_PERMALINK_URL + "?" + query);
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ConversationsHistoryResponse(boolean ok, String error, List<SlackMessage> messages,
            @JsonProperty("response_metadata") ResponseMetadata responseMetadata) {
        static ConversationsHistoryResponse httpError(int statusCode) {
            return new ConversationsHistoryResponse(false, "http_status_" + statusCode, List.of(),
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
    record PermalinkResponse(boolean ok, String error, String permalink) {
        static PermalinkResponse httpError(int statusCode) {
            return new PermalinkResponse(false, "http_status_" + statusCode, null);
        }
    }
}

