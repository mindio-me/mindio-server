/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "source_clips")
@Getter
@Setter
@ToString(exclude = "tags")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SourceClip {

    public enum SourceType {
        WEBPAGE, WECHAT_ARTICLE, WECHAT_CHAT_TEXT, WECHAT_CHAT_IMAGE
    }

    public enum ExtractionMode {
        FULL, LINK_ONLY
    }

    public enum ExtractionStatus {
        SUCCESS, FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SourceType sourceType;

    @Column(length = 1000)
    private String sourceUrl;

    @Column(length = 500)
    private String sourceTitle;

    @Column(length = 200)
    private String sourceAuthor;

    @Column(length = 100)
    private String aiModel;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = Integer.MAX_VALUE)
    private String content;

    @Column(length = 20)
    private String contentFormat; // html | markdown | text

    @Column(length = Integer.MAX_VALUE)
    private String excerpt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ExtractionMode extractionMode = ExtractionMode.FULL;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ExtractionStatus extractionStatus;

    @ManyToMany
    @JoinTable(
            name = "source_clip_tags",
            joinColumns = @JoinColumn(name = "clip_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime modifiedAt;

    private LocalDateTime originalBookmarkedAt; // 可空；来自导入的书签原始 ADD_DATE，其余创建途径为 null

    @Column(nullable = false)
    @Builder.Default
    private Boolean wasDetectedDeadLink = false; // AI 体检阶段曾判定为疑似失效

    @Column(nullable = false)
    @Builder.Default
    private Boolean manuallyConfirmedAlive = false; // 用户人工判断这个链接其实正常，修正了 AI 的判断

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SourceClip that = (SourceClip) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
