/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.service;

import com.entropybits.worknotes.spring_boot.dto.SiteSettingsRequest;
import com.entropybits.worknotes.spring_boot.dto.SiteSettingsResponse;
import com.entropybits.worknotes.spring_boot.entity.Profile;
import com.entropybits.worknotes.spring_boot.entity.SiteSettings;
import com.entropybits.worknotes.spring_boot.repository.ProfileRepository;
import com.entropybits.worknotes.spring_boot.repository.SiteSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 全局站点与集成设置相关业务
 */
@Service
@RequiredArgsConstructor
public class SettingsService {

    private final SiteSettingsRepository siteSettingsRepository;
    private final ProfileRepository profileRepository;

    @Transactional
    public SiteSettingsResponse getSiteSettings() {
        SiteSettings settings = getOrInitSettingsFromProfileIfNeeded();
        return SiteSettingsResponse.fromEntity(settings);
    }

    @Transactional
    public SiteSettingsResponse updateSiteSettings(SiteSettingsRequest request) {
        SiteSettings settings = getOrInitSettingsFromProfileIfNeeded();
        settings.setSiteName(request.getSiteName());
        settings.setLogoUrl(request.getLogoUrl());
        SiteSettings saved = siteSettingsRepository.save(settings);
        return SiteSettingsResponse.fromEntity(saved);
    }

    /**
     * 获取唯一一条站点设置；如不存在则尝试从首条 Profile 记录中迁移 siteName/logoUrl。
     */
    @Transactional
    protected SiteSettings getOrInitSettingsFromProfileIfNeeded() {
        Optional<SiteSettings> existing = siteSettingsRepository.findTopByOrderByIdAsc();
        if (existing.isPresent()) {
            return existing.get();
        }

        SiteSettings.SiteSettingsBuilder builder = SiteSettings.builder();

        // 简单迁移：从第一条 Profile 记录中拷贝站点名称和 Logo（如果存在）
        profileRepository.findAll().stream().findFirst().ifPresent((Profile profile) -> {
            builder.siteName(profile.getSiteName());
            builder.logoUrl(profile.getLogoUrl());
        });

        SiteSettings created = builder.build();
        return siteSettingsRepository.save(created);
    }
}


