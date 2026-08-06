/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 用户第三方平台凭证（按用户存储）。
 * - provider: 平台名称（如 feishu）
 * - appId: 平台应用 App ID
 * - appSecretEnc: 加密存储的 App Secret
 */
@Entity
@Table(
        name = "user_credentials",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_credentials_uid_provider", columnNames = {"uid", "provider"})
        },
        indexes = {
                @Index(name = "idx_user_credentials_uid_provider", columnList = "uid,provider")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uid", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(name = "app_id", nullable = false, length = 200)
    private String appId;

    @Column(name = "app_secret", nullable = false, length = 2000)
    private String appSecretEnc;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}


