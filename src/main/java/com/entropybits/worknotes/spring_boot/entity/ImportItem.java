/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "import_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportItem {

    public enum Category {
        NOISE, DUPLICATE, PENDING_CHECK, IMPORTABLE, DEAD_LINK
    }

    public enum NoiseReason {
        JS_BOOKMARKLET, INTERNAL_SCHEME
    }

    public enum UserDecision {
        PENDING, CONFIRMED, SKIPPED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private ImportJob job;

    @Column(length = 500)
    private String rawTitle;

    @Column(nullable = false, length = 2000)
    private String rawUrl;

    @Column(nullable = false, length = 2000)
    private String normalizedUrl;

    @Column(length = 500)
    private String folderPath;

    private LocalDateTime bookmarkAddedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private NoiseReason noiseReason;

    private Long duplicateOfClipId;

    @Column(length = 20)
    private String httpStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserDecision userDecision = UserDecision.PENDING;

    private Long resultClipId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImportItem that = (ImportItem) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
