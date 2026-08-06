/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.dto;

import com.entropybits.worknotes.spring_boot.entity.SourceClip;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
public class SourceClipResponse {

    private Long id;
    private SourceClip.SourceType sourceType;
    private String sourceUrl;
    private String sourceTitle;
    private String sourceAuthor;
    private SourceClip.ExtractionMode extractionMode;
    private SourceClip.ExtractionStatus extractionStatus;
    private String title;
    private String content;   // null in list responses
    private String contentFormat;
    private String excerpt;
    private Set<TagResponse> tags;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;

    /** 列表用：不含 content 全文 */
    public static SourceClipResponse fromEntitySummary(SourceClip c) {
        return SourceClipResponse.builder()
                .id(c.getId())
                .sourceType(c.getSourceType())
                .sourceUrl(c.getSourceUrl())
                .sourceTitle(c.getSourceTitle())
                .sourceAuthor(c.getSourceAuthor())
                .extractionMode(c.getExtractionMode())
                .extractionStatus(c.getExtractionStatus())
                .title(c.getTitle())
                .contentFormat(c.getContentFormat())
                .excerpt(c.getExcerpt())
                .tags(c.getTags().stream().map(TagResponse::fromEntity).collect(Collectors.toSet()))
                .createdAt(c.getCreatedAt())
                .modifiedAt(c.getModifiedAt())
                .build();
    }

    /** 详情用：含 content 全文 */
    public static SourceClipResponse fromEntity(SourceClip c) {
        SourceClipResponse r = fromEntitySummary(c);
        r.setContent(c.getContent());
        return r;
    }
}
