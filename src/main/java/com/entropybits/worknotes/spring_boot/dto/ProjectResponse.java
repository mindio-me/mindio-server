/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.dto;

import com.entropybits.worknotes.spring_boot.entity.Project;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目响应 DTO
 */
@Data
public class ProjectResponse {

    private Long id;
    private String name;
    private String shortName; // 项目简称/缩写
    private String subtitle;
    private String description;
    private String icon;
    private String imageUrl;
    private String projectUrl;
    private String githubUrl;
    private String category;
    private String technologies;
    private String content; // 项目正文内容（富文本）
    private String contentType; // 内容类型：richtext, markdown等
    private Boolean isPublic;
    private Boolean isFeatured;
    private Integer displayOrder;
    private String ownerUsername;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;

    public static ProjectResponse fromEntity(Project project) {
        ProjectResponse response = new ProjectResponse();
        response.setId(project.getId());
        response.setName(project.getName());
        response.setShortName(project.getShortName());
        response.setSubtitle(project.getSubtitle());
        response.setDescription(project.getDescription());
        response.setIcon(project.getIcon());
        response.setImageUrl(project.getImageUrl());
        response.setProjectUrl(project.getProjectUrl());
        response.setGithubUrl(project.getGithubUrl());
        response.setCategory(project.getCategory());
        response.setTechnologies(project.getTechnologies());
        response.setContent(project.getContent());
        response.setContentType(project.getContentType());
        response.setIsPublic(project.getIsPublic());
        response.setIsFeatured(project.getIsFeatured());
        response.setDisplayOrder(project.getDisplayOrder());
        response.setOwnerUsername(project.getOwner().getUsername());
        response.setCreatedAt(project.getCreatedAt());
        response.setModifiedAt(project.getModifiedAt());
        return response;
    }
}
