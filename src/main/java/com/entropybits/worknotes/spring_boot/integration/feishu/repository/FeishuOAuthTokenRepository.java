/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.feishu.repository;

import com.entropybits.worknotes.spring_boot.integration.feishu.entity.FeishuOAuthToken;
import com.entropybits.worknotes.spring_boot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeishuOAuthTokenRepository extends JpaRepository<FeishuOAuthToken, Long> {
    Optional<FeishuOAuthToken> findByUser(User user);
}


