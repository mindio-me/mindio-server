/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.wechat.entity;

import com.entropybits.worknotes.spring_boot.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class WechatBindingTest {

    @Test
    void builderCreatesBindingWithExpectedFields() {
        User user = User.builder().id(1L).username("alice").build();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);

        WechatBinding binding = WechatBinding.builder()
                .bindCode("123456")
                .user(user)
                .status(WechatBinding.Status.PENDING)
                .codeExpiresAt(expiresAt)
                .build();

        assertThat(binding.getBindCode()).isEqualTo("123456");
        assertThat(binding.getUser().getUsername()).isEqualTo("alice");
        assertThat(binding.getStatus()).isEqualTo(WechatBinding.Status.PENDING);
        assertThat(binding.getOpenid()).isNull();
    }
}
