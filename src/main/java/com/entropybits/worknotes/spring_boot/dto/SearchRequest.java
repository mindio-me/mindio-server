/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 搜索请求 DTO - 支持多条件组合搜索
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {

    /**
     * 搜索关键词（在标题、内容、摘要中搜索）
     */
    private String keyword;

    /**
     * 是否公开（null 表示不筛选）
     */
    private Boolean isPublic;

    /**
     * 标签 ID 集合（null 或空集合表示不筛选）
     */
    private Set<Long> tagIds = new HashSet<>();

    /**
     * 开始日期（创建时间 >= startDate）
     */
    private LocalDateTime startDate;

    /**
     * 结束日期（创建时间 <= endDate）
     */
    private LocalDateTime endDate;
}

