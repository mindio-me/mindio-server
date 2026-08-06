/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.repository;

import com.entropybits.worknotes.spring_boot.entity.SourceClip;
import com.entropybits.worknotes.spring_boot.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SourceClipRepository extends JpaRepository<SourceClip, Long> {

    Page<SourceClip> findByOwner(User owner, Pageable pageable);

    Page<SourceClip> findByOwnerAndSourceType(User owner, SourceClip.SourceType sourceType, Pageable pageable);

    @Query("SELECT c FROM SourceClip c WHERE c.owner = :owner " +
           "AND (:keyword IS NULL OR c.title LIKE %:keyword% OR c.excerpt LIKE %:keyword% OR c.sourceTitle LIKE %:keyword%)")
    Page<SourceClip> searchByOwner(@Param("owner") User owner,
                                   @Param("keyword") String keyword,
                                   Pageable pageable);

    @Query("SELECT c FROM SourceClip c WHERE c.owner = :owner " +
           "AND (:keyword IS NULL OR c.title LIKE %:keyword% OR c.excerpt LIKE %:keyword% OR c.sourceTitle LIKE %:keyword%) " +
           "AND (:sourceType IS NULL OR c.sourceType = :sourceType)")
    Page<SourceClip> searchByOwnerAndType(@Param("owner") User owner,
                                          @Param("keyword") String keyword,
                                          @Param("sourceType") SourceClip.SourceType sourceType,
                                          Pageable pageable);

    @Query("SELECT c FROM SourceClip c JOIN c.tags t WHERE c.owner = :owner AND t.id IN :tagIds")
    List<SourceClip> findByOwnerAndTagIds(@Param("owner") User owner, @Param("tagIds") List<Long> tagIds);
}
