/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.service;

import com.entropybits.worknotes.spring_boot.dto.NoteClipLinkRequest;
import com.entropybits.worknotes.spring_boot.dto.NoteClipRefResponse;
import com.entropybits.worknotes.spring_boot.entity.Note;
import com.entropybits.worknotes.spring_boot.entity.NoteClipRef;
import com.entropybits.worknotes.spring_boot.entity.SourceClip;
import com.entropybits.worknotes.spring_boot.exception.BadRequestException;
import com.entropybits.worknotes.spring_boot.exception.ResourceNotFoundException;
import com.entropybits.worknotes.spring_boot.repository.NoteClipRefRepository;
import com.entropybits.worknotes.spring_boot.repository.NoteRepository;
import com.entropybits.worknotes.spring_boot.repository.SourceClipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NoteClipRefService {

    private final NoteClipRefRepository refRepository;
    private final NoteRepository noteRepository;
    private final SourceClipRepository clipRepository;

    @Transactional(readOnly = true)
    public List<NoteClipRefResponse> getClipsForNote(Long noteId) {
        Note note = findNote(noteId);
        return refRepository.findByNoteOrderBySortOrderAsc(note)
                .stream()
                .map(NoteClipRefResponse::fromEntity)
                .toList();
    }

    @Transactional
    public NoteClipRefResponse linkClipToNote(Long noteId, Long clipId, NoteClipLinkRequest req) {
        Note note = findNote(noteId);
        SourceClip clip = findClip(clipId);

        if (refRepository.existsByNoteAndClip(note, clip)) {
            throw new BadRequestException("该素材已关联到此笔记");
        }

        NoteClipRef ref = NoteClipRef.builder()
                .note(note)
                .clip(clip)
                .userNote(req != null ? req.getUserNote() : null)
                .sortOrder(req != null && req.getSortOrder() != null ? req.getSortOrder() : 0)
                .build();

        return NoteClipRefResponse.fromEntity(refRepository.save(ref));
    }

    @Transactional
    public void unlinkClipFromNote(Long noteId, Long clipId) {
        Note note = findNote(noteId);
        SourceClip clip = findClip(clipId);
        refRepository.deleteByNoteAndClip(note, clip);
    }

    @Transactional
    public NoteClipRefResponse updateRef(Long noteId, Long clipId, NoteClipLinkRequest req) {
        Note note = findNote(noteId);
        SourceClip clip = findClip(clipId);
        NoteClipRef ref = refRepository.findByNoteAndClip(note, clip)
                .orElseThrow(() -> new ResourceNotFoundException("关联不存在"));

        if (req.getUserNote() != null) ref.setUserNote(req.getUserNote());
        if (req.getSortOrder() != null) ref.setSortOrder(req.getSortOrder());

        return NoteClipRefResponse.fromEntity(refRepository.save(ref));
    }

    @Transactional(readOnly = true)
    public List<NoteClipRefResponse> getNotesForClip(Long clipId) {
        SourceClip clip = findClip(clipId);
        return refRepository.findByClip(clip)
                .stream()
                .map(NoteClipRefResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public int countClipsForNote(Long noteId) {
        Note note = findNote(noteId);
        return refRepository.countByNote(note);
    }

    private Note findNote(Long id) {
        return noteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("笔记不存在"));
    }

    private SourceClip findClip(Long id) {
        return clipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("素材不存在"));
    }
}
