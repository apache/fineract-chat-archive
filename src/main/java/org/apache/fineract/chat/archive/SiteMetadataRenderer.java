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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class SiteMetadataRenderer {

    private SiteMetadataRenderer() {}

    static String renderRobotsTxt(String siteBaseUrl) {
        StringBuilder builder = new StringBuilder();
        builder.append("User-agent: *\n");
        builder.append("Allow: /\n");
        if (siteBaseUrl != null && !siteBaseUrl.isBlank()) {
            builder.append("Sitemap: ").append(siteBaseUrl).append("/sitemap.xml\n");
        }
        return builder.toString();
    }

    static String renderSitemapXml(String siteBaseUrl, Map<String, List<LocalDate>> datesByChannel) {
        List<String> paths = new ArrayList<>();
        paths.add("");
        for (Map.Entry<String, List<LocalDate>> entry : datesByChannel.entrySet()) {
            String channel = entry.getKey();
            paths.add("daily/" + channel + "/");
            for (LocalDate date : entry.getValue()) {
                paths.add("daily/" + channel + "/" + date + "/");
            }
        }

        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        builder.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        for (String path : paths) {
            builder.append("  <url><loc>")
                    .append(joinSitePath(siteBaseUrl, path))
                    .append("</loc></url>\n");
        }
        builder.append("</urlset>\n");
        return builder.toString();
    }

    static String renderStylesheet() {
        return """
                :root {
                  color-scheme: light;
                }

                body {
                  margin: 0 auto;
                  max-width: 960px;
                  padding: 1.5rem;
                  font-family: "Segoe UI", Arial, sans-serif;
                  line-height: 1.5;
                  color: #1e293b;
                  background: #f8fafc;
                }

                .archive-log {
                  display: grid;
                  gap: 0.75rem;
                }

                .archive-message {
                  display: grid;
                  grid-template-columns: max-content max-content 1fr;
                  gap: 0.75rem;
                  align-items: baseline;
                  padding: 0.625rem 0.75rem;
                  border: 1px solid #dbe3ef;
                  border-radius: 8px;
                  background: #ffffff;
                }

                .archive-message-reply {
                  margin-left: 1.5rem;
                  border-left: 4px solid #cbd5e1;
                }

                .archive-reply-indicator {
                  color: #64748b;
                  font-weight: 600;
                }

                .archive-time {
                  font-family: "SFMono-Regular", Consolas, monospace;
                  font-size: 0.9rem;
                  color: #334155;
                  text-decoration: none;
                  white-space: nowrap;
                }

                .archive-time-link:hover {
                  text-decoration: underline;
                }

                .archive-user {
                  font-weight: 700;
                  color: #0f172a;
                  white-space: nowrap;
                }

                .archive-text {
                  color: #1e293b;
                  overflow-wrap: anywhere;
                }

                .archive-link {
                  color: #0a58ca;
                  text-decoration: underline;
                }

                .archive-index ul {
                  list-style: none;
                  margin: 0;
                  padding: 0;
                }

                .archive-index li {
                  margin: 0.35rem 0;
                }

                @media (max-width: 720px) {
                  body {
                    padding: 1rem;
                  }

                  .archive-message {
                    grid-template-columns: 1fr;
                    gap: 0.35rem;
                  }

                  .archive-message-reply {
                    margin-left: 0.75rem;
                  }
                }
                """;
    }

    private static String joinSitePath(String siteBaseUrl, String path) {
        if (path == null || path.isBlank()) {
            return siteBaseUrl + "/";
        }
        return siteBaseUrl + "/" + path;
    }
}
