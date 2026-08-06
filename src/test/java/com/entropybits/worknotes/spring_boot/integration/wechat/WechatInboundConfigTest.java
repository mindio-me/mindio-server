/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.wechat;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WechatInboundConfigTest {

    @Test
    void isConfiguredTrueWhenAppIdAndSecretPresent() {
        WechatInboundConfig config = new WechatInboundConfig();
        config.setAppId("app-id");
        config.setAppSecret("app-secret");
        config.setToken("token");

        assertThat(config.isConfigured()).isTrue();
    }

    @Test
    void isConfiguredFalseWhenBlank() {
        WechatInboundConfig config = new WechatInboundConfig();
        assertThat(config.isConfigured()).isFalse();

        config.setAppId("app-id");
        assertThat(config.isConfigured()).isFalse();
    }
}
