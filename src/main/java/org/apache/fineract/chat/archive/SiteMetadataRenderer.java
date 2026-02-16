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

    private static String joinSitePath(String siteBaseUrl, String path) {
        if (path == null || path.isBlank()) {
            return siteBaseUrl + "/";
        }
        return siteBaseUrl + "/" + path;
    }
}
