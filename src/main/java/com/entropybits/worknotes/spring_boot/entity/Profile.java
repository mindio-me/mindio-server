/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Profile 实体 - 用户个人资料表
 */
@Entity
@Table(name = "profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 100)
    private String fullName; // 全名

    @Column(length = 200)
    private String title; // 职位/头衔

    @Column(length = Integer.MAX_VALUE)
    private String bio; // 个人简介

    @Column(length = 500)
    private String avatarUrl; // 头像URL

    @Column(length = 200)
    private String location; // 地理位置

    @Column(length = 200)
    private String website; // 个人网站

    @Column(length = 200)
    private String github; // GitHub 链接

    @Column(length = 200)
    private String linkedin; // LinkedIn 链接

    @Column(length = 200)
    private String twitter; // Twitter 链接

    @Column(length = 200)
    private String wechat; // 微信/WeChat

    @Column(length = 500)
    private String wechatQrUrl; // 微信二维码图片URL

    @Column(length = 200)
    private String email; // 联系邮箱

    // 网站相关信息
    @Column(length = 100)
    private String siteName; // 网站名称

    @Column(length = 500)
    private String logoUrl; // Logo URL

    @Column(length = Integer.MAX_VALUE)
    private String skills; // 技能列表，JSON格式

    @Column(length = Integer.MAX_VALUE)
    private String experience; // 工作经历，JSON格式

    @Column(length = Integer.MAX_VALUE)
    private String education; // 教育背景，JSON格式

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime modifiedAt;
}
