/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.reddit.dto;

public record RedditStatusResponse(
        boolean appConfigured,
        boolean connected,
        String redditUsername,
        boolean tokenExpired
) {}
