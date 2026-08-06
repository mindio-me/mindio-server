/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.repository;

import com.entropybits.worknotes.spring_boot.entity.Profile;
import com.entropybits.worknotes.spring_boot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Profile Repository
 */
@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {

    /**
     * 通过用户查找个人资料
     */
    Optional<Profile> findByUser(User user);

    /**
     * 通过用户ID查找个人资料
     */
    Optional<Profile> findByUserId(Long userId);

    /**
     * 检查用户是否已有个人资料
     */
    boolean existsByUser(User user);

    /**
     * 获取第一条 profile 记录（站点 Owner）
     */
    Optional<Profile> findFirstByOrderByIdAsc();
}
