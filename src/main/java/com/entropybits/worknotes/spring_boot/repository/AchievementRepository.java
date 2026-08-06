/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.repository;

import com.entropybits.worknotes.spring_boot.entity.Achievement;
import com.entropybits.worknotes.spring_boot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Achievement Repository
 */
@Repository
public interface AchievementRepository extends JpaRepository<Achievement, Long> {

    /**
     * 查找用户的所有成就
     */
    List<Achievement> findByOwnerOrderByDisplayOrderAsc(User owner);

    /**
     * 查找所有公开展示的成就
     */
    List<Achievement> findByIsActiveTrueOrderByDisplayOrderAsc();
}
