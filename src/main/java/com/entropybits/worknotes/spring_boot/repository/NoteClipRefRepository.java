/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.repository;

import com.entropybits.worknotes.spring_boot.entity.Note;
import com.entropybits.worknotes.spring_boot.entity.NoteClipRef;
import com.entropybits.worknotes.spring_boot.entity.SourceClip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoteClipRefRepository extends JpaRepository<NoteClipRef, Long> {

    List<NoteClipRef> findByNoteOrderBySortOrderAsc(Note note);

    List<NoteClipRef> findByClip(SourceClip clip);

    Optional<NoteClipRef> findByNoteAndClip(Note note, SourceClip clip);

    boolean existsByNoteAndClip(Note note, SourceClip clip);

    void deleteByNoteAndClip(Note note, SourceClip clip);

    int countByNote(Note note);
}
