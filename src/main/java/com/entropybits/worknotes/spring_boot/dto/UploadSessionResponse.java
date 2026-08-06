/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UploadSessionResponse {
    private String sessionId;
    private String token;
    private String status; // PENDING, COMPLETED, FAILED, EXPIRED
    private LocalDateTime expiresAt;
    private String qrUrl;
    private FileResultVo result;
    private String error;
}


