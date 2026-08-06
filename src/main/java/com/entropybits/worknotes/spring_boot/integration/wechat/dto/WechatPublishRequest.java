/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.wechat.dto;

import lombok.Data;

@Data
public class WechatPublishRequest {
    private String title;        // 可选，覆盖笔记标题
    private String author;       // 可选
    private String digest;       // 可选，覆盖自动摘要
    private String thumbMediaId;   // 可选，前端已上传的封面素材ID
    private String coverImageUrl;  // 可选，本地图片 URL，后端下载后上传微信获取 thumbMediaId
}
