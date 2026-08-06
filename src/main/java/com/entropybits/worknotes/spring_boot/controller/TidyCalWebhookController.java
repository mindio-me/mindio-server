/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.controller;

import com.entropybits.worknotes.spring_boot.service.BookingService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * TidyCal / 自动化平台 Webhook 回调接口
 */
@RestController
@RequestMapping("/v1/integrations/tidycal")
@RequiredArgsConstructor
@Tag(name = "TidyCal Webhook", description = "接收来自 TidyCal / Zapier / Integrately 的预约回调")
@Slf4j
public class TidyCalWebhookController {

    private final BookingService bookingService;

    @Value("${tidycal.webhook-secret:}")
    private String webhookSecret;

    /**
     * 接收新预约事件
     *
     * 建议在 TidyCal / Zapier / Integrately 中配置：
     *  - Method: POST
     *  - URL: /api/v1/integrations/tidycal/bookings
     *  - Header: X-Tidycal-Secret: 与配置 tidycal.webhook-secret 一致
     */
    @PostMapping("/bookings")
    @Operation(summary = "接收新预约事件（Webhook）")
    public ResponseEntity<?> handleNewBooking(
            @RequestHeader(value = "X-Tidycal-Secret", required = false) String secret,
            @RequestBody JsonNode payload
    ) {
        if (webhookSecret != null && !webhookSecret.isBlank()) {
            if (secret == null || !webhookSecret.equals(secret)) {
                log.warn("收到未通过校验的 TidyCal Webhook 请求，secret 无效");
                return ResponseEntity.status(401).body(
                        java.util.Map.of("status", "unauthorized", "message", "Invalid webhook secret")
                );
            }
        }

        bookingService.handleNewBooking(payload);
        return ResponseEntity.ok(java.util.Map.of("status", "ok"));
    }
}

