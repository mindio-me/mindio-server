/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.repository;

import com.entropybits.worknotes.spring_boot.entity.Tag;
import com.entropybits.worknotes.spring_boot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * TagRepository - 标签数据访问层
 */
@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    /**
     * 查找用户的所有标签
     */
    List<Tag> findByOwner(User owner);

    /**
     * 根据名称和所有者查找标签
     */
    Optional<Tag> findByNameAndOwner(String name, User owner);

    /**
     * 检查标签名称是否存在（同一用户下）
     */
    Boolean existsByNameAndOwner(String name, User owner);
}
