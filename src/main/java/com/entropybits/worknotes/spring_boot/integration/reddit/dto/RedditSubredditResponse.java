/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.reddit.dto;

public record RedditSubredditResponse(
        String name,
        String title,
        long subscribers
) {}
