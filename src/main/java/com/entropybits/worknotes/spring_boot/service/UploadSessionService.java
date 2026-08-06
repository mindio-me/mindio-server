/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.service;

import com.entropybits.worknotes.spring_boot.dto.FileResultVo;
import com.entropybits.worknotes.spring_boot.dto.UploadSessionResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UploadSessionService {
    UploadSessionResponse createSession(String model, Integer pid);

    UploadSessionResponse createSession(String model, Integer pid, String accept);

    UploadSessionResponse getSession(String sessionId, String token);

    UploadSessionResponse uploadToSession(String sessionId, String token, MultipartFile file, String model, Integer pid);

    void markFailed(String sessionId, String token, String error);

    void markCompleted(String sessionId, String token, FileResultVo result);
}


