/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Resource 实体 - 资源工具表
 */
@Entity
@Table(name = "resources")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "资源名称不能为空")
    @Size(max = 200, message = "资源名称长度不能超过200个字符")
    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = Integer.MAX_VALUE)
    private String description;

    @Column(nullable = false, length = 500)
    private String url; // 资源链接

    @Column(length = 100)
    private String icon; // Element UI 图标名称

    @NotBlank(message = "资源分类不能为空")
    @Column(nullable = false, length = 100)
    private String category; // 分类: ai-tools, dev-tools, frameworks, learning

    @Column(length = Integer.MAX_VALUE)
    private String tags; // 标签，逗号分隔

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isPublic = true; // 是否公开展示

    @Column(nullable = false)
    @Builder.Default
    private Boolean isFeatured = false; // 是否为推荐资源

    @Column(nullable = false)
    @Builder.Default
    private Integer displayOrder = 0; // 显示顺序

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime modifiedAt;
}
