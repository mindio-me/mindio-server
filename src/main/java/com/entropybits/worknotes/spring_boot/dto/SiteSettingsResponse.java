/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.dto;

import com.entropybits.worknotes.spring_boot.entity.SiteSettings;
import lombok.Data;

/**
 * 站点设置响应 DTO
 */
@Data
public class SiteSettingsResponse {

    private String siteName;
    private String logoUrl;

    public static SiteSettingsResponse fromEntity(SiteSettings entity) {
        SiteSettingsResponse resp = new SiteSettingsResponse();
        resp.setSiteName(entity.getSiteName());
        resp.setLogoUrl(entity.getLogoUrl());
        return resp;
    }
}


