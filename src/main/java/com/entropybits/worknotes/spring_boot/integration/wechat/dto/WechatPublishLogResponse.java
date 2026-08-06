/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.wechat.dto;

import com.entropybits.worknotes.spring_boot.integration.wechat.entity.WechatPublishLog;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class WechatPublishLogResponse {
    private Long id;
    private Long noteId;
    private String wxTitle;
    private String wxAuthor;
    private String mediaId;
    private String publishId;
    private String mode;
    private String status;
    private String errorMessage;
    private LocalDateTime createdAt;

    public static WechatPublishLogResponse fromEntity(WechatPublishLog log) {
        return WechatPublishLogResponse.builder()
            .id(log.getId())
            .noteId(log.getNote().getId())
            .wxTitle(log.getWxTitle())
            .wxAuthor(log.getWxAuthor())
            .mediaId(log.getMediaId())
            .publishId(log.getPublishId())
            .mode(log.getMode())
            .status(log.getStatus())
            .errorMessage(log.getErrorMessage())
            .createdAt(log.getCreatedAt())
            .build();
    }
}
