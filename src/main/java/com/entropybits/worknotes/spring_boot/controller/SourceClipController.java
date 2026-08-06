/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.controller;

import com.entropybits.worknotes.spring_boot.dto.ClipImportUrlRequest;
import com.entropybits.worknotes.spring_boot.dto.ClipTitleUpdateRequest;
import com.entropybits.worknotes.spring_boot.dto.NoteClipRefResponse;
import com.entropybits.worknotes.spring_boot.dto.SourceClipDraft;
import com.entropybits.worknotes.spring_boot.dto.SourceClipRequest;
import com.entropybits.worknotes.spring_boot.dto.SourceClipResponse;
import com.entropybits.worknotes.spring_boot.entity.SourceClip;
import com.entropybits.worknotes.spring_boot.service.ClipImportService;
import com.entropybits.worknotes.spring_boot.service.NoteClipRefService;
import com.entropybits.worknotes.spring_boot.service.SourceClipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/clips")
@RequiredArgsConstructor
public class SourceClipController {

    private final SourceClipService clipService;
    private final ClipImportService importService;
    private final NoteClipRefService refService;

    @PostMapping
    public ResponseEntity<SourceClipResponse> create(
            @Valid @RequestBody SourceClipRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(clipService.createClip(request, user.getUsername()));
    }

    @GetMapping
    public ResponseEntity<Page<SourceClipResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) SourceClip.SourceType sourceType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails user) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(clipService.listClips(user.getUsername(), keyword, sourceType, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SourceClipResponse> get(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(clipService.getClip(id, user.getUsername()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SourceClipResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SourceClipRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(clipService.updateClip(id, request, user.getUsername()));
    }

    @PatchMapping("/{id}/title")
    public ResponseEntity<SourceClipResponse> updateTitle(
            @PathVariable Long id,
            @Valid @RequestBody ClipTitleUpdateRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(clipService.updateTitle(id, request.getTitle()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        clipService.deleteClip(id, user.getUsername());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/notes")
    public ResponseEntity<java.util.List<NoteClipRefResponse>> getLinkedNotes(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(refService.getNotesForClip(id));
    }

    // --- import endpoints ---

    @PostMapping("/import/url")
    public ResponseEntity<SourceClipDraft> importFromUrl(
            @Valid @RequestBody ClipImportUrlRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(importService.fetchFromUrl(request));
    }
}
