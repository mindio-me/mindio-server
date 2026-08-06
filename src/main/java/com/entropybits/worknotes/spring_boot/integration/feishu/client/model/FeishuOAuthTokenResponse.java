/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.feishu.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 飞书 OAuth Token 响应
 * <p>
 * 支持两种响应格式：
 * 1. v2 接口（标准 OAuth 2.0）：token 字段直接在根级别
 * 2. v1 接口（旧格式）：token 字段在 data 节点下
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeishuOAuthTokenResponse extends FeishuBaseResponse {

    // v1 格式：数据在 data 节点下
    private DataNode data;

    // v2 格式（OAuth 2.0 标准）：token 直接在根级别
    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("expires_in")
    private Integer expiresIn;

    @JsonProperty("refresh_token_expires_in")
    private Integer refreshTokenExpiresIn;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("scope")
    private String scope;

    @JsonProperty("open_id")
    private String openId;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("tenant_key")
    private String tenantKey;

    /**
     * 获取统一的数据视图，兼容 v1 和 v2 格式
     * <p>
     * 如果 data 节点存在（v1 格式），返回 data；
     * 否则将根级别字段包装成 DataNode 返回（v2 格式）。
     */
    public DataNode getData() {
        // v1 格式：直接返回 data 节点
        if (data != null && data.getAccessToken() != null) {
            return data;
        }

        // v2 格式：将根级别字段包装成 DataNode
        if (accessToken != null) {
            DataNode node = new DataNode();
            node.setAccessToken(accessToken);
            node.setRefreshToken(refreshToken);
            node.setExpiresIn(expiresIn);
            node.setRefreshTokenExpiresIn(refreshTokenExpiresIn);
            node.setTokenType(tokenType);
            node.setScope(scope);
            node.setOpenId(openId);
            node.setUserId(userId);
            node.setTenantKey(tenantKey);
            return node;
        }

        return data;
    }

    @lombok.Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataNode {
        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("refresh_token")
        private String refreshToken;

        @JsonProperty("expires_in")
        private Integer expiresIn;

        @JsonProperty("refresh_token_expires_in")
        private Integer refreshTokenExpiresIn;

        @JsonProperty("token_type")
        private String tokenType;

        @JsonProperty("scope")
        private String scope;

        // user identifiers (field names vary by version; keep optional)
        @JsonProperty("open_id")
        private String openId;

        @JsonProperty("user_id")
        private String userId;

        @JsonProperty("tenant_key")
        private String tenantKey;
    }
}
