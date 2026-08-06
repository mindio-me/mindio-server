/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.dto;

import com.entropybits.worknotes.spring_boot.entity.SourceClip;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClipImportUrlRequest {

    @NotBlank
    private String url;

    private SourceClip.ExtractionMode extractionMode = SourceClip.ExtractionMode.FULL;
}
