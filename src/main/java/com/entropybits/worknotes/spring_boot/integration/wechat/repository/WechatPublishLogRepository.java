/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.wechat.repository;

import com.entropybits.worknotes.spring_boot.integration.wechat.entity.WechatPublishLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WechatPublishLogRepository extends JpaRepository<WechatPublishLog, Long> {

    List<WechatPublishLog> findByNoteIdOrderByCreatedAtDesc(Long noteId);

    long countByModeAndStatusAndCreatedAtBetween(
        String mode, String status, LocalDateTime start, LocalDateTime end);
}
