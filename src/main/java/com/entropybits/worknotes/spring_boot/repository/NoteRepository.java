/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.repository;

import com.entropybits.worknotes.spring_boot.entity.Note;
import com.entropybits.worknotes.spring_boot.entity.Project;
import com.entropybits.worknotes.spring_boot.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * NoteRepository - 笔记数据访问层
 */
@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {

    /**
     * 查找用户的所有笔记（分页）
     */
    Page<Note> findByOwner(User owner, Pageable pageable);

    /**
     * 查找用户的所有公开笔记（分页）
     */
    Page<Note> findByOwnerAndIsPublic(User owner, Boolean isPublic, Pageable pageable);

    /**
     * 根据标题模糊搜索用户的笔记（分页）
     */
    Page<Note> findByOwnerAndTitleContaining(User owner, String title, Pageable pageable);

    /**
     * 查找所有公开笔记（分页）
     */
    Page<Note> findByIsPublic(Boolean isPublic, Pageable pageable);

    /**
     * 查找所有公开笔记（不分页，按修改时间倒序），用于站点导出
     */
    List<Note> findByIsPublicTrueOrderByModifiedAtDesc();

    /**
     * 根据标签查找笔记
     */
    @Query("SELECT n FROM Note n JOIN n.tags t WHERE n.owner = :owner AND t.id IN :tagIds")
    Page<Note> findByOwnerAndTagIds(@Param("owner") User owner, @Param("tagIds") Iterable<Long> tagIds, Pageable pageable);

    /**
     * 全文搜索：在标题和内容中搜索关键词
     */
    @Query("SELECT n FROM Note n WHERE n.owner = :owner AND " +
           "(LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(n.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(n.summary) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Note> fullTextSearch(@Param("owner") User owner, @Param("keyword") String keyword, Pageable pageable);

    /**
     * 多条件组合搜索
     */
    @Query("SELECT DISTINCT n FROM Note n LEFT JOIN n.tags t WHERE n.owner = :owner " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "     LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(n.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     (n.summary IS NOT NULL AND LOWER(n.summary) LIKE LOWER(CONCAT('%', :keyword, '%')))) " +
           "AND (:isPublic IS NULL OR n.isPublic = :isPublic) " +
           "AND (:tagIds IS NULL OR t.id IN :tagIds) " +
           "AND (:startDate IS NULL OR n.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR n.createdAt <= :endDate)")
    Page<Note> advancedSearch(
            @Param("owner") User owner,
            @Param("keyword") String keyword,
            @Param("isPublic") Boolean isPublic,
            @Param("tagIds") Set<Long> tagIds,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * 根据ID查找笔记并同时加载 owner（避免懒加载异常）
     */
    @Query("SELECT n FROM Note n JOIN FETCH n.owner WHERE n.id = :id")
    Optional<Note> findByIdWithOwner(@Param("id") Long id);

    /**
     * 查找关联指定项目的所有笔记（用于级联删除）
     */
    List<Note> findByProject(Project project);

    /**
     * 查找用户当前有效的自动生成结果（知识地图/时间线回顾），每个用户每种类型最多一条
     */
    Optional<Note> findByOwnerAndGeneratedType(User owner, Note.GeneratedType generatedType);

    /**
     * 根据项目ID列表查找用户的笔记（支持多选筛选）
     */
    @Query("SELECT n FROM Note n WHERE n.owner = :owner AND n.project.id IN :projectIds")
    Page<Note> findByOwnerAndProjectIdIn(@Param("owner") User owner, @Param("projectIds") List<Long> projectIds, Pageable pageable);
}
