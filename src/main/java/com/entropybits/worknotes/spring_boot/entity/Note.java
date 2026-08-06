/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Objects;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Note 实体 - 笔记表
 */
@Entity
@Table(name = "notes")
@Getter
@Setter
@ToString(exclude = "tags")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200个字符")
    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "LONGTEXT")
    @Builder.Default
    private String content = "";

    @NotBlank(message = "内容类型不能为空")
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String contentType = "richtext"; // markdown 或 richtext

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isPublic = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime modifiedAt;

    @PrePersist
    protected void prePersist() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (this.createdAt == null) this.createdAt = now;
        if (this.modifiedAt == null) this.modifiedAt = now;
    }

    @PreUpdate
    protected void preUpdate() {
        this.modifiedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    @ManyToMany
    @JoinTable(
        name = "note_tags",
        joinColumns = @JoinColumn(name = "note_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = true)
    private Project project;

    @Column(columnDefinition = "TEXT")
    private String summary; // AI 生成的摘要

    @Column(length = 10)
    private String language; // 'zh', 'en', null 表示未指定

    public enum GeneratedType { CLUSTER, TIMELINE }

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private GeneratedType generatedType; // 可空；CLUSTER/TIMELINE 表示这是自动生成的知识地图/时间线回顾，普通笔记为 null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_note_id", nullable = true)
    private Note sourceNote; // 若为翻译版本则指向原始笔记

    @ElementCollection
    @CollectionTable(name = "note_sections", joinColumns = @JoinColumn(name = "note_id"))
    @Column(name = "section_content", length = Integer.MAX_VALUE)
    @OrderColumn(name = "section_order")
    @Builder.Default
    private List<String> sectionContents = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "note_section_types", joinColumns = @JoinColumn(name = "note_id"))
    @Column(name = "section_type", length = 50)
    @OrderColumn(name = "section_order")
    @Builder.Default
    private List<String> sectionTypes = new ArrayList<>(); // richtext, markdown, ai-chat, iframe, gallery

    @Column(nullable = false)
    @Builder.Default
    private Integer viewCount = 0; // 阅读次数

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Note note = (Note) o;
        return id != null && Objects.equals(id, note.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
