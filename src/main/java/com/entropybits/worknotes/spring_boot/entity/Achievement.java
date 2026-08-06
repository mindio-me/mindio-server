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
 * Achievement 实体 - 成就/成果展示表
 *
 * 与 Project（经验）互相独立：Project 表达"做过什么"的过程，
 * Achievement 表达从某段经验里提炼出的、可量化的结果/成绩，
 * 也可以是完全独立于任何经历记录的成果（如独立产品）。
 */
@Entity
@Table(name = "achievements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200个字符")
    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 200)
    private String subtitle;

    @Column(length = 100)
    private String type; // 自由文本，如：独立产品、比赛获奖、实习成果

    @Column(length = 20)
    @Builder.Default
    private String status = "active"; // active(进行中) | done(已完成)，前端负责映射展示文案

    @Column(length = 100)
    private String icon; // Element UI 图标名称

    @Column(length = 20)
    @Builder.Default
    private String iconVariant = "purple"; // 图标底色变体，如 purple/green/blue

    @NotBlank(message = "描述不能为空")
    @Column(nullable = false, length = Integer.MAX_VALUE)
    private String description;

    @Column(length = Integer.MAX_VALUE)
    private String technologies; // 逗号分隔的技术栈标签

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true; // 是否公开展示

    @Column(nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime modifiedAt;
}
