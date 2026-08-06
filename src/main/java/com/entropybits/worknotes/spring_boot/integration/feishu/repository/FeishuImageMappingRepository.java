/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.feishu.repository;

import com.entropybits.worknotes.spring_boot.entity.Note;
import com.entropybits.worknotes.spring_boot.integration.feishu.entity.FeishuDocumentSnapshot;
import com.entropybits.worknotes.spring_boot.integration.feishu.entity.FeishuImageMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 飞书图片映射 Repository
 */
@Repository
public interface FeishuImageMappingRepository extends JpaRepository<FeishuImageMapping, Long> {

    /**
     * 根据 file_token 查询图片映射（唯一）
     */
    Optional<FeishuImageMapping> findByFileToken(String fileToken);

    /**
     * 查询某个快照的所有图片
     */
    List<FeishuImageMapping> findBySnapshotOrderByCreatedAtAsc(FeishuDocumentSnapshot snapshot);

    /**
     * 查询某个笔记的所有图片
     */
    List<FeishuImageMapping> findByNoteOrderByCreatedAtAsc(Note note);

    /**
     * 查询某个文档的所有图片
     */
    List<FeishuImageMapping> findByDocumentIdOrderByCreatedAtAsc(String documentId);

    /**
     * 查询某个用户的所有图片
     */
    List<FeishuImageMapping> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 查询某个状态的所有图片
     */
    List<FeishuImageMapping> findByDownloadStatusOrderByCreatedAtDesc(String status);

    /**
     * 查询某个快照中指定状态的图片
     */
    List<FeishuImageMapping> findBySnapshotAndDownloadStatusOrderByCreatedAtAsc(
            FeishuDocumentSnapshot snapshot, String status);

    /**
     * 统计某个快照的图片数量
     */
    long countBySnapshot(FeishuDocumentSnapshot snapshot);

    /**
     * 统计某个快照中指定状态的图片数量
     */
    long countBySnapshotAndDownloadStatus(FeishuDocumentSnapshot snapshot, String status);

    /**
     * 按下载状态统计图片数量
     */
    @Query("SELECT i.downloadStatus, COUNT(i) FROM FeishuImageMapping i GROUP BY i.downloadStatus")
    List<Object[]> countByDownloadStatus();

    /**
     * 查询失败的图片（用于重试）
     */
    @Query("SELECT i FROM FeishuImageMapping i WHERE i.downloadStatus = 'failed' " +
           "ORDER BY i.createdAt DESC")
    List<FeishuImageMapping> findFailedImages();

    /**
     * 查询待下载的图片（用于异步处理）
     */
    @Query("SELECT i FROM FeishuImageMapping i WHERE i.downloadStatus = 'pending' " +
           "ORDER BY i.createdAt ASC")
    List<FeishuImageMapping> findPendingImages();

    /**
     * 检查 file_token 是否已存在
     */
    boolean existsByFileToken(String fileToken);
}
