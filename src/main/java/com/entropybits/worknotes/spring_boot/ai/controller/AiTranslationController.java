/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.ai.controller;

import com.entropybits.worknotes.spring_boot.ai.dto.TranslationRequest;
import com.entropybits.worknotes.spring_boot.ai.service.NoteTranslationOrchestrator;
import com.entropybits.worknotes.spring_boot.dto.NoteResponse;
import com.entropybits.worknotes.spring_boot.entity.Note;
import com.entropybits.worknotes.spring_boot.exception.BadRequestException;
import com.entropybits.worknotes.spring_boot.exception.ResourceNotFoundException;
import com.entropybits.worknotes.spring_boot.exception.UnauthorizedException;
import com.entropybits.worknotes.spring_boot.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1/ai")
@RequiredArgsConstructor
public class AiTranslationController {

    private final NoteTranslationOrchestrator orchestrator;
    private final NoteRepository noteRepository;

    @PostMapping("/translate/note/{noteId}")
    public ResponseEntity<NoteResponse> translateNote(
            @PathVariable Long noteId,
            @RequestBody TranslationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Note source = noteRepository.findByIdWithOwner(noteId)
            .orElseThrow(() -> new ResourceNotFoundException("笔记不存在"));

        if (!source.getOwner().getUsername().equals(userDetails.getUsername())) {
            throw new UnauthorizedException("无权限操作此笔记");
        }

        log.info("Translating note {} for user={} mode={} lang={}",
            noteId, userDetails.getUsername(), request.getMode(), request.getTargetLanguage());

        try {
            Note translated = orchestrator.translate(source, request);
            return ResponseEntity.ok(NoteResponse.fromEntity(translated));
        } catch (BadRequestException | UnauthorizedException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Translation failed for note {}: {}", noteId, e.getMessage(), e);
            throw new BadRequestException("翻译失败：" + e.getMessage());
        }
    }

    @PostMapping("/linkedin/hashtags/{noteId}")
    public ResponseEntity<Map<String, List<String>>> generateLinkedInHashtags(
            @PathVariable Long noteId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Note note = noteRepository.findByIdWithOwner(noteId)
            .orElseThrow(() -> new ResourceNotFoundException("笔记不存在"));

        if (!note.getOwner().getUsername().equals(userDetails.getUsername())) {
            throw new UnauthorizedException("无权限操作此笔记");
        }

        log.info("Generating LinkedIn hashtags for note {} user={}", noteId, userDetails.getUsername());

        try {
            List<String> hashtags = orchestrator.generateLinkedInHashtags(note);
            return ResponseEntity.ok(Map.of("hashtags", hashtags));
        } catch (BadRequestException | UnauthorizedException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Hashtag generation failed for note {}: {}", noteId, e.getMessage(), e);
            throw new BadRequestException("标签生成失败：" + e.getMessage());
        }
    }
}
