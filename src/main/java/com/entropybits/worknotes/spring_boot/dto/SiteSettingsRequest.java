/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.dto;

import lombok.Data;

/**
 * 站点设置请求 DTO
 */
@Data
public class SiteSettingsRequest {

    private String siteName;
    private String logoUrl;
}


