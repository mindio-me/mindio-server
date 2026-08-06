/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.controller;

import com.entropybits.worknotes.spring_boot.dto.UploadSessionResponse;
import com.entropybits.worknotes.spring_boot.service.UploadSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 扫码上传会话控制器
 *
 * 设计目标：桌面端创建会话 -> 展示二维码 -> 手机端打开二维码页面上传 -> 桌面端轮询获取结果
 * 安全：使用 sessionId + token（随机）进行会话校验（不要求JWT，便于手机端使用）
 */
@RestController
@RequestMapping("/v1/upload/sessions")
@Tag(name = "扫码上传会话", description = "扫码上传会话相关接口")
public class UploadSessionController {

    private final UploadSessionService uploadSessionService;

    public UploadSessionController(UploadSessionService uploadSessionService) {
        this.uploadSessionService = uploadSessionService;
    }

    @PostMapping
    @Operation(summary = "创建上传会话", description = "创建一个用于扫码上传的会话，并返回二维码URL")
    public ResponseEntity<UploadSessionResponse> create(
            @Parameter(description = "模块名称", example = "note") @RequestParam(defaultValue = "note") String model,
            @Parameter(description = "分类ID", example = "0") @RequestParam(defaultValue = "0") Integer pid,
            @Parameter(description = "文件类型过滤（传给手机端，如 audio/*）") @RequestParam(required = false, defaultValue = "") String accept
    ) {
        return ResponseEntity.ok(uploadSessionService.createSession(model, pid, accept));
    }

    @GetMapping("/{sessionId}")
    @Operation(summary = "查询上传会话", description = "查询会话状态与上传结果")
    public ResponseEntity<UploadSessionResponse> get(
            @PathVariable String sessionId,
            @RequestParam String token
    ) {
        return ResponseEntity.ok(uploadSessionService.getSession(sessionId, token));
    }

    @PostMapping("/{sessionId}/file")
    @Operation(summary = "会话上传文件", description = "手机端上传文件到该会话")
    public ResponseEntity<UploadSessionResponse> upload(
            @PathVariable String sessionId,
            @RequestParam String token,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "note") String model,
            @RequestParam(defaultValue = "0") Integer pid
    ) {
        return ResponseEntity.ok(uploadSessionService.uploadToSession(sessionId, token, file, model, pid));
    }
}


