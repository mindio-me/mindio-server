/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.dto;

import com.entropybits.worknotes.spring_boot.entity.Note;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 笔记响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoteResponse {

    private Long id;
    private String title;
    private String content;
    private String contentType;
    private Boolean isPublic;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private Long ownerId;
    private String ownerUsername;
    private Set<TagResponse> tags;
    private Long projectId; // 关联的项目ID
    private String projectName; // 关联的项目名称
    private String summary;
    private List<String> sectionContents;
    private List<String> sectionTypes;
    private Integer viewCount;
    private String language;
    private Long sourceNoteId;

    // 飞书导入相关元数据（如果笔记是从飞书 Wiki 导入）
    private String feishuSpaceId;
    private String feishuNodeToken;
    private String feishuSourceUrl;

    public static NoteResponse fromEntity(Note note) {
        return NoteResponse.builder()
                .id(note.getId())
                .title(note.getTitle())
                .content(note.getContent())
                .contentType(note.getContentType())
                .isPublic(note.getIsPublic())
                .createdAt(note.getCreatedAt())
                .modifiedAt(note.getModifiedAt())
                .ownerId(note.getOwner().getId())
                .ownerUsername(note.getOwner().getUsername())
                .tags(note.getTags().stream()
                        .map(TagResponse::fromEntity)
                        .collect(Collectors.toSet()))
                .projectId(note.getProject() != null ? note.getProject().getId() : null)
                .projectName(note.getProject() != null ? note.getProject().getName() : null)
                .summary(note.getSummary())
                .sectionContents(note.getSectionContents())
                .sectionTypes(note.getSectionTypes())
                .viewCount(note.getViewCount())
                .language(note.getLanguage())
                .sourceNoteId(note.getSourceNote() != null ? note.getSourceNote().getId() : null)
                .build();
    }
}
