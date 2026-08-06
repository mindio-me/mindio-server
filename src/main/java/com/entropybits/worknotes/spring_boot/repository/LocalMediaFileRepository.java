/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.repository;

import com.entropybits.worknotes.spring_boot.entity.LocalMediaDirectory;
import com.entropybits.worknotes.spring_boot.entity.LocalMediaFile;
import com.entropybits.worknotes.spring_boot.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LocalMediaFileRepository extends JpaRepository<LocalMediaFile, Long> {

    @Query("SELECT f FROM LocalMediaFile f WHERE f.directory = :directory " +
           "AND (:keyword IS NULL OR :keyword = '' OR LOWER(f.fileName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:mediaType IS NULL OR :mediaType = '' OR f.mediaType = :mediaType)")
    Page<LocalMediaFile> searchInDirectory(
            @Param("directory") LocalMediaDirectory directory,
            @Param("keyword") String keyword,
            @Param("mediaType") String mediaType,
            Pageable pageable);

    @Query("SELECT f FROM LocalMediaFile f WHERE f.owner = :owner " +
           "AND (:keyword IS NULL OR :keyword = '' OR LOWER(f.fileName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:mediaType IS NULL OR :mediaType = '' OR f.mediaType = :mediaType)")
    Page<LocalMediaFile> searchByOwner(
            @Param("owner") User owner,
            @Param("keyword") String keyword,
            @Param("mediaType") String mediaType,
            Pageable pageable);

    void deleteByDirectory(LocalMediaDirectory directory);
}
