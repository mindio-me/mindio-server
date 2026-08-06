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
 * Project 实体 - 项目作品表
 */
@Entity
@Table(name = "projects")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "项目名称不能为空")
    @Size(max = 200, message = "项目名称长度不能超过200个字符")
    @Column(nullable = false, length = 200)
    private String name;

    @Size(max = 20, message = "项目简称长度不能超过20个字符")
    @Column(length = 20)
    private String shortName; // 项目简称/缩写，如 "WN"、"DC"

    @Column(length = 200)
    private String subtitle; // 项目副标题，如 "SaaS for dance training institutions"

    @NotBlank(message = "项目描述不能为空")
    @Column(nullable = false, length = Integer.MAX_VALUE)
    private String description;

    @Column(length = 100)
    private String icon; // 项目图标，Element UI 图标类名

    @Column(length = 500)
    private String imageUrl; // 项目封面图片

    @Column(length = 500)
    private String projectUrl; // 项目链接

    @Column(length = 500)
    private String githubUrl; // GitHub 链接

    @Column(length = 100)
    private String category; // 项目分类: web, saas, mobile, ai

    @Column(length = Integer.MAX_VALUE)
    private String technologies; // 技术栈，逗号分隔

    @Column(length = Integer.MAX_VALUE)
    private String content; // 项目正文内容（富文本）

    @Column(length = 20)
    @Builder.Default
    private String contentType = "richtext"; // 内容类型：richtext, markdown等

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isPublic = true; // 是否公开展示

    @Column(nullable = false)
    @Builder.Default
    private Boolean isFeatured = false; // 是否为精选项目

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
