/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.reddit.repository;

import com.entropybits.worknotes.spring_boot.entity.User;
import com.entropybits.worknotes.spring_boot.integration.reddit.entity.RedditUserAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RedditUserAuthRepository extends JpaRepository<RedditUserAuth, Long> {
    Optional<RedditUserAuth> findByUser(User user);
}
