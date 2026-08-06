/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.repository;

import com.entropybits.worknotes.spring_boot.entity.LocalDocDirectory;
import com.entropybits.worknotes.spring_boot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocalDocDirectoryRepository extends JpaRepository<LocalDocDirectory, Long> {

    List<LocalDocDirectory> findByOwnerOrderByCreatedAtDesc(User owner);

    Optional<LocalDocDirectory> findByOwnerAndDirPath(User owner, String dirPath);
}
