/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.reddit.dto;

import com.entropybits.worknotes.spring_boot.integration.reddit.entity.RedditPublishLog;

import java.time.LocalDateTime;

public record RedditPublishLogResponse(
        Long id,
        Long noteId,
        String subreddit,
        String postTitle,
        String postUrl,
        String status,
        String errorMessage,
        LocalDateTime createdAt
) {
    public static RedditPublishLogResponse from(RedditPublishLog log) {
        return new RedditPublishLogResponse(
                log.getId(),
                log.getNote().getId(),
                log.getSubreddit(),
                log.getPostTitle(),
                log.getPostUrl(),
                log.getStatus().name(),
                log.getErrorMessage(),
                log.getCreatedAt()
        );
    }
}
