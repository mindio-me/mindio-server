/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.service;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Component
public class BookmarkUrlNormalizer {

    private static final Set<String> TRACKING_PARAMS = Set.of(
            "utm_source", "utm_medium", "utm_campaign", "utm_content", "utm_term", "fbclid", "gclid");

    public String normalize(String url) {
        if (url == null) return null;
        String trimmed = url.trim();
        try {
            URI uri = new URI(trimmed);
            String host = uri.getHost() != null ? uri.getHost().toLowerCase() : "";
            String path = uri.getPath() != null ? uri.getPath() : "";
            if (path.endsWith("/") && path.length() > 1) path = path.substring(0, path.length() - 1);
            String query = filterTrackingParams(uri.getRawQuery());

            StringBuilder sb = new StringBuilder("https://").append(host).append(path);
            if (query != null && !query.isEmpty()) sb.append('?').append(query);
            return sb.toString();
        } catch (URISyntaxException e) {
            return trimmed;
        }
    }

    private String filterTrackingParams(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) return null;
        Map<String, String> kept = new LinkedHashMap<>();
        for (String pair : rawQuery.split("&")) {
            if (pair.isBlank()) continue;
            String key = pair.contains("=") ? pair.substring(0, pair.indexOf('=')) : pair;
            if (!TRACKING_PARAMS.contains(key.toLowerCase())) {
                kept.put(key, pair);
            }
        }
        return kept.isEmpty() ? null : String.join("&", kept.values());
    }
}
