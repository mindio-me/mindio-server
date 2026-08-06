/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.service;

public interface DeadLinkTrigger {
    /** 异步触发对该 job 里 category=PENDING_CHECK 条目的失效检测，方法本身立即返回，不阻塞调用方 */
    void triggerCheck(Long jobId);
}
