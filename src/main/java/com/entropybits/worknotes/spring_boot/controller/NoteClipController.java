/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.controller;

import com.entropybits.worknotes.spring_boot.dto.NoteClipLinkRequest;
import com.entropybits.worknotes.spring_boot.dto.NoteClipRefResponse;
import com.entropybits.worknotes.spring_boot.service.NoteClipRefService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/notes/{noteId}/clips")
@RequiredArgsConstructor
public class NoteClipController {

    private final NoteClipRefService refService;

    @GetMapping
    public ResponseEntity<List<NoteClipRefResponse>> list(
            @PathVariable Long noteId,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(refService.getClipsForNote(noteId));
    }

    @PostMapping("/{clipId}")
    public ResponseEntity<NoteClipRefResponse> link(
            @PathVariable Long noteId,
            @PathVariable Long clipId,
            @RequestBody(required = false) NoteClipLinkRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(refService.linkClipToNote(noteId, clipId, request));
    }

    @DeleteMapping("/{clipId}")
    public ResponseEntity<Void> unlink(
            @PathVariable Long noteId,
            @PathVariable Long clipId,
            @AuthenticationPrincipal UserDetails user) {
        refService.unlinkClipFromNote(noteId, clipId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{clipId}")
    public ResponseEntity<NoteClipRefResponse> update(
            @PathVariable Long noteId,
            @PathVariable Long clipId,
            @RequestBody NoteClipLinkRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(refService.updateRef(noteId, clipId, request));
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Integer>> count(
            @PathVariable Long noteId,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(Map.of("count", refService.countClipsForNote(noteId)));
    }
}
