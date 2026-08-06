/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.service.impl;

import com.entropybits.worknotes.spring_boot.dto.FileResultVo;
import com.entropybits.worknotes.spring_boot.dto.UploadSessionResponse;
import com.entropybits.worknotes.spring_boot.exception.BadRequestException;
import com.entropybits.worknotes.spring_boot.service.UploadService;
import com.entropybits.worknotes.spring_boot.service.UploadSessionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UploadSessionServiceImpl implements UploadSessionService {

    private static class Session {
        String sessionId;
        String token;
        String model;
        Integer pid;
        String accept;
        String status; // PENDING, COMPLETED, FAILED, EXPIRED
        LocalDateTime expiresAt;
        FileResultVo result;
        String error;
    }

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final UploadService uploadService;

    @Value("${worknotes.frontend.base-url:http://localhost:10822}")
    private String frontendBaseUrl;

    public UploadSessionServiceImpl(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @Override
    public UploadSessionResponse createSession(String model, Integer pid) {
        return createSession(model, pid, null);
    }

    @Override
    public UploadSessionResponse createSession(String model, Integer pid, String accept) {
        Session s = new Session();
        s.sessionId = UUID.randomUUID().toString().replace("-", "");
        s.token = UUID.randomUUID().toString().replace("-", "");
        s.model = (model == null || model.isBlank()) ? "note" : model;
        s.pid = pid == null ? 0 : pid;
        s.accept = (accept == null || accept.isBlank()) ? "" : accept;
        s.status = "PENDING";
        s.expiresAt = LocalDateTime.now().plusMinutes(10);
        sessions.put(s.sessionId, s);

        return toResponse(s, buildQrUrl(s));
    }

    @Override
    public UploadSessionResponse getSession(String sessionId, String token) {
        Session s = sessions.get(sessionId);
        if (s == null) {
            throw new BadRequestException("会话不存在");
        }
        validate(s, token);
        expireIfNeeded(s);
        return toResponse(s, buildQrUrl(s));
    }

    @Override
    public UploadSessionResponse uploadToSession(String sessionId, String token, MultipartFile file, String model, Integer pid) {
        Session s = sessions.get(sessionId);
        if (s == null) {
            throw new BadRequestException("会话不存在");
        }
        validate(s, token);
        expireIfNeeded(s);
        if ("EXPIRED".equals(s.status)) {
            throw new BadRequestException("会话已过期");
        }
        if ("COMPLETED".equals(s.status)) {
            return getSession(sessionId, token);
        }

        String useModel = (model == null || model.isBlank()) ? s.model : model;
        Integer usePid = pid == null ? s.pid : pid;

        FileResultVo result;
        String contentType = file != null ? file.getContentType() : null;
        boolean isImage = contentType != null && contentType.startsWith("image/");
        if (isImage) {
            result = uploadService.imageUpload(file, useModel, usePid, null);
        } else {
            result = uploadService.fileUpload(file, useModel, usePid, null);
        }

        s.status = "COMPLETED";
        s.result = result;
        return toResponse(s, buildQrUrl(s));
    }

    @Override
    public void markFailed(String sessionId, String token, String error) {
        Session s = sessions.get(sessionId);
        if (s == null) return;
        validate(s, token);
        s.status = "FAILED";
        s.error = error;
    }

    @Override
    public void markCompleted(String sessionId, String token, FileResultVo result) {
        Session s = sessions.get(sessionId);
        if (s == null) return;
        validate(s, token);
        s.status = "COMPLETED";
        s.result = result;
    }

    private void validate(Session s, String token) {
        if (token == null || token.isBlank() || !token.equals(s.token)) {
            throw new BadRequestException("token无效");
        }
    }

    private void expireIfNeeded(Session s) {
        if (!"COMPLETED".equals(s.status) && LocalDateTime.now().isAfter(s.expiresAt)) {
            s.status = "EXPIRED";
        }
    }

    private String buildQrUrl(Session s) {
        StringBuilder url = new StringBuilder(
                frontendBaseUrl.replaceAll("/+$", "") +
                "/workspace/upload-session/" + s.sessionId +
                "?token=" + s.token +
                "&model=" + s.model +
                "&pid=" + s.pid
        );
        if (s.accept != null && !s.accept.isBlank()) {
            url.append("&accept=").append(URLEncoder.encode(s.accept, StandardCharsets.UTF_8));
        }
        return url.toString();
    }

    private UploadSessionResponse toResponse(Session s, String qrUrl) {
        return UploadSessionResponse.builder()
                .sessionId(s.sessionId)
                .token(s.token)
                .status(s.status)
                .expiresAt(s.expiresAt)
                .qrUrl(qrUrl)
                .result(s.result)
                .error(s.error)
                .build();
    }
}


