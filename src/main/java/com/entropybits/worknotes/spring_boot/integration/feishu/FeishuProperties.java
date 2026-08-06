/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.feishu;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "feishu")
public class FeishuProperties {
    /**
     * OAuth 回调地址（redirect_uri）
     */
    private String oauthRedirectUri;

    /**
     * OpenAPI Base URL
     */
    private String baseUrl = "https://open.feishu.cn";

    /**
     * Base64 AES key (32 bytes recommended) for token encryption at rest
     */
    private String tokenCryptoKey;
}


