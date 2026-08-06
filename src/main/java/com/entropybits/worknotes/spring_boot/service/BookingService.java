/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.service;

import com.entropybits.worknotes.spring_boot.entity.Booking;
import com.entropybits.worknotes.spring_boot.repository.BookingRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Booking 业务逻辑
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ObjectMapper objectMapper;

    /**
     * 处理来自 TidyCal / 自动化平台的新预约事件
     *
     * @param payload 原始 JSON payload
     */
    @Transactional
    public void handleNewBooking(JsonNode payload) {
        if (payload == null) {
            log.warn("收到空的预约 payload，忽略");
            return;
        }

        // 尝试从常见字段中提取信息（字段名根据 TidyCal / Zapier 的结构调整）
        String externalIdCandidate = textValue(payload, "id");
        if (externalIdCandidate == null || externalIdCandidate.isBlank()) {
            // 有些平台可能使用 booking_id / uuid 等字段
            externalIdCandidate = textValue(payload, "booking_id");
        }

        if (externalIdCandidate == null || externalIdCandidate.isBlank()) {
            log.warn("预约 payload 中缺少 externalId 字段，将仍然保存一条记录但 externalId 为占位符");
            externalIdCandidate = "unknown-" + System.currentTimeMillis();
        }

        final String externalId = externalIdCandidate;

        String name = textValue(payload, "name");
        String email = textValue(payload, "email");
        String timezone = textValue(payload, "timezone");
        String eventSlug = textValue(payload, "event_slug");

        // 解析开始/结束时间（假设为 ISO-8601 字符串）
        var startTime = parseDateTime(payload, "start_time");
        var endTime = parseDateTime(payload, "end_time");

        String rawJson;
        try {
            rawJson = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("序列化预约 payload 失败，将使用 toString()", e);
            rawJson = payload.toString();
        }

        Booking booking = bookingRepository.findByExternalId(externalId)
                .orElseGet(() -> Booking.builder().externalId(externalId).build());

        booking.setName(name);
        booking.setEmail(email);
        booking.setTimezone(timezone);
        booking.setEventSlug(eventSlug);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setRawPayload(rawJson);

        bookingRepository.save(booking);

        log.info("保存新的预约记录 externalId={}, name={}, email={}, startTime={}",
                externalId, name, email, startTime);

        // TODO: 这里可以扩展业务逻辑，例如：
        // - 发送站长通知邮件
        // - 根据 email 关联到 ContactSubmission / User 等
    }

    private String textValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && !value.isNull() ? value.asText() : null;
    }

    private java.time.LocalDateTime parseDateTime(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        try {
            // 优先按 OffsetDateTime 解析（带时区）
            OffsetDateTime odt = OffsetDateTime.parse(value.asText());
            return odt.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        } catch (Exception e) {
            log.warn("解析预约时间字段 {} 失败，原始值={}", field, value.asText(), e);
            return null;
        }
    }
}

