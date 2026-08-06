/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.dto;

import com.entropybits.worknotes.spring_boot.entity.BookmarkAgentJob;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BookmarkAgentJobResponse {
    private Long id;
    private BookmarkAgentJob.Type type;
    private BookmarkAgentJob.Status status;
    private int completedSteps;
    private int totalSteps;
    private Long resultNoteId;
    private LocalDateTime finishedAt;
    private String errorMessage;

    public static BookmarkAgentJobResponse empty(BookmarkAgentJob.Type type) {
        return BookmarkAgentJobResponse.builder()
                .type(type)
                .build();
    }

    public static BookmarkAgentJobResponse fromEntity(BookmarkAgentJob job) {
        return BookmarkAgentJobResponse.builder()
                .id(job.getId())
                .type(job.getType())
                .status(job.getStatus())
                .completedSteps(job.getCompletedSteps())
                .totalSteps(job.getTotalSteps())
                .resultNoteId(job.getResultNoteId())
                .finishedAt(job.getFinishedAt())
                .errorMessage(job.getErrorMessage())
                .build();
    }
}
