/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.controller;

import com.entropybits.worknotes.spring_boot.dto.ProfileRequest;
import com.entropybits.worknotes.spring_boot.dto.ProfileResponse;
import com.entropybits.worknotes.spring_boot.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Profile API Controller
 */
@RestController
@RequestMapping("/v1/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    /**
     * 获取当前用户的个人资料
     * GET /api/v1/profiles/me
     */
    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getCurrentUserProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();
        ProfileResponse profile = profileService.getCurrentUserProfile(username);
        return ResponseEntity.ok(profile);
    }

    /**
     * 获取站点 Owner 的公开资料（无需认证）
     * GET /api/v1/profiles/owner
     */
    @GetMapping("/owner")
    public ResponseEntity<ProfileResponse> getOwnerProfile() {
        ProfileResponse profile = profileService.getOwnerProfile();
        return ResponseEntity.ok(profile);
    }

    /**
     * 根据用户名获取个人资料
     * GET /api/v1/profiles/{username}
     */
    @GetMapping("/{username}")
    public ResponseEntity<ProfileResponse> getProfileByUsername(@PathVariable String username) {
        ProfileResponse profile = profileService.getProfileByUsername(username);
        return ResponseEntity.ok(profile);
    }

    /**
     * 创建或更新当前用户的个人资料
     * PUT /api/v1/profiles/me
     */
    @PutMapping("/me")
    public ResponseEntity<ProfileResponse> createOrUpdateProfile(
            @Valid @RequestBody ProfileRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();
        ProfileResponse profile = profileService.createOrUpdateProfile(request, username);
        return ResponseEntity.ok(profile);
    }
}
