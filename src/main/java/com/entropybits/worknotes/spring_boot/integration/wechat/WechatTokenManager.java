/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.wechat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class WechatTokenManager {

    private static final String TOKEN_URL =
        "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s";

    private final RestTemplate restTemplate;

    private final Map<String, CachedToken> cache = new ConcurrentHashMap<>();

    private record CachedToken(String token, LocalDateTime expiresAt) {}

    public synchronized String getAccessToken(String appId, String appSecret) {
        CachedToken cached = cache.get(appId);
        if (cached != null && LocalDateTime.now().isBefore(cached.expiresAt())) {
            return cached.token();
        }
        CachedToken refreshed = refresh(appId, appSecret);
        cache.put(appId, refreshed);
        return refreshed.token();
    }

    @SuppressWarnings("unchecked")
    private CachedToken refresh(String appId, String appSecret) {
        String url = String.format(TOKEN_URL, appId, appSecret);
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        if (response == null) {
            throw new RuntimeException("WeChat token API returned null");
        }
        if (response.containsKey("errcode")) {
            throw new RuntimeException("WeChat token error: " + response.get("errmsg"));
        }
        String token = (String) response.get("access_token");
        int expiresIn = ((Number) response.get("expires_in")).intValue();
        log.info("WeChat access_token refreshed for appId={}, expires in {}s", appId, expiresIn);
        return new CachedToken(token, LocalDateTime.now().plusSeconds(expiresIn - 300));
    }
}
