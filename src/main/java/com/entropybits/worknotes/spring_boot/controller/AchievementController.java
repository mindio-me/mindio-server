/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.controller;

import com.entropybits.worknotes.spring_boot.dto.AchievementRequest;
import com.entropybits.worknotes.spring_boot.dto.AchievementResponse;
import com.entropybits.worknotes.spring_boot.service.AchievementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Achievement API Controller
 */
@RestController
@RequestMapping("/v1/achievements")
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementService achievementService;

    /**
     * 获取所有公开展示的成就
     * GET /api/v1/achievements
     */
    @GetMapping
    public ResponseEntity<List<AchievementResponse>> getAllActiveAchievements() {
        return ResponseEntity.ok(achievementService.getAllActiveAchievements());
    }

    /**
     * 获取当前用户的全部成就（含未公开的），供管理页使用
     * GET /api/v1/achievements/my
     */
    @GetMapping("/my")
    public ResponseEntity<List<AchievementResponse>> getMyAchievements(Authentication authentication) {
        return ResponseEntity.ok(achievementService.getCurrentUserAchievements(authentication.getName()));
    }

    /**
     * 根据ID获取成就
     * GET /api/v1/achievements/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<AchievementResponse> getAchievementById(@PathVariable Long id) {
        return ResponseEntity.ok(achievementService.getAchievementById(id));
    }

    /**
     * 创建成就
     * POST /api/v1/achievements
     */
    @PostMapping
    public ResponseEntity<AchievementResponse> createAchievement(
            @Valid @RequestBody AchievementRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        AchievementResponse achievement = achievementService.createAchievement(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(achievement);
    }

    /**
     * 更新成就
     * PUT /api/v1/achievements/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<AchievementResponse> updateAchievement(
            @PathVariable Long id,
            @Valid @RequestBody AchievementRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        AchievementResponse achievement = achievementService.updateAchievement(id, request, username);
        return ResponseEntity.ok(achievement);
    }

    /**
     * 删除成就
     * DELETE /api/v1/achievements/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAchievement(
            @PathVariable Long id,
            Authentication authentication) {
        String username = authentication.getName();
        achievementService.deleteAchievement(id, username);
        return ResponseEntity.noContent().build();
    }
}
