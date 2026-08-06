/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.wechat.controller;

import com.entropybits.worknotes.spring_boot.exception.BadRequestException;
import com.entropybits.worknotes.spring_boot.integration.wechat.WechatInboundConfig;
import com.entropybits.worknotes.spring_boot.integration.wechat.dto.WechatBindingResponse;
import com.entropybits.worknotes.spring_boot.integration.wechat.service.WechatBindingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WechatBindingControllerTest {

    @Mock private WechatBindingService bindingService;

    private WechatInboundConfig config;
    private WechatBindingController controller;
    private final UserDetails alice = new User("alice", "pwd", List.of());

    @BeforeEach
    void setUp() {
        config = new WechatInboundConfig();
        controller = new WechatBindingController(bindingService, config);
    }

    @Test
    void statusReflectsConfiguredFlag() {
        config.setAppId("app-id");
        config.setAppSecret("app-secret");

        assertThat(controller.status().getBody()).containsEntry("configured", true);
    }

    @Test
    void generateCodeRejectedWhenNotConfigured() {
        assertThatThrownBy(() -> controller.generateCode(alice))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void generateCodeDelegatesToService() {
        config.setAppId("app-id");
        config.setAppSecret("app-secret");
        when(bindingService.generateBindCode("alice")).thenReturn("123456");

        var response = controller.generateCode(alice);

        assertThat(response.getBody()).containsEntry("code", "123456");
    }

    @Test
    void listDelegatesToService() {
        WechatBindingResponse item = WechatBindingResponse.builder().id(1L).status("BOUND").build();
        when(bindingService.listBindings("alice")).thenReturn(List.of(item));

        var response = controller.list(alice);

        assertThat(response.getBody()).containsExactly(item);
    }

    @Test
    void unbindDelegatesToService() {
        controller.unbind(5L, alice);

        verify(bindingService).unbind(5L, "alice");
    }
}
