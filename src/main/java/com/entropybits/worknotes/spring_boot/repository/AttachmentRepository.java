/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.repository;

import com.entropybits.worknotes.spring_boot.entity.Attachment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 附件管理表数据访问层
 */
@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    /**
     * 根据分类ID和类型查询附件
     */
    Page<Attachment> findByPidAndAttType(Integer pid, String attType, Pageable pageable);

    /**
     * 根据分类ID查询附件
     */
    Page<Attachment> findByPid(Integer pid, Pageable pageable);

    /**
     * 根据用户ID查询附件
     */
    Page<Attachment> findByOwner(Long owner, Pageable pageable);

    /**
     * 根据用户ID和分类ID查询附件
     */
    Page<Attachment> findByOwnerAndPid(Long owner, Integer pid, Pageable pageable);

    /**
     * 根据ID列表删除附件
     */
    void deleteByAttIdIn(List<Long> attIds);

    /**
     * 根据路径查询附件
     */
    Attachment findByAttDir(String attDir);

    /**
     * 根据名称查询附件（按创建时间倒序）
     */
    List<Attachment> findByNameOrderByCreateTimeDesc(String name);
}

