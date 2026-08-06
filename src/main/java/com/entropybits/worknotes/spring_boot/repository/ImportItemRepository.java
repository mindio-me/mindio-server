/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.repository;

import com.entropybits.worknotes.spring_boot.entity.ImportItem;
import com.entropybits.worknotes.spring_boot.entity.ImportJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportItemRepository extends JpaRepository<ImportItem, Long> {

    List<ImportItem> findByJob(ImportJob job);

    List<ImportItem> findByJobAndCategory(ImportJob job, ImportItem.Category category);

    List<ImportItem> findByJobAndCategoryAndUserDecision(
            ImportJob job, ImportItem.Category category, ImportItem.UserDecision userDecision);
}
