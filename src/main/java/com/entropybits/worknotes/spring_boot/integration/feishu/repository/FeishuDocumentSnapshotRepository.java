/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.feishu.repository;

import com.entropybits.worknotes.spring_boot.entity.Note;
import com.entropybits.worknotes.spring_boot.integration.feishu.entity.FeishuDocumentSnapshot;
import com.entropybits.worknotes.spring_boot.integration.feishu.entity.FeishuWikiImportMapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 飞书文档快照 Repository
 */
@Repository
public interface FeishuDocumentSnapshotRepository extends JpaRepository<FeishuDocumentSnapshot, Long> {

    /**
     * 查询某个 mapping 的最新快照
     */
    Optional<FeishuDocumentSnapshot> findFirstByMappingOrderByCreatedAtDesc(FeishuWikiImportMapping mapping);

    /**
     * 查询某个 mapping 的所有快照（按时间倒序）
     */
    List<FeishuDocumentSnapshot> findByMappingOrderByCreatedAtDesc(FeishuWikiImportMapping mapping);

    /**
     * 分页查询某个 mapping 的快照历史
     */
    Page<FeishuDocumentSnapshot> findByMappingOrderByCreatedAtDesc(FeishuWikiImportMapping mapping, Pageable pageable);

    /**
     * 根据文档ID查询最新快照
     */
    Optional<FeishuDocumentSnapshot> findFirstByDocumentIdOrderByCreatedAtDesc(String documentId);

    /**
     * 查询某个笔记的所有快照
     */
    List<FeishuDocumentSnapshot> findByNoteOrderByCreatedAtDesc(Note note);

    /**
     * 查询某个用户的所有快照
     */
    List<FeishuDocumentSnapshot> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 统计某个 mapping 的快照数量
     */
    long countByMapping(FeishuWikiImportMapping mapping);

    /**
     * 查询大于指定大小的快照（用于数据库优化）
     */
    @Query("SELECT s FROM FeishuDocumentSnapshot s WHERE s.contentSizeBytes > :sizeBytes ORDER BY s.contentSizeBytes DESC")
    List<FeishuDocumentSnapshot> findLargeSnapshots(@Param("sizeBytes") int sizeBytes, Pageable pageable);

    /**
     * 查询失败的导入记录
     */
    List<FeishuDocumentSnapshot> findByImportStatusOrderByCreatedAtDesc(String status);

    /**
     * 按导入状态统计
     */
    @Query("SELECT s.importStatus, COUNT(s) FROM FeishuDocumentSnapshot s GROUP BY s.importStatus")
    List<Object[]> countByImportStatus();
}
