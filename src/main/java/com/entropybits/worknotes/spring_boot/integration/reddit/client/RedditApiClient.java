/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.reddit.client;

import com.entropybits.worknotes.spring_boot.exception.BadRequestException;
import com.entropybits.worknotes.spring_boot.integration.reddit.RedditProperties;
import com.entropybits.worknotes.spring_boot.integration.reddit.client.model.RedditTokenResponse;
import com.entropybits.worknotes.spring_boot.integration.reddit.client.model.RedditUserInfoResponse;
import com.entropybits.worknotes.spring_boot.integration.reddit.dto.RedditSubredditResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedditApiClient {

    private static final String TOKEN_URL     = "https://www.reddit.com/api/v1/access_token";
    private static final String OAUTH_BASE    = "https://oauth.reddit.com";
    private static final String USERINFO_URL  = OAUTH_BASE + "/api/v1/me";
    private static final String SUBREDDITS_URL = OAUTH_BASE + "/subreddits/mine/subscriber?limit=100";
    private static final String SUBMIT_URL    = OAUTH_BASE + "/api/submit";

    private final RedditProperties redditProperties;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public RedditTokenResponse exchangeAuthorizationCode(String code, String redirectUri) {
        return postTokenForm(Map.of(
                "grant_type",   "authorization_code",
                "code",         code,
                "redirect_uri", redirectUri
        ));
    }

    public RedditTokenResponse refreshAccessToken(String refreshToken) {
        return postTokenForm(Map.of(
                "grant_type",    "refresh_token",
                "refresh_token", refreshToken
        ));
    }

    public RedditUserInfoResponse fetchUserInfo(String accessToken) {
        HttpRequest req = buildBearerGet(USERINFO_URL, accessToken);
        try {
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() >= 400) {
                log.warn("Reddit /me HTTP {}: {}", resp.statusCode(), resp.body());
                throw new BadRequestException("获取 Reddit 用户信息失败: HTTP " + resp.statusCode());
            }
            RedditUserInfoResponse body = objectMapper.readValue(resp.body(), RedditUserInfoResponse.class);
            if (body.getName() == null || body.getName().isBlank()) {
                throw new BadRequestException("Reddit 用户信息缺少 name 字段");
            }
            return body;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("获取 Reddit 用户信息失败: " + e.getMessage());
        }
    }

    /**
     * Returns up to 100 subreddits the user subscribes to, sorted by name.
     */
    public List<RedditSubredditResponse> fetchSubscribedSubreddits(String accessToken) {
        HttpRequest req = buildBearerGet(SUBREDDITS_URL, accessToken);
        try {
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() >= 400) {
                log.warn("Reddit subreddits HTTP {}: {}", resp.statusCode(), resp.body());
                return List.of();
            }
            JsonNode root = objectMapper.readTree(resp.body());
            JsonNode children = root.path("data").path("children");
            List<RedditSubredditResponse> result = new ArrayList<>();
            if (children.isArray()) {
                for (JsonNode child : children) {
                    JsonNode data = child.path("data");
                    String name        = data.path("display_name").asText("");
                    String title       = data.path("title").asText("");
                    long   subscribers = data.path("subscribers").asLong(0);
                    if (!name.isBlank()) {
                        result.add(new RedditSubredditResponse(name, title, subscribers));
                    }
                }
            }
            result.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
            return result;
        } catch (Exception e) {
            log.warn("Reddit subreddits fetch failed: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Submits a self (text) post. Returns the post URL on success.
     */
    public String submitSelfPost(String accessToken, String subreddit, String title, String text) {
        Map<String, String> params = Map.of(
                "api_type", "json",
                "kind",     "self",
                "sr",       subreddit,
                "title",    title,
                "text",     text
        );
        String formBody = params.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                        + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(SUBMIT_URL))
                .header("Authorization", "Bearer " + accessToken)
                .header("User-Agent", redditProperties.getUserAgent())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(formBody, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            log.debug("Reddit submit HTTP {}: {}", resp.statusCode(), resp.body());

            if (resp.statusCode() >= 400) {
                String msg = switch (resp.statusCode()) {
                    case 401 -> "Reddit 访问令牌无效或已过期，请重新连接";
                    case 403 -> "Reddit 拒绝发布（subreddit 权限不足或被封禁）";
                    case 429 -> "Reddit 请求过于频繁，请稍后再试";
                    default  -> "Reddit 发布失败";
                };
                throw new BadRequestException(msg + ": HTTP " + resp.statusCode());
            }

            JsonNode root = objectMapper.readTree(resp.body());
            // Check for API-level errors returned with HTTP 200
            JsonNode errors = root.path("json").path("errors");
            if (errors.isArray() && !errors.isEmpty()) {
                String errMsg = errors.get(0).toString();
                throw new BadRequestException("Reddit 发布失败: " + errMsg);
            }

            String url = root.path("json").path("data").path("url").asText("");
            if (url.isBlank()) {
                throw new BadRequestException("Reddit 发布成功但未返回帖子 URL");
            }
            return url;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Reddit 发布失败: " + e.getMessage());
        }
    }

    // ── internals ──────────────────────────────────────────────────────────────

    /**
     * Reddit token endpoint uses HTTP Basic Auth (clientId:clientSecret).
     */
    private RedditTokenResponse postTokenForm(Map<String, String> params) {
        String formBody = params.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                        + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        String credentials = Base64.getEncoder().encodeToString(
                (redditProperties.getClientId() + ":" + redditProperties.getClientSecret())
                        .getBytes(StandardCharsets.UTF_8));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Authorization", "Basic " + credentials)
                .header("User-Agent", redditProperties.getUserAgent())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(formBody, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() >= 400) {
                log.warn("Reddit token HTTP {}: {}", resp.statusCode(), resp.body());
                throw new BadRequestException("Reddit OAuth 请求失败: HTTP " + resp.statusCode());
            }
            RedditTokenResponse body = objectMapper.readValue(resp.body(), RedditTokenResponse.class);
            if (body.getError() != null) {
                throw new BadRequestException("Reddit OAuth 错误: " + body.getError()
                        + (body.getErrorDescription() != null ? " — " + body.getErrorDescription() : ""));
            }
            if (body.getAccessToken() == null || body.getAccessToken().isBlank()) {
                throw new BadRequestException("Reddit 未返回 access_token");
            }
            return body;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Reddit OAuth 请求失败: " + e.getMessage());
        }
    }

    private HttpRequest buildBearerGet(String url, String accessToken) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .header("User-Agent", redditProperties.getUserAgent())
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
    }
}
