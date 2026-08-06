/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.repository;

import com.entropybits.worknotes.spring_boot.entity.Resource;
import com.entropybits.worknotes.spring_boot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Resource Repository
 */
@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {

    /**
     * 查找用户的所有资源
     */
    List<Resource> findByOwnerOrderByDisplayOrderAsc(User owner);

    /**
     * 查找所有公开资源
     */
    List<Resource> findByIsPublicTrueOrderByDisplayOrderAsc();

    /**
     * 查找所有推荐资源
     */
    List<Resource> findByIsFeaturedTrueAndIsPublicTrueOrderByDisplayOrderAsc();

    /**
     * 按分类查找公开资源
     */
    List<Resource> findByCategoryAndIsPublicTrueOrderByDisplayOrderAsc(String category);

    /**
     * 查找用户的公开资源数量
     */
    long countByOwnerAndIsPublicTrue(User owner);
}
