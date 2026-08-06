/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.dto;

import com.entropybits.worknotes.spring_boot.entity.Achievement;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 成就响应 DTO
 */
@Data
public class AchievementResponse {

    private Long id;
    private String title;
    private String subtitle;
    private String type;
    private String status;
    private String icon;
    private String iconVariant;
    private String description;
    private String technologies;
    private Boolean isActive;
    private Integer displayOrder;
    private String ownerUsername;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;

    public static AchievementResponse fromEntity(Achievement achievement) {
        AchievementResponse response = new AchievementResponse();
        response.setId(achievement.getId());
        response.setTitle(achievement.getTitle());
        response.setSubtitle(achievement.getSubtitle());
        response.setType(achievement.getType());
        response.setStatus(achievement.getStatus());
        response.setIcon(achievement.getIcon());
        response.setIconVariant(achievement.getIconVariant());
        response.setDescription(achievement.getDescription());
        response.setTechnologies(achievement.getTechnologies());
        response.setIsActive(achievement.getIsActive());
        response.setDisplayOrder(achievement.getDisplayOrder());
        response.setOwnerUsername(achievement.getOwner().getUsername());
        response.setCreatedAt(achievement.getCreatedAt());
        response.setModifiedAt(achievement.getModifiedAt());
        return response;
    }
}
