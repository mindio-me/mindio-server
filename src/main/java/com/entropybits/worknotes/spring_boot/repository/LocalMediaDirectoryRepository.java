/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.repository;

import com.entropybits.worknotes.spring_boot.entity.LocalMediaDirectory;
import com.entropybits.worknotes.spring_boot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocalMediaDirectoryRepository extends JpaRepository<LocalMediaDirectory, Long> {

    List<LocalMediaDirectory> findByOwnerOrderByCreatedAtDesc(User owner);

    Optional<LocalMediaDirectory> findByOwnerAndDirPath(User owner, String dirPath);
}
