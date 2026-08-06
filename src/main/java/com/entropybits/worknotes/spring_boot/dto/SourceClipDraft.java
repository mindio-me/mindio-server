/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.dto;

import com.entropybits.worknotes.spring_boot.entity.SourceClip;
import lombok.Builder;
import lombok.Data;

/**
 * URL 抓取的预览草稿，不入库，由前端确认后再提交 SourceClipRequest。
 */
@Data
@Builder
public class SourceClipDraft {

    private SourceClip.SourceType sourceType;
    private SourceClip.ExtractionMode extractionMode;
    private SourceClip.ExtractionStatus extractionStatus;
    private String sourceUrl;
    private String sourceTitle;
    private String sourceAuthor;
    private String suggestedTitle;   // 建议给用户的标题（可编辑）
    private String content;
    private String contentFormat;
    /** true 表示未阻塞保存流程（FULL 模式下正文抓取失败时仍为 true，只是 content 为空） */
    private boolean fetchSuccess;
    private String fetchFailReason;
}
