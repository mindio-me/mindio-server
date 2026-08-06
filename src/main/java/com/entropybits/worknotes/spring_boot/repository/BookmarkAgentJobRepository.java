/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.repository;

import com.entropybits.worknotes.spring_boot.entity.BookmarkAgentJob;
import com.entropybits.worknotes.spring_boot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface BookmarkAgentJobRepository extends JpaRepository<BookmarkAgentJob, Long> {

    Optional<BookmarkAgentJob> findTopByOwnerAndTypeOrderByCreatedAtDesc(User owner, BookmarkAgentJob.Type type);

    /**
     * @Transactional 是必须的：这个方法会从 BookmarkAgentService 的后台线程池调用，没有环绕事务；
     * 参照 ImportJobRepository.incrementCheckedCount 已经踩过的坑——@Modifying 查询没有事务会抛
     * TransactionRequiredException，且异常不会被任何调用方感知到。
     */
    @Transactional
    @Modifying
    @Query("UPDATE BookmarkAgentJob j SET j.completedSteps = j.completedSteps + 1 WHERE j.id = :jobId")
    void incrementCompletedSteps(@Param("jobId") Long jobId);

    @Transactional
    @Modifying
    @Query("UPDATE BookmarkAgentJob j SET j.totalSteps = :totalSteps WHERE j.id = :jobId")
    void updateTotalSteps(@Param("jobId") Long jobId, @Param("totalSteps") int totalSteps);
}
