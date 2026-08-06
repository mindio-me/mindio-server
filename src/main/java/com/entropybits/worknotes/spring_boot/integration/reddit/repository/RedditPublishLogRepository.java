/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.reddit.repository;

import com.entropybits.worknotes.spring_boot.integration.reddit.entity.RedditPublishLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RedditPublishLogRepository extends JpaRepository<RedditPublishLog, Long> {
    List<RedditPublishLog> findByNote_IdOrderByCreatedAtDesc(Long noteId);
}
