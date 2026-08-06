/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.dto;

import com.entropybits.worknotes.spring_boot.entity.ImportItem;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ImportItemResponse {
    private Long id;
    private String rawTitle;
    private String rawUrl;
    private String folderPath;
    private LocalDateTime bookmarkAddedAt;
    private ImportItem.Category category;
    private ImportItem.NoiseReason noiseReason;
    private Long duplicateOfClipId;
    private String httpStatus;
    private ImportItem.UserDecision userDecision;

    public static ImportItemResponse fromEntity(ImportItem i) {
        return ImportItemResponse.builder()
                .id(i.getId())
                .rawTitle(i.getRawTitle())
                .rawUrl(i.getRawUrl())
                .folderPath(i.getFolderPath())
                .bookmarkAddedAt(i.getBookmarkAddedAt())
                .category(i.getCategory())
                .noiseReason(i.getNoiseReason())
                .duplicateOfClipId(i.getDuplicateOfClipId())
                .httpStatus(i.getHttpStatus())
                .userDecision(i.getUserDecision())
                .build();
    }
}
