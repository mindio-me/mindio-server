/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.service;

import java.time.LocalDateTime;

public record ParsedBookmark(String title, String rawUrl, LocalDateTime addedAt, String folderPath) {
}
