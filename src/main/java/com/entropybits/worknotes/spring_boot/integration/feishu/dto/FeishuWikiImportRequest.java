/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.feishu.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FeishuWikiImportRequest {
    @NotBlank
    private String spaceId;

    @NotBlank
    private String nodeToken;

    /**
     * 实际文档对象 token（如 Docx 的 doc_token）；
     * 如果为空，则回退使用 nodeToken。
     */
    private String objToken;

    private String nodeTitle;
    private String objType;
    private String objCreateTime;
    private String objEditTime;
}


