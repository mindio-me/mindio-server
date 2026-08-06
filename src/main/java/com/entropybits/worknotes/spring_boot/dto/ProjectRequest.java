/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 项目请求 DTO
 */
@Data
public class ProjectRequest {

    @NotBlank(message = "项目名称不能为空")
    @Size(max = 200, message = "项目名称长度不能超过200个字符")
    private String name;

    @Size(max = 20, message = "项目简称长度不能超过20个字符")
    private String shortName; // 项目简称/缩写

    private String subtitle;

    @NotBlank(message = "项目描述不能为空")
    private String description;

    private String icon;

    private String imageUrl;

    private String projectUrl;

    private String githubUrl;

    private String category;

    private String technologies;

    private String content; // 项目正文内容（富文本）

    private String contentType = "richtext"; // 内容类型：richtext, markdown等

    private Boolean isPublic = true;

    private Boolean isFeatured = false;

    private Integer displayOrder = 0;
}
