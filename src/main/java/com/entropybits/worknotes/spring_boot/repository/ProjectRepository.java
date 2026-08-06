/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.repository;

import com.entropybits.worknotes.spring_boot.entity.Project;
import com.entropybits.worknotes.spring_boot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Project Repository
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    /**
     * 查找用户的所有项目
     */
    List<Project> findByOwnerOrderByDisplayOrderAsc(User owner);

    /**
     * 查找所有公开项目
     */
    List<Project> findByIsPublicTrueOrderByDisplayOrderAsc();

    /**
     * 查找所有精选项目
     */
    List<Project> findByIsFeaturedTrueOrderByDisplayOrderAsc();

    /**
     * 按分类查找公开项目
     */
    List<Project> findByCategoryAndIsPublicTrueOrderByDisplayOrderAsc(String category);

    /**
     * 查找用户的公开项目数量
     */
    long countByOwnerAndIsPublicTrue(User owner);
}
