/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.feishu.repository;

import com.entropybits.worknotes.spring_boot.entity.Note;
import com.entropybits.worknotes.spring_boot.entity.User;
import com.entropybits.worknotes.spring_boot.integration.feishu.entity.FeishuWikiImportMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeishuWikiImportMappingRepository extends JpaRepository<FeishuWikiImportMapping, Long> {
    Optional<FeishuWikiImportMapping> findByUserAndSpaceIdAndNodeToken(User user, String spaceId, String nodeToken);
    Optional<FeishuWikiImportMapping> findByNote(Note note);
}


