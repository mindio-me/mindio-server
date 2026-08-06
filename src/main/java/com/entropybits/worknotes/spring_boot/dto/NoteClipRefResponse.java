/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.dto;

import com.entropybits.worknotes.spring_boot.entity.NoteClipRef;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NoteClipRefResponse {

    private Long refId;
    private Long noteId;
    private String userNote;
    private Integer sortOrder;
    private LocalDateTime linkedAt;
    private SourceClipResponse clip; // excerpt only, no full content

    public static NoteClipRefResponse fromEntity(NoteClipRef ref) {
        return NoteClipRefResponse.builder()
                .refId(ref.getId())
                .noteId(ref.getNote().getId())
                .userNote(ref.getUserNote())
                .sortOrder(ref.getSortOrder())
                .linkedAt(ref.getLinkedAt())
                .clip(SourceClipResponse.fromEntitySummary(ref.getClip()))
                .build();
    }
}
