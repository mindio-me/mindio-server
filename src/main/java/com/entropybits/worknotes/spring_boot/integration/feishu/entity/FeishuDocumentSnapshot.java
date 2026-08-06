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

import java.time.LocalDateTime;

/**
 * 飞书文档原始数据快照实体
 * <p>
 * 保存从飞书导入的原始数据，支持：
 * - 数据完整性验证
 * - 重新转换
 * - 版本追踪
 * - 双向同步基线
 */
@Entity
@Table(name = "feishu_document_snapshots", indexes = {
    @Index(name = "idx_mapping_id", columnList = "mapping_id"),
    @Index(name = "idx_note_id", columnList = "note_id"),
    @Index(name = "idx_document_id", columnList = "document_id"),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_created_at", columnList = "created_at"),
    @Index(name = "idx_mapping_created", columnList = "mapping_id,created_at"),
    @Index(name = "idx_document_created", columnList = "document_id,created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeishuDocumentSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========== 关联关系 ==========

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mapping_id", nullable = false)
    private FeishuWikiImportMapping mapping;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id")
    private Note note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ========== 飞书文档标识 ==========

    @Column(name = "document_id", length = 128, nullable = false)
    private String documentId;

    @Column(name = "document_revision_id")
    private Long documentRevisionId;

    // ========== 原始数据（两种格式） ==========

    /**
     * raw_content API 返回的 markdown
     * 可能为空（如果直接使用blocks API）
     * 可能被截断（如果文档过大）
     */
    @Column(name = "raw_markdown", length = Integer.MAX_VALUE)
    private String rawMarkdown;

    /**
     * raw_markdown 是否被截断
     */
    @Column(name = "raw_markdown_truncated")
    @Builder.Default
    private Boolean rawMarkdownTruncated = false;

    /**
     * blocks API 返回的完整 JSON
     * 必须保存，作为数据的"真实来源"
     */
    @Column(name = "blocks_json", nullable = false, length = Integer.MAX_VALUE)
    private String blocksJson;

    /**
     * blocks 总数
     */
    @Column(name = "blocks_count")
    @Builder.Default
    private Integer blocksCount = 0;

    /**
     * blocks 分页数
     */
    @Column(name = "blocks_pages")
    @Builder.Default
    private Integer blocksPages = 0;

    // ========== 元数据 ==========

    /**
     * 飞书文档修改时间（API返回的字符串）
     */
    @Column(name = "feishu_modified_time", length = 64)
    private String feishuModifiedTime;

    /**
     * 原始数据总大小（字节）
     * = raw_markdown.bytes + blocks_json.bytes
     */
    @Column(name = "content_size_bytes")
    private Integer contentSizeBytes;

    /**
     * 是否启用了压缩存储
     */
    @Column(name = "is_compressed")
    @Builder.Default
    private Boolean isCompressed = false;

    // ========== 转换结果 ==========

    /**
     * 转换后的 markdown（冗余存储，便于查询）
     * 用于：
     * - 快速查看转换结果
     * - 对比本地修改（双向同步）
     */
    @Column(name = "converted_markdown", length = Integer.MAX_VALUE)
    private String convertedMarkdown;

    /**
     * 转换策略
     * - raw_content: 使用 raw_content API
     * - blocks_api: 使用 blocks API
     * - hybrid: 两者结合
     */
    @Column(name = "conversion_strategy", length = 32)
    private String conversionStrategy;

    // ========== 导入状态 ==========

    /**
     * 导入状态
     * - success: 成功
     * - partial: 部分成功（某些块转换失败）
     * - failed: 失败
     */
    @Column(name = "import_status", length = 32)
    @Builder.Default
    private String importStatus = "success";

    /**
     * 导入错误信息
     */
    @Column(name = "import_error_message", length = Integer.MAX_VALUE)
    private String importErrorMessage;

    // ========== 同步追踪（预留） ==========

    /**
     * 同步方向
     * - feishu_to_local: 飞书→本地（导入）
     * - local_to_feishu: 本地→飞书（推送）
     */
    @Column(name = "sync_direction", length = 32)
    private String syncDirection;

    /**
     * 最后同步时间
     */
    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    // ========== 审计 ==========

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
