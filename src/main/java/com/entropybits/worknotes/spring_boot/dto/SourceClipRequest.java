/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.dto;

import com.entropybits.worknotes.spring_boot.entity.SourceClip;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class SourceClipRequest {

    @NotNull
    private SourceClip.SourceType sourceType;

    private String sourceUrl;
    private String sourceTitle;
    private String sourceAuthor;

    private SourceClip.ExtractionMode extractionMode;
    private SourceClip.ExtractionStatus extractionStatus;

    @NotBlank
    @Size(max = 200)
    private String title;

    private String content;

    private String contentFormat; // html | markdown | text

    private List<Long> tagIds;
}
