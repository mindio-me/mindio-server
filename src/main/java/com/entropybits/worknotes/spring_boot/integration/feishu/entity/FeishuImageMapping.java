/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.feishu.entity;

import com.entropybits.worknotes.spring_boot.entity.Attachment;
import com.entropybits.worknotes.spring_boot.entity.Note;
import com.entropybits.worknotes.spring_boot.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 飞书图片映射实体
 * <p>
 * 追踪从飞书文档导入时下载的图片，支持：
 * - 图片下载状态追踪
 * - 图片URL映射（飞书token → 本地URL）
 * - 图片元数据保存
 * - 失败重试
 */
@Entity
@Table(name = "feishu_image_mappings", indexes = {
    @Index(name = "idx_snapshot_id", columnList = "snapshot_id"),
    @Index(name = "idx_note_id", columnList = "note_id"),
    @Index(name = "idx_document_id", columnList = "document_id"),
    @Index(name = "idx_user_id", columnList = "user_id"),
    // 注意：idx_file_token 使用前缀索引（191字符），在迁移脚本 V5 中定义
    // 这里不定义，避免 Hibernate 自动创建完整字段索引导致错误
    @Index(name = "idx_download_status", columnList = "download_status")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_file_token", columnNames = "file_token")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeishuImageMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========== 飞书标识 ==========

    /**
     * 飞书图片的 file_token（唯一标识）
     */
    @Column(name = "file_token", length = 255, nullable = false, unique = true)
    private String fileToken;

    /**
     * 图片所在的 block_id
     */
    @Column(name = "block_id", length = 128)
    private String blockId;

    /**
     * 飞书文档ID
     */
    @Column(name = "document_id", length = 128, nullable = false)
    private String documentId;

    // ========== 关联关系 ==========

    /**
     * 关联的快照ID（必须）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private FeishuDocumentSnapshot snapshot;

    /**
     * 关联的笔记ID（可能为空）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id")
    private Note note;

    /**
     * 关联的附件ID（上传成功后关联）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attachment_id")
    private Attachment attachment;

    /**
     * 导入用户ID
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ========== 元数据 ==========

    /**
     * 图片宽度（像素）
     */
    @Column(name = "width")
    private Integer width;

    /**
     * 图片高度（像素）
     */
    @Column(name = "height")
    private Integer height;

    /**
     * 对齐方式
     * 1: 居左排版
     * 2: 居中排版
     * 3: 居右排版
     */
    @Column(name = "align")
    private Integer align;

    /**
     * 图片描述
     */
    @Column(name = "caption", length = Integer.MAX_VALUE)
    private String caption;

    // ========== 下载状态 ==========

    /**
     * 下载状态
     * - pending: 等待下载
     * - success: 下载成功
     * - failed: 下载失败
     */
    @Column(name = "download_status", length = 32, nullable = false)
    @Builder.Default
    private String downloadStatus = "pending";

    /**
     * 下载失败时的错误信息
     */
    @Column(name = "download_error_message", length = Integer.MAX_VALUE)
    private String downloadErrorMessage;

    /**
     * 飞书临时下载URL（有效期约4小时）
     */
    @Column(name = "feishu_url", length = 1000)
    private String feishuUrl;

    /**
     * 本地存储URL（上传成功后）
     */
    @Column(name = "local_url", length = 500)
    private String localUrl;

    // ========== 审计 ==========

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 下载完成时间
     */
    @Column(name = "downloaded_at")
    private LocalDateTime downloadedAt;
}
