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
public class FeishuWikiNodeDto {
    private String nodeToken;
    private String objToken;
    private String title;
    private String objType;
    private boolean hasChild;
    private String parentNodeToken;
    private String objCreateTime;
    private String objEditTime;
}


