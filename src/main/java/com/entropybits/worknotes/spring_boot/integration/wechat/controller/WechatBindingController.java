/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.wechat.controller;

import com.entropybits.worknotes.spring_boot.exception.BadRequestException;
import com.entropybits.worknotes.spring_boot.integration.wechat.WechatInboundConfig;
import com.entropybits.worknotes.spring_boot.integration.wechat.dto.WechatBindingResponse;
import com.entropybits.worknotes.spring_boot.integration.wechat.service.WechatBindingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/integrations/wechat/binding")
@RequiredArgsConstructor
public class WechatBindingController {

    private final WechatBindingService bindingService;
    private final WechatInboundConfig inboundConfig;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> status() {
        return ResponseEntity.ok(Map.of("configured", inboundConfig.isConfigured()));
    }

    @PostMapping("/generate-code")
    public ResponseEntity<Map<String, Object>> generateCode(@AuthenticationPrincipal UserDetails user) {
        if (!inboundConfig.isConfigured()) {
            throw new BadRequestException("微信消息接收未配置，请联系管理员设置 WECHAT_INBOUND_APP_ID 等环境变量");
        }
        String code = bindingService.generateBindCode(user.getUsername());
        return ResponseEntity.ok(Map.of("code", code, "expiresInSeconds", 600));
    }

    @GetMapping
    public ResponseEntity<List<WechatBindingResponse>> list(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(bindingService.listBindings(user.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> unbind(@PathVariable Long id, @AuthenticationPrincipal UserDetails user) {
        bindingService.unbind(id, user.getUsername());
        return ResponseEntity.noContent().build();
    }
}
