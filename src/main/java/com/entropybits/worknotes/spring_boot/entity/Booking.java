/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Booking 实体 - 第三方预约记录（如 TidyCal）
 */
@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 第三方预约 ID（如 TidyCal booking id）
     */
    @Column(name = "external_id", nullable = false, length = 191, unique = true)
    private String externalId;

    /**
     * 预约人姓名
     */
    @Column(length = 200)
    private String name;

    /**
     * 预约人邮箱
     */
    @Column(length = 200)
    private String email;

    /**
     * 开始时间（统一保存为 UTC）
     */
    @Column(name = "start_time")
    private LocalDateTime startTime;

    /**
     * 结束时间（可选）
     */
    @Column(name = "end_time")
    private LocalDateTime endTime;

    /**
     * 时区字符串（例如：Europe/London）
     */
    @Column(length = 100)
    private String timezone;

    /**
     * 事件类型 / slug（例如：30-minute-consultation）
     */
    @Column(name = "event_slug", length = 191)
    private String eventSlug;

    /**
     * 原始 payload（JSON 字符串，便于调试）
     */
    @Column(name = "raw_payload", length = Integer.MAX_VALUE)
    private String rawPayload;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

