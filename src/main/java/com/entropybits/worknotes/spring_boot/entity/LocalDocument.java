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

@Entity
@Table(name = "local_documents",
    indexes = {
        @Index(name = "idx_ld_directory", columnList = "directory_id"),
        @Index(name = "idx_ld_owner", columnList = "owner_id"),
        @Index(name = "idx_ld_file_type", columnList = "file_type"),
        @Index(name = "idx_ld_file_name", columnList = "file_name")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocalDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "directory_id", nullable = false)
    private LocalDocDirectory directory;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @NotBlank
    @Size(max = 500)
    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @NotBlank
    @Column(name = "absolute_path", nullable = false, length = 2000)
    private String absolutePath;

    @Column(name = "relative_path", length = 2000)
    private String relativePath;

    @NotBlank
    @Size(max = 20)
    @Column(name = "file_type", nullable = false, length = 20)
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_last_modified")
    private LocalDateTime fileLastModified;

    @CreationTimestamp
    @Column(name = "snapshot_created_at", nullable = false, updatable = false)
    private LocalDateTime snapshotCreatedAt;
}
