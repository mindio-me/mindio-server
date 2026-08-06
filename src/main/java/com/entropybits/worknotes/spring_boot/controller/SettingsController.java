/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.controller;

import com.entropybits.worknotes.spring_boot.dto.SiteSettingsRequest;
import com.entropybits.worknotes.spring_boot.dto.SiteSettingsResponse;
import com.entropybits.worknotes.spring_boot.integration.feishu.dto.FeishuCredentialRequest;
import com.entropybits.worknotes.spring_boot.repository.UserRepository;
import com.entropybits.worknotes.spring_boot.service.SettingsService;
import com.entropybits.worknotes.spring_boot.service.UserCredentialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * 全局站点与集成配置相关接口
 */
@RestController
@RequestMapping("/v1/settings")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class SettingsController {

    private final SettingsService settingsService;
    private final UserRepository userRepository;
    private final UserCredentialService userCredentialService;

    /**
     * 获取站点设置（siteName / logoUrl 等）
     */
    @GetMapping("/site")
    public ResponseEntity<SiteSettingsResponse> getSiteSettings() {
        return ResponseEntity.ok(settingsService.getSiteSettings());
    }

    /**
     * 更新站点设置，仅管理员可写。
     */
    @PutMapping("/site")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SiteSettingsResponse> updateSiteSettings(
            @Valid @RequestBody SiteSettingsRequest request
    ) {
        return ResponseEntity.ok(settingsService.updateSiteSettings(request));
    }

    /**
     * 保存/更新飞书应用凭证（App ID / App Secret）。
     * 目前仍复用 UserCredentialService，并将当前用户的配置视为全局配置来源。
     */
    @PostMapping("/integrations/feishu/credentials")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> upsertFeishuCredentials(
            @Valid @RequestBody FeishuCredentialRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        var user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new com.entropybits.worknotes.spring_boot.exception.ResourceNotFoundException("用户不存在"));
        userCredentialService.upsertFeishu(user, request.getAppId(), request.getAppSecret());
        return ResponseEntity.noContent().build();
    }

    /**
     * 阿里云 OSS 配置占位接口：目前仅设计签名，未做真实持久化。
     */
    @PutMapping("/integrations/oss")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> upsertOssSettings() {
        // 后续接入阿里云 OSS 配置持久化时，在此实现具体保存逻辑
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}


