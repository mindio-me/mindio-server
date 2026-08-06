/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.repository;

import com.entropybits.worknotes.spring_boot.entity.LocalDocDirectory;
import com.entropybits.worknotes.spring_boot.entity.LocalDocument;
import com.entropybits.worknotes.spring_boot.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LocalDocumentRepository extends JpaRepository<LocalDocument, Long> {

    @Query("SELECT d FROM LocalDocument d WHERE d.directory = :directory " +
           "AND (:keyword IS NULL OR :keyword = '' OR LOWER(d.fileName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:fileType IS NULL OR :fileType = '' OR d.fileType = :fileType)")
    Page<LocalDocument> searchInDirectory(
            @Param("directory") LocalDocDirectory directory,
            @Param("keyword") String keyword,
            @Param("fileType") String fileType,
            Pageable pageable);

    @Query("SELECT d FROM LocalDocument d WHERE d.owner = :owner " +
           "AND (:keyword IS NULL OR :keyword = '' OR LOWER(d.fileName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:fileType IS NULL OR :fileType = '' OR d.fileType = :fileType)")
    Page<LocalDocument> searchByOwner(
            @Param("owner") User owner,
            @Param("keyword") String keyword,
            @Param("fileType") String fileType,
            Pageable pageable);

    void deleteByDirectory(LocalDocDirectory directory);

    long countByDirectory(LocalDocDirectory directory);
}
