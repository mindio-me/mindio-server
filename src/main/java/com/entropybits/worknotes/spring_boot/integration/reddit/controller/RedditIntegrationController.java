/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.reddit.controller;

import com.entropybits.worknotes.spring_boot.integration.reddit.dto.*;
import com.entropybits.worknotes.spring_boot.integration.reddit.service.RedditIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/integrations/reddit")
@RequiredArgsConstructor
public class RedditIntegrationController {

    private final RedditIntegrationService service;

    @Value("${worknotes.frontend.base-url:http://localhost:10822}")
    private String frontendBaseUrl;

    @GetMapping("/status")
    public ResponseEntity<RedditStatusResponse> status(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(service.getStatus(userDetails.getUsername()));
    }

    @GetMapping("/oauth/authorize-url")
    public ResponseEntity<RedditOAuthUrlResponse> authorizeUrl(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(service.buildAuthorizeUrl(userDetails.getUsername()));
    }

    /**
     * Browser redirect callback — no JWT. Username is parsed from the signed OAuth state.
     */
    @GetMapping("/oauth/callback")
    public ResponseEntity<Void> oauthCallback(
            @RequestParam String code,
            @RequestParam(required = false) String state) {
        try {
            service.handleOAuthCallback(code, state);
            return ResponseEntity.status(302)
                    .location(URI.create(frontendBaseUrl + "/workspace/notes?reddit_connected=1"))
                    .build();
        } catch (Exception e) {
            log.warn("Reddit OAuth callback failed: {}", e.getMessage());
            String errorParam = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            return ResponseEntity.status(302)
                    .location(URI.create(frontendBaseUrl + "/workspace/notes?reddit_error=" + errorParam))
                    .build();
        }
    }

    @PostMapping("/disconnect")
    public ResponseEntity<Void> disconnect(
            @AuthenticationPrincipal UserDetails userDetails) {
        service.disconnect(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/subreddits")
    public ResponseEntity<List<RedditSubredditResponse>> subreddits(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(service.getSubscribedSubreddits(userDetails.getUsername()));
    }

    @PostMapping("/publish/{noteId}")
    public ResponseEntity<RedditPublishLogResponse> publish(
            @PathVariable Long noteId,
            @RequestBody RedditPublishRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(service.publish(noteId, request, userDetails.getUsername()));
    }

    @GetMapping("/logs/{noteId}")
    public ResponseEntity<List<RedditPublishLogResponse>> logs(
            @PathVariable Long noteId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(service.getLogs(noteId, userDetails.getUsername()));
    }
}
