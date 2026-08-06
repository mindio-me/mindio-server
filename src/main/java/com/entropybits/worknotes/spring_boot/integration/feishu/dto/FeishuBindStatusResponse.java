/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.feishu.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeishuBindStatusResponse {
    private boolean bound;
    /**
     * 当前用户是否已配置飞书应用凭证（App ID / App Secret）
     */
    private boolean configured;
    /**
     * 脱敏后的 App ID（可选，用于 UI 展示）
     */
    private String appIdMasked;
    private String tenantKey;
    private String feishuUserId;
    private String feishuOpenId;
}


