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
import java.util.Map;
import org.junit.jupiter.api.Test;

class SiteMetadataRendererTest {

    @Test
    void robotsTxtIncludesSitemapWhenBaseUrlPresent() {
        String robots = SiteMetadataRenderer.renderRobotsTxt("https://example.org/archive");

        assertTrue(robots.contains("User-agent: *"));
        assertTrue(robots.contains("Allow: /"));
        assertTrue(robots.contains("Sitemap: https://example.org/archive/sitemap.xml"));
    }

    @Test
    void robotsTxtOmitsSitemapWhenBaseUrlMissing() {
        String robots = SiteMetadataRenderer.renderRobotsTxt("");

        assertTrue(robots.contains("User-agent: *"));
        assertFalse(robots.contains("Sitemap:"));
    }

    @Test
    void sitemapIncludesIndexAndDailyPages() {
        Map<String, List<LocalDate>> dates = Map.of(
                "fineract", List.of(LocalDate.parse("2026-02-06"), LocalDate.parse("2026-02-05")));

        String sitemap = SiteMetadataRenderer.renderSitemapXml("https://example.org/archive", dates);

        assertTrue(sitemap.contains("<loc>https://example.org/archive/</loc>"));
        assertTrue(sitemap.contains("<loc>https://example.org/archive/daily/fineract/</loc>"));
        assertTrue(sitemap.contains("<loc>https://example.org/archive/daily/fineract/2026-02-06/</loc>"));
        assertTrue(sitemap.contains("<loc>https://example.org/archive/daily/fineract/2026-02-05/</loc>"));
    }

}
