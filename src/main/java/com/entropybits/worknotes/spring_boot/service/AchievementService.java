/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.service;

import com.entropybits.worknotes.spring_boot.dto.AchievementRequest;
import com.entropybits.worknotes.spring_boot.dto.AchievementResponse;
import com.entropybits.worknotes.spring_boot.entity.Achievement;
import com.entropybits.worknotes.spring_boot.entity.User;
import com.entropybits.worknotes.spring_boot.exception.ResourceNotFoundException;
import com.entropybits.worknotes.spring_boot.repository.AchievementRepository;
import com.entropybits.worknotes.spring_boot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Achievement 业务逻辑层
 */
@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AchievementResponse> getAllActiveAchievements() {
        return achievementRepository.findByIsActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(AchievementResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AchievementResponse> getCurrentUserAchievements(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        return achievementRepository.findByOwnerOrderByDisplayOrderAsc(user)
                .stream()
                .map(AchievementResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AchievementResponse getAchievementById(Long id) {
        Achievement achievement = achievementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("成就不存在，ID: " + id));
        return AchievementResponse.fromEntity(achievement);
    }

    @Transactional
    public AchievementResponse createAchievement(AchievementRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        Achievement achievement = Achievement.builder()
                .title(request.getTitle())
                .subtitle(request.getSubtitle())
                .type(request.getType())
                .status(request.getStatus())
                .icon(request.getIcon())
                .iconVariant(request.getIconVariant())
                .description(request.getDescription())
                .technologies(request.getTechnologies())
                .isActive(request.getIsActive())
                .displayOrder(request.getDisplayOrder())
                .owner(user)
                .build();

        Achievement saved = achievementRepository.save(achievement);
        return AchievementResponse.fromEntity(saved);
    }

    @Transactional
    public AchievementResponse updateAchievement(Long id, AchievementRequest request, String username) {
        Achievement achievement = achievementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("成就不存在，ID: " + id));

        if (!achievement.getOwner().getUsername().equals(username)) {
            throw new RuntimeException("没有权限修改此成就");
        }

        achievement.setTitle(request.getTitle());
        achievement.setSubtitle(request.getSubtitle());
        achievement.setType(request.getType());
        achievement.setStatus(request.getStatus());
        achievement.setIcon(request.getIcon());
        achievement.setIconVariant(request.getIconVariant());
        achievement.setDescription(request.getDescription());
        achievement.setTechnologies(request.getTechnologies());
        achievement.setIsActive(request.getIsActive());
        achievement.setDisplayOrder(request.getDisplayOrder());

        Achievement updated = achievementRepository.save(achievement);
        return AchievementResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteAchievement(Long id, String username) {
        Achievement achievement = achievementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("成就不存在，ID: " + id));

        if (!achievement.getOwner().getUsername().equals(username)) {
            throw new RuntimeException("没有权限删除此成就");
        }

        achievementRepository.delete(achievement);
    }
}
