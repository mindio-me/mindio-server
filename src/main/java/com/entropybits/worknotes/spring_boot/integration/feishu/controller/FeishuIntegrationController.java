/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.feishu.controller;

import com.entropybits.worknotes.spring_boot.integration.feishu.dto.*;
import com.entropybits.worknotes.spring_boot.integration.feishu.service.FeishuIntegrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/integrations/feishu")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class FeishuIntegrationController {

    private final FeishuIntegrationService service;
    private final com.entropybits.worknotes.spring_boot.repository.UserRepository userRepository;
    private final com.entropybits.worknotes.spring_boot.service.UserCredentialService userCredentialService;

    @Value("${worknotes.frontend.base-url:http://localhost:10822}")
    private String frontendBaseUrl;

    @GetMapping("/status")
    public ResponseEntity<FeishuBindStatusResponse> status(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(service.getBindStatus(userDetails.getUsername()));
    }

    // TODO: 飞书应用凭证管理接口已迁移到 /v1/settings/integrations/feishu/credentials。
    // 如需兼容旧前端，可在此保留一个简单代理到 SettingsController 的实现。

    @GetMapping("/oauth/url")
    public ResponseEntity<FeishuOAuthUrlResponse> oauthUrl(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(service.buildOAuthUrl(userDetails.getUsername()));
    }

    /**
     * OAuth callback is public (see SecurityConfig).
     * For MVP, we parse username from state as "username:uuid".
     */
    @GetMapping("/oauth/callback")
    public ResponseEntity<Void> callback(
            @RequestParam String code,
            @RequestParam(required = false) String state
    ) {
        String username = parseUsernameFromState(state);
        service.handleOAuthCallback(code, state, username);

        // redirect to frontend feishu import page (飞书导入页面)
        return ResponseEntity.status(302)
                .location(URI.create(frontendBaseUrl + "/workspace/feishu-import"))
                .build();
    }

    @PostMapping("/disconnect")
    public ResponseEntity<Void> disconnect(@AuthenticationPrincipal UserDetails userDetails) {
        service.disconnect(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/wiki/spaces")
    public ResponseEntity<List<FeishuWikiSpaceDto>> spaces(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(service.listSpaces(userDetails.getUsername()));
    }

    @GetMapping("/wiki/nodes")
    public ResponseEntity<List<FeishuWikiNodeDto>> nodes(
            @RequestParam String spaceId,
            @RequestParam(required = false) String pageToken,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String parentNodeToken,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(service.listNodes(userDetails.getUsername(), spaceId, pageToken, pageSize, parentNodeToken));
    }

    @PostMapping("/wiki/import")
    public ResponseEntity<FeishuWikiImportResponse> importSingle(
            @Valid @RequestBody FeishuWikiImportRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(service.importSinglePage(userDetails.getUsername(), request));
    }

    // ========== 云空间（我的文档资料库）相关端点 ==========

    /**
     * 列出云空间文件（我的文档资料库）
     *
     * @param folderToken 文件夹 token，null 表示根目录
     */
    @GetMapping("/drive/files")
    public ResponseEntity<List<FeishuDriveFileDto>> listDriveFiles(
            @RequestParam(required = false) String folderToken,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(service.listDriveFiles(userDetails.getUsername(), folderToken));
    }

    /**
     * 获取用户个人空间（我的文档库）根目录 folder token
     */
    @GetMapping("/drive/my-docs-root")
    public ResponseEntity<Map<String, String>> getMyDocsRoot(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String token = service.getMyDocsRootToken(userDetails.getUsername());
        return ResponseEntity.ok(Map.of("folderToken", token));
    }

    /**
     * 导入云空间单个文档
     */
    @PostMapping("/drive/import")
    public ResponseEntity<FeishuWikiImportResponse> importDriveDocument(
            @Valid @RequestBody FeishuDriveImportRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(service.importDriveDocument(userDetails.getUsername(), request));
    }

    private String parseUsernameFromState(String state) {
        if (state == null) return null;
        int idx = state.indexOf(':');
        if (idx <= 0) return null;
        return state.substring(0, idx);
    }
}


