/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 成就请求 DTO
 */
@Data
public class AchievementRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200个字符")
    private String title;

    private String subtitle;

    private String type;

    private String status = "active";

    private String icon;

    private String iconVariant = "purple";

    @NotBlank(message = "描述不能为空")
    private String description;

    private String technologies;

    private Boolean isActive = true;

    private Integer displayOrder = 0;
}
