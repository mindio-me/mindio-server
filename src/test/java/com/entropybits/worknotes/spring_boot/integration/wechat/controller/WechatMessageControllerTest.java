/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.wechat.controller;

import com.entropybits.worknotes.spring_boot.integration.wechat.WechatInboundConfig;
import com.entropybits.worknotes.spring_boot.integration.wechat.service.WechatInboundMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WechatMessageControllerTest {

    @Mock private WechatInboundMessageService messageService;

    private WechatInboundConfig config;
    private WechatMessageController controller;

    @BeforeEach
    void setUp() {
        config = new WechatInboundConfig();
        config.setAppId("app-id");
        config.setAppSecret("app-secret");
        config.setToken("my-token");
        controller = new WechatMessageController(config, messageService);
    }

    private String sha1(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String validSignature(String timestamp, String nonce) {
        String[] arr = {"my-token", timestamp, nonce};
        Arrays.sort(arr);
        return sha1(String.join("", arr));
    }

    @Test
    void verifyReturnsEchostrOnValidSignature() {
        String signature = validSignature("1700000000", "nonce1");

        var response = controller.verify(signature, "1700000000", "nonce1", "echo-value");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("echo-value");
    }

    @Test
    void verifyRejectsInvalidSignature() {
        var response = controller.verify("bad-signature", "1700000000", "nonce1", "echo-value");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void receiveDelegatesToMessageServiceOnValidSignature() {
        String signature = validSignature("1700000000", "nonce2");
        when(messageService.handleIncoming("<xml>body</xml>")).thenReturn("<xml>reply</xml>");

        var response = controller.receive(signature, "1700000000", "nonce2", "<xml>body</xml>");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("<xml>reply</xml>");
    }

    @Test
    void receiveRejectsInvalidSignature() {
        var response = controller.receive("bad-signature", "1700000000", "nonce2", "<xml>body</xml>");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
