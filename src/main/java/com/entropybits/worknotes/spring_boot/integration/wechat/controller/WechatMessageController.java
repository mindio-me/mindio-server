/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.wechat.controller;

import com.entropybits.worknotes.spring_boot.integration.wechat.WechatInboundConfig;
import com.entropybits.worknotes.spring_boot.integration.wechat.service.WechatInboundMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

@Slf4j
@RestController
@RequestMapping("/v1/integrations/wechat/callback")
@RequiredArgsConstructor
public class WechatMessageController {

    private final WechatInboundConfig inboundConfig;
    private final WechatInboundMessageService messageService;

    @GetMapping
    public ResponseEntity<String> verify(
            @RequestParam("signature") String signature,
            @RequestParam("timestamp") String timestamp,
            @RequestParam("nonce") String nonce,
            @RequestParam("echostr") String echostr) {
        if (!inboundConfig.isConfigured() || !verifySignature(signature, timestamp, nonce)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(echostr);
    }

    @PostMapping(produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> receive(
            @RequestParam("signature") String signature,
            @RequestParam("timestamp") String timestamp,
            @RequestParam("nonce") String nonce,
            @RequestBody String xmlBody) {
        if (!inboundConfig.isConfigured() || !verifySignature(signature, timestamp, nonce)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            String replyXml = messageService.handleIncoming(xmlBody);
            return ResponseEntity.ok(replyXml);
        } catch (Exception e) {
            log.error("Failed to handle WeChat callback: {}", e.getMessage(), e);
            return ResponseEntity.ok("success"); // 避免微信因异常反复重试
        }
    }

    private boolean verifySignature(String signature, String timestamp, String nonce) {
        String token = inboundConfig.getToken();
        if (token == null || token.isBlank()) return false;
        String[] arr = {token, timestamp, nonce};
        Arrays.sort(arr);
        String computed = sha1Hex(String.join("", arr));
        return computed.equalsIgnoreCase(signature);
    }

    private String sha1Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
