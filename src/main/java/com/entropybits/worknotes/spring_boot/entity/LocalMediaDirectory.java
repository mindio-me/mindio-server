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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "local_media_directories",
    uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "dir_path"}),
    indexes = {
        @Index(name = "idx_lmd_owner", columnList = "owner_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocalMediaDirectory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @NotBlank
    @Size(max = 1000)
    @Column(name = "dir_path", nullable = false, length = 1000)
    private String dirPath;

    @Size(max = 200)
    @Column(name = "display_name", length = 200)
    private String displayName;

    @Column(name = "scan_status", nullable = false, length = 20)
    @Builder.Default
    private String scanStatus = "IDLE";

    @Column(name = "last_scan_at")
    private LocalDateTime lastScanAt;

    @Column(name = "last_scan_error", length = Integer.MAX_VALUE)
    private String lastScanError;

    @Column(name = "file_count", nullable = false)
    @Builder.Default
    private Integer fileCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "directory", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LocalMediaFile> files = new ArrayList<>();
}
