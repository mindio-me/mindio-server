/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.repository;

import com.entropybits.worknotes.spring_boot.entity.ImportJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ImportJobRepository extends JpaRepository<ImportJob, Long> {

    /**
     * @Transactional 是必须的：这个方法从 BookmarkDeadLinkChecker 的后台线程池调用，没有任何环绕事务；
     * @Modifying 查询在没有事务时会抛 TransactionRequiredException，且没人对 CompletableFuture.runAsync
     * 的结果调用 .join()/.exceptionally()，异常被静默吞掉——外部表现就是 checkedCount 永远不增长、
     * job 永远卡在 CHECKING（因为 CompletableFuture.allOf().thenRun() 只在全部正常完成时才触发）。
     */
    @Transactional
    @Modifying
    @Query("UPDATE ImportJob j SET j.checkedCount = j.checkedCount + 1 WHERE j.id = :jobId")
    void incrementCheckedCount(@Param("jobId") Long jobId);
}
