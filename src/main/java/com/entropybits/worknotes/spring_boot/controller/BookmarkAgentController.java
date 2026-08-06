/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.controller;

import com.entropybits.worknotes.spring_boot.dto.BookmarkAgentJobResponse;
import com.entropybits.worknotes.spring_boot.entity.BookmarkAgentJob;
import com.entropybits.worknotes.spring_boot.entity.User;
import com.entropybits.worknotes.spring_boot.exception.ResourceNotFoundException;
import com.entropybits.worknotes.spring_boot.repository.BookmarkAgentJobRepository;
import com.entropybits.worknotes.spring_boot.repository.UserRepository;
import com.entropybits.worknotes.spring_boot.service.BookmarkAgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/bookmark-agent")
@RequiredArgsConstructor
public class BookmarkAgentController {

    private final BookmarkAgentService agentService;
    private final BookmarkAgentJobRepository jobRepository;
    private final UserRepository userRepository;

    @PostMapping("/generate")
    public ResponseEntity<BookmarkAgentJobResponse> generate(
            @RequestParam BookmarkAgentJob.Type type,
            @AuthenticationPrincipal UserDetails principal) {
        User user = resolveUser(principal);
        BookmarkAgentJob job = agentService.generate(type, user);
        return ResponseEntity.ok(BookmarkAgentJobResponse.fromEntity(job));
    }

    @GetMapping("/jobs/current")
    public ResponseEntity<BookmarkAgentJobResponse> current(
            @RequestParam BookmarkAgentJob.Type type,
            @AuthenticationPrincipal UserDetails principal) {
        User user = resolveUser(principal);
        return jobRepository.findTopByOwnerAndTypeOrderByCreatedAtDesc(user, type)
                .map(job -> ResponseEntity.ok(BookmarkAgentJobResponse.fromEntity(job)))
                .orElseGet(() -> ResponseEntity.ok(BookmarkAgentJobResponse.empty(type)));
    }

    private User resolveUser(UserDetails principal) {
        return userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
    }
}
