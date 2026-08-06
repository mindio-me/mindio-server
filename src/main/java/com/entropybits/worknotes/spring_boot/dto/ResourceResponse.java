/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.dto;

import com.entropybits.worknotes.spring_boot.entity.Resource;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资源响应 DTO
 */
@Data
public class ResourceResponse {

    private Long id;
    private String name;
    private String description;
    private String url;
    private String icon;
    private String category;
    private String tags;
    private Boolean isPublic;
    private Boolean isFeatured;
    private Integer displayOrder;
    private String ownerUsername;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;

    public static ResourceResponse fromEntity(Resource resource) {
        ResourceResponse response = new ResourceResponse();
        response.setId(resource.getId());
        response.setName(resource.getName());
        response.setDescription(resource.getDescription());
        response.setUrl(resource.getUrl());
        response.setIcon(resource.getIcon());
        response.setCategory(resource.getCategory());
        response.setTags(resource.getTags());
        response.setIsPublic(resource.getIsPublic());
        response.setIsFeatured(resource.getIsFeatured());
        response.setDisplayOrder(resource.getDisplayOrder());
        response.setOwnerUsername(resource.getOwner().getUsername());
        response.setCreatedAt(resource.getCreatedAt());
        response.setModifiedAt(resource.getModifiedAt());
        return response;
    }
}
