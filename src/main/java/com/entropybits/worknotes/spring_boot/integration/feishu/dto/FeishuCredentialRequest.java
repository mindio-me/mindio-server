/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.feishu.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FeishuCredentialRequest {
    @NotBlank(message = "App ID 不能为空")
    private String appId;
    @NotBlank(message = "App Secret 不能为空")
    private String appSecret;
}


