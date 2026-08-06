/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.feishu.entity;

import com.entropybits.worknotes.spring_boot.entity.Note;
import com.entropybits.worknotes.spring_boot.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "feishu_wiki_import_mappings",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "space_id", "node_token"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeishuWikiImportMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "space_id", length = 128, nullable = false)
    private String spaceId;

    @Column(name = "node_token", length = 128, nullable = false)
    private String nodeToken;

    @Column(name = "node_title", length = 512)
    private String nodeTitle;

    @Column(name = "obj_type", length = 32)
    private String objType;

    @Column(name = "source_url", length = 1024)
    private String sourceUrl;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id")
    private Note note;

    @Column(name = "source_modified_at")
    private LocalDateTime sourceModifiedAt;

    @Column(name = "content_hash", length = 128)
    private String contentHash;

    // ========== 快照关联（Phase 0 新增） ==========

    /**
     * 最新快照
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "latest_snapshot_id")
    private FeishuDocumentSnapshot latestSnapshot;

    /**
     * 快照总数
     */
    @Column(name = "total_snapshots")
    @Builder.Default
    private Integer totalSnapshots = 0;

    /**
     * 是否启用同步
     */
    @Column(name = "sync_enabled")
    @Builder.Default
    private Boolean syncEnabled = false;

    /**
     * 所有快照（历史记录）
     */
    @OneToMany(mappedBy = "mapping", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FeishuDocumentSnapshot> snapshots = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}


