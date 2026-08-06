/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 资源请求 DTO
 */
@Data
public class ResourceRequest {

    @NotBlank(message = "资源名称不能为空")
    @Size(max = 200, message = "资源名称长度不能超过200个字符")
    private String name;

    private String description;

    @NotBlank(message = "资源链接不能为空")
    private String url;

    private String icon;

    @NotBlank(message = "资源分类不能为空")
    private String category;

    private String tags;

    private Boolean isPublic = true;

    private Boolean isFeatured = false;

    private Integer displayOrder = 0;
}
