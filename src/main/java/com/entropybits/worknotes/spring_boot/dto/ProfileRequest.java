/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.dto;

import lombok.Data;

/**
 * 个人资料请求 DTO
 */
@Data
public class ProfileRequest {

    private String fullName;
    private String title;
    private String bio;
    private String avatarUrl;
    private String location;
    private String website;
    private String github;
    private String linkedin;
    private String twitter;
    private String wechat;
    private String wechatQrUrl;
    private String email;
    private String skills; // JSON格式
    private String experience; // JSON格式
    private String education; // JSON格式
}
