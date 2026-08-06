/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.reddit.service;

import com.entropybits.worknotes.spring_boot.entity.Note;
import com.entropybits.worknotes.spring_boot.entity.User;
import com.entropybits.worknotes.spring_boot.exception.BadRequestException;
import com.entropybits.worknotes.spring_boot.exception.ResourceNotFoundException;
import com.entropybits.worknotes.spring_boot.exception.UnauthorizedException;
import com.entropybits.worknotes.spring_boot.integration.feishu.crypto.AesGcmStringEncryptor;
import com.entropybits.worknotes.spring_boot.integration.reddit.RedditProperties;
import com.entropybits.worknotes.spring_boot.integration.reddit.client.RedditApiClient;
import com.entropybits.worknotes.spring_boot.integration.reddit.client.model.RedditTokenResponse;
import com.entropybits.worknotes.spring_boot.integration.reddit.client.model.RedditUserInfoResponse;
import com.entropybits.worknotes.spring_boot.integration.reddit.dto.*;
import com.entropybits.worknotes.spring_boot.integration.reddit.entity.RedditPublishLog;
import com.entropybits.worknotes.spring_boot.integration.reddit.entity.RedditUserAuth;
import com.entropybits.worknotes.spring_boot.integration.reddit.repository.RedditPublishLogRepository;
import com.entropybits.worknotes.spring_boot.integration.reddit.repository.RedditUserAuthRepository;
import com.entropybits.worknotes.spring_boot.integration.reddit.util.EditorJsToMarkdownConverter;
import com.entropybits.worknotes.spring_boot.repository.NoteRepository;
import com.entropybits.worknotes.spring_boot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedditIntegrationService {

    private static final String OAUTH_SCOPE = "identity submit mysubreddits";
    private static final int    POST_TEXT_MAX = 39_000; // Reddit selftext limit is 40k; leave margin

    private final RedditProperties redditProperties;
    private final RedditApiClient redditApiClient;
    private final RedditUserAuthRepository authRepository;
    private final RedditPublishLogRepository publishLogRepository;
    private final UserRepository userRepository;
    private final NoteRepository noteRepository;
    private final EditorJsToMarkdownConverter markdownConverter;

    /** Lazy-loaded encryptor, created only on first use */
    private AesGcmStringEncryptor cachedEncryptor;

    private AesGcmStringEncryptor encryptor() {
        if (cachedEncryptor == null) {
            cachedEncryptor = new AesGcmStringEncryptor(redditProperties.getTokenCryptoKey());
        }
        return cachedEncryptor;
    }

    // ── Status ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public RedditStatusResponse getStatus(String username) {
        User user = getUser(username);
        boolean appConfigured = redditProperties.isConfigured();
        return authRepository.findByUser(user)
                .map(auth -> {
                    boolean expired = auth.getExpiresAt() != null
                            && auth.getExpiresAt().isBefore(LocalDateTime.now().plusSeconds(60));
                    return new RedditStatusResponse(appConfigured, true, auth.getRedditUsername(), expired);
                })
                .orElseGet(() -> new RedditStatusResponse(appConfigured, false, null, false));
    }

    // ── OAuth ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public RedditOAuthUrlResponse buildAuthorizeUrl(String username) {
        if (!redditProperties.isConfigured()) {
            throw new BadRequestException("Reddit 未配置（reddit.client-id / client-secret / oauth-redirect-uri）");
        }
        getUser(username);
        String state = generateOAuthState(username);
        String url = UriComponentsBuilder
                .fromUriString("https://www.reddit.com/api/v1/authorize")
                .queryParam("client_id",     redditProperties.getClientId())
                .queryParam("response_type", "code")
                .queryParam("state",         state)
                .queryParam("redirect_uri",  redditProperties.getOauthRedirectUri())
                .queryParam("duration",      "permanent")
                .queryParam("scope",         OAUTH_SCOPE)
                .encode()
                .build()
                .toUriString();
        return new RedditOAuthUrlResponse(url);
    }

    @Transactional
    public void handleOAuthCallback(String code, String state) {
        if (!redditProperties.isConfigured()) {
            throw new BadRequestException("Reddit 未配置");
        }
        if (code == null || code.isBlank()) {
            throw new BadRequestException("缺少 OAuth code");
        }
        String username = validateOAuthState(state);
        User user = getUser(username);

        RedditTokenResponse token = redditApiClient.exchangeAuthorizationCode(
                code, redditProperties.getOauthRedirectUri());

        RedditUserInfoResponse userInfo = redditApiClient.fetchUserInfo(token.getAccessToken());

        AesGcmStringEncryptor enc = encryptor();
        LocalDateTime expiresAt = token.getExpiresIn() != null
                ? LocalDateTime.now().plusSeconds(token.getExpiresIn()) : null;

        RedditUserAuth auth = authRepository.findByUser(user)
                .orElse(RedditUserAuth.builder().user(user).build());

        auth.setRedditUsername(userInfo.getName());
        auth.setAccessTokenEnc(enc.encrypt(token.getAccessToken()));
        if (token.getRefreshToken() != null && !token.getRefreshToken().isBlank()) {
            auth.setRefreshTokenEnc(enc.encrypt(token.getRefreshToken()));
        }
        auth.setExpiresAt(expiresAt);
        auth.setScope(token.getScope());
        authRepository.save(auth);
        log.info("Reddit OAuth connected for user={} redditUsername={}", username, userInfo.getName());
    }

    @Transactional
    public void disconnect(String username) {
        User user = getUser(username);
        authRepository.findByUser(user).ifPresent(auth -> {
            authRepository.delete(auth);
            log.info("Reddit auth disconnected for user={}", username);
        });
    }

    // ── Subreddits ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<RedditSubredditResponse> getSubscribedSubreddits(String username) {
        User user = getUser(username);
        RedditUserAuth auth = authRepository.findByUser(user)
                .orElseThrow(() -> new BadRequestException("请先连接 Reddit 账号"));
        String accessToken = requireFreshAccessToken(auth);
        return redditApiClient.fetchSubscribedSubreddits(accessToken);
    }

    // ── Publish ────────────────────────────────────────────────────────────────

    @Transactional
    public RedditPublishLogResponse publish(Long noteId, RedditPublishRequest request, String username) {
        if (!redditProperties.isConfigured()) {
            throw new BadRequestException("Reddit 未配置");
        }
        if (request.getSubreddit() == null || request.getSubreddit().isBlank()) {
            throw new BadRequestException("请指定目标 subreddit");
        }

        User user = getUser(username);
        RedditUserAuth auth = authRepository.findByUser(user)
                .orElseThrow(() -> new BadRequestException("请先连接 Reddit 账号"));

        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("笔记不存在"));
        if (!note.getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedException("无权限发布此笔记");
        }

        String accessToken = requireFreshAccessToken(auth);
        String subreddit   = request.getSubreddit().trim().replaceFirst("^r/", "");
        String postTitle   = (request.getTitle() != null && !request.getTitle().isBlank())
                ? request.getTitle().trim()
                : (note.getTitle() != null ? note.getTitle().trim() : "Untitled");
        if (postTitle.length() > 300) postTitle = postTitle.substring(0, 300);

        String postText = markdownConverter.convert(note.getContent(), POST_TEXT_MAX);
        log.info("Reddit publish: user={} subreddit=r/{} title={} textLen={}",
                username, subreddit, postTitle, postText.length());

        RedditPublishLog failureLog = RedditPublishLog.builder()
                .note(note).user(user)
                .subreddit(subreddit).postTitle(postTitle)
                .status(RedditPublishLog.Status.FAILED)
                .build();

        try {
            String postUrl = redditApiClient.submitSelfPost(accessToken, subreddit, postTitle, postText);
            log.info("Reddit publish success: postUrl={}", postUrl);
            RedditPublishLog ok = RedditPublishLog.builder()
                    .note(note).user(user)
                    .subreddit(subreddit).postTitle(postTitle)
                    .postUrl(postUrl)
                    .status(RedditPublishLog.Status.SUCCESS)
                    .build();
            return RedditPublishLogResponse.from(publishLogRepository.save(ok));
        } catch (BadRequestException e) {
            failureLog.setErrorMessage(e.getMessage());
            publishLogRepository.save(failureLog);
            throw e;
        } catch (Exception e) {
            log.error("Reddit publish failed", e);
            failureLog.setErrorMessage(e.getMessage());
            publishLogRepository.save(failureLog);
            throw new BadRequestException("发布失败: " + e.getMessage());
        }
    }

    // ── Logs ───────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<RedditPublishLogResponse> getLogs(Long noteId, String username) {
        User user = getUser(username);
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("笔记不存在"));
        if (!note.getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedException("无权限查看此笔记的发布记录");
        }
        return publishLogRepository.findByNote_IdOrderByCreatedAtDesc(noteId).stream()
                .map(RedditPublishLogResponse::from)
                .toList();
    }

    // ── Token helpers ──────────────────────────────────────────────────────────

    private String requireFreshAccessToken(RedditUserAuth auth) {
        AesGcmStringEncryptor enc = encryptor();
        boolean needsRefresh = auth.getExpiresAt() == null
                || auth.getExpiresAt().isBefore(LocalDateTime.now().plusSeconds(120));
        if (!needsRefresh) {
            return enc.decrypt(auth.getAccessTokenEnc());
        }
        if (auth.getRefreshTokenEnc() == null || auth.getRefreshTokenEnc().isBlank()) {
            throw new BadRequestException("Reddit 访问令牌已过期，请重新连接账号");
        }
        String refreshPlain = enc.decrypt(auth.getRefreshTokenEnc());
        RedditTokenResponse refreshed = redditApiClient.refreshAccessToken(refreshPlain);
        auth.setAccessTokenEnc(enc.encrypt(refreshed.getAccessToken()));
        if (refreshed.getRefreshToken() != null && !refreshed.getRefreshToken().isBlank()) {
            auth.setRefreshTokenEnc(enc.encrypt(refreshed.getRefreshToken()));
        }
        if (refreshed.getExpiresIn() != null) {
            auth.setExpiresAt(LocalDateTime.now().plusSeconds(refreshed.getExpiresIn()));
        }
        authRepository.save(auth);
        return refreshed.getAccessToken();
    }

    // ── OAuth state (HMAC-SHA256, same pattern as LinkedIn) ───────────────────

    private String generateOAuthState(String username) {
        String payload = username + ":" + UUID.randomUUID();
        return payload + ":" + hmacHex(payload);
    }

    private String validateOAuthState(String state) {
        if (state == null || state.isBlank()) {
            throw new BadRequestException("缺少 OAuth state 参数");
        }
        int lastColon = state.lastIndexOf(':');
        if (lastColon <= 0) {
            throw new BadRequestException("OAuth state 格式无效");
        }
        String payload      = state.substring(0, lastColon);
        String providedMac  = state.substring(lastColon + 1);
        String expectedMac  = hmacHex(payload);
        if (!MessageDigest.isEqual(
                providedMac.getBytes(StandardCharsets.UTF_8),
                expectedMac.getBytes(StandardCharsets.UTF_8))) {
            throw new BadRequestException("OAuth state 签名无效，请重新发起授权");
        }
        int firstColon = payload.indexOf(':');
        if (firstColon <= 0) {
            throw new BadRequestException("OAuth state 无法解析用户名");
        }
        return payload.substring(0, firstColon);
    }

    private String hmacHex(String data) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(redditProperties.getTokenCryptoKey());
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keyBytes, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC 计算失败", e);
        }
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
    }
}
