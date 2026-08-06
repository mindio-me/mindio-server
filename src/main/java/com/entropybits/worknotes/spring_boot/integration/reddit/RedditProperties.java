/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.reddit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Reddit OAuth + Submit API configuration.
 */
@Data
@ConfigurationProperties(prefix = "reddit")
public class RedditProperties {

    private String clientId = "";
    private String clientSecret = "";
    /** Must match the redirect URI registered in your Reddit app settings. */
    private String oauthRedirectUri = "";
    /**
     * User-Agent header required by Reddit API.
     * Format: platform:app_id:version (by /u/username)
     */
    private String userAgent = "MindIO/1.0 by mindio-app";
    /** Base64-encoded 16/24/32-byte AES key for encrypting stored tokens at rest. */
    private String tokenCryptoKey = "";

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank()
                && oauthRedirectUri != null && !oauthRedirectUri.isBlank();
    }
}
