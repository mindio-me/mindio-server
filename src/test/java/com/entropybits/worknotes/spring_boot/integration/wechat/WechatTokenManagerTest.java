/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.wechat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WechatTokenManagerTest {

    @Mock
    private RestTemplate restTemplate;

    private WechatTokenManager tokenManager;

    @BeforeEach
    void setUp() {
        tokenManager = new WechatTokenManager(restTemplate);
    }

    @Test
    void returnsTokenOnSuccess() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
            .thenReturn(Map.of("access_token", "token-abc", "expires_in", 7200));

        assertThat(tokenManager.getAccessToken("app-1", "secret-1")).isEqualTo("token-abc");
    }

    @Test
    void throwsOnApiError() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
            .thenReturn(Map.of("errcode", 40013, "errmsg", "invalid appid"));

        assertThatThrownBy(() -> tokenManager.getAccessToken("app-1", "secret-1"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("invalid appid");
    }

    @Test
    void cachesPreviousTokenPerAppId() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
            .thenReturn(Map.of("access_token", "cached-token", "expires_in", 7200));

        tokenManager.getAccessToken("app-1", "secret-1");
        tokenManager.getAccessToken("app-1", "secret-1"); // 第二次应命中缓存

        verify(restTemplate, times(1)).getForObject(anyString(), eq(Map.class));
    }

    @Test
    void doesNotShareCacheBetweenDifferentAppIds() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
            .thenReturn(Map.of("access_token", "token-x", "expires_in", 7200));

        tokenManager.getAccessToken("app-1", "secret-1");
        tokenManager.getAccessToken("app-2", "secret-2"); // 不同 appId，应各自请求一次

        verify(restTemplate, times(2)).getForObject(anyString(), eq(Map.class));
    }
}
