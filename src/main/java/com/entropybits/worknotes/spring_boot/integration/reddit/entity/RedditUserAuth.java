/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.reddit.entity;

import com.entropybits.worknotes.spring_boot.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "reddit_user_auth",
        uniqueConstraints = @UniqueConstraint(columnNames = "user_id")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RedditUserAuth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Reddit username from /api/v1/me */
    @Column(nullable = false, length = 128)
    private String redditUsername;

    @Column(nullable = false, length = Integer.MAX_VALUE)
    private String accessTokenEnc;

    @Column(length = Integer.MAX_VALUE)
    private String refreshTokenEnc;

    private LocalDateTime expiresAt;

    @Column(length = 512)
    private String scope;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
