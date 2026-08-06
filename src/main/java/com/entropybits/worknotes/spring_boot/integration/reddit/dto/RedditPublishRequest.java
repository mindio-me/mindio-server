/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.reddit.dto;

import lombok.Data;

@Data
public class RedditPublishRequest {
    /** Target subreddit name, without the r/ prefix. Required. */
    private String subreddit;
    /** Optional title override. Defaults to note title. */
    private String title;
}
