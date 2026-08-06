/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.wechat.controller;

import com.entropybits.worknotes.spring_boot.integration.wechat.UploadResult;
import com.entropybits.worknotes.spring_boot.integration.wechat.WechatConfig;
import com.entropybits.worknotes.spring_boot.integration.wechat.dto.WechatPublishLogResponse;
import com.entropybits.worknotes.spring_boot.integration.wechat.dto.WechatPublishRequest;
import com.entropybits.worknotes.spring_boot.integration.wechat.service.WechatIntegrationService;
import com.entropybits.worknotes.spring_boot.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/integrations/wechat")
@RequiredArgsConstructor
public class WechatIntegrationController {

    private final WechatIntegrationService service;
    private final WechatConfig config;

    /** 检查配置是否就绪 */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> status() {
        return ResponseEntity.ok(Map.of("configured", config.isConfigured()));
    }

    /** 上传封面图到微信永久素材库 */
    @PostMapping("/upload-image")
    public ResponseEntity<Map<String, String>> uploadCoverImage(
            @RequestParam MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (!config.isConfigured()) {
            throw new BadRequestException("微信公众号未配置，请设置 WECHAT_APP_ID 和 WECHAT_APP_SECRET 环境变量");
        }
        if (file.isEmpty()) {
            throw new BadRequestException("文件不能为空");
        }
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png"))) {
            throw new BadRequestException("仅支持 jpg/png 格式");
        }
        try {
            UploadResult result = service.uploadCoverImage(file);
            return ResponseEntity.ok(Map.of("mediaId", result.mediaId(), "url", result.url()));
        } catch (Exception e) {
            throw new BadRequestException("封面图上传失败：" + e.getMessage());
        }
    }

    /** 推送为微信草稿 */
    @PostMapping("/draft/{noteId}")
    public ResponseEntity<WechatPublishLogResponse> pushDraft(
            @PathVariable Long noteId,
            @RequestBody WechatPublishRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (!config.isConfigured()) {
            throw new BadRequestException("微信公众号未配置，请设置 WECHAT_APP_ID 和 WECHAT_APP_SECRET 环境变量");
        }
        return ResponseEntity.ok(
            service.pushDraft(noteId, request, userDetails.getUsername()));
    }

    /** 推送草稿并直接群发 */
    @PostMapping("/publish/{noteId}")
    public ResponseEntity<WechatPublishLogResponse> publish(
            @PathVariable Long noteId,
            @RequestBody WechatPublishRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (!config.isConfigured()) {
            throw new BadRequestException("微信公众号未配置，请设置 WECHAT_APP_ID 和 WECHAT_APP_SECRET 环境变量");
        }
        return ResponseEntity.ok(
            service.publish(noteId, request, userDetails.getUsername()));
    }

    /** 获取某篇笔记的微信发布历史 */
    @GetMapping("/logs/{noteId}")
    public ResponseEntity<List<WechatPublishLogResponse>> getLogs(
            @PathVariable Long noteId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
            service.getLogs(noteId, userDetails.getUsername()));
    }
}
