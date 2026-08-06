/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.feishu.service;

import com.entropybits.worknotes.spring_boot.dto.NoteRequest;
import com.entropybits.worknotes.spring_boot.entity.User;
import com.entropybits.worknotes.spring_boot.exception.BadRequestException;
import com.entropybits.worknotes.spring_boot.exception.ResourceNotFoundException;
import com.entropybits.worknotes.spring_boot.integration.feishu.FeishuProperties;
import com.entropybits.worknotes.spring_boot.integration.feishu.client.FeishuClient;
import com.entropybits.worknotes.spring_boot.integration.feishu.client.model.FeishuDocxBlocksResponse;
import com.entropybits.worknotes.spring_boot.integration.feishu.client.model.FeishuDocxContentResponse;
import com.entropybits.worknotes.spring_boot.integration.feishu.client.model.FeishuOAuthTokenResponse;
import com.entropybits.worknotes.spring_boot.integration.feishu.client.model.FeishuWikiNodeListResponse;
import com.entropybits.worknotes.spring_boot.integration.feishu.client.model.FeishuWikiSpaceListResponse;
import com.entropybits.worknotes.spring_boot.integration.feishu.converter.FeishuBlocksToMarkdownConverter;
import com.entropybits.worknotes.spring_boot.integration.feishu.crypto.AesGcmStringEncryptor;
import com.entropybits.worknotes.spring_boot.integration.feishu.dto.*;
import com.entropybits.worknotes.spring_boot.integration.feishu.entity.FeishuDocumentSnapshot;
import com.entropybits.worknotes.spring_boot.integration.feishu.entity.FeishuOAuthToken;
import com.entropybits.worknotes.spring_boot.integration.feishu.entity.FeishuWikiImportMapping;
import com.entropybits.worknotes.spring_boot.integration.feishu.repository.FeishuDocumentSnapshotRepository;
import com.entropybits.worknotes.spring_boot.integration.feishu.repository.FeishuOAuthTokenRepository;
import com.entropybits.worknotes.spring_boot.integration.feishu.repository.FeishuWikiImportMappingRepository;
import com.entropybits.worknotes.spring_boot.repository.UserRepository;
import com.entropybits.worknotes.spring_boot.service.UserCredentialService;
import com.entropybits.worknotes.spring_boot.repository.NoteRepository;
import com.entropybits.worknotes.spring_boot.service.NoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeishuIntegrationService {

    private final FeishuProperties props;
    private final FeishuClient client;
    private final FeishuBlocksToMarkdownConverter blocksConverter;
    private final FeishuImageDownloadService imageDownloadService;

    private final UserRepository userRepository;
    private final FeishuOAuthTokenRepository tokenRepository;
    private final FeishuWikiImportMappingRepository mappingRepository;
    private final FeishuDocumentSnapshotRepository snapshotRepository;

    private final NoteService noteService;
    private final NoteRepository noteRepository;
    private final UserCredentialService userCredentialService;

    private AesGcmStringEncryptor encryptor() {
        return new AesGcmStringEncryptor(props.getTokenCryptoKey());
    }

    @Transactional(readOnly = true)
    public FeishuBindStatusResponse getBindStatus(String username) {
        User user = getUser(username);
        boolean configured = userCredentialService.isConfigured(user, UserCredentialService.PROVIDER_FEISHU);
        final String appIdMasked = configured ? safeGetMaskedAppId(user) : null;
        return tokenRepository.findByUser(user)
                .map(t -> new FeishuBindStatusResponse(true, configured, appIdMasked, t.getTenantKey(), t.getFeishuUserId(), t.getFeishuOpenId()))
                .orElseGet(() -> new FeishuBindStatusResponse(false, configured, appIdMasked, null, null, null));
    }

    @Transactional(readOnly = true)
    public FeishuOAuthUrlResponse buildOAuthUrl(String username) {
        User user = getUser(username);
        var cred = userCredentialService.getRequired(user, UserCredentialService.PROVIDER_FEISHU);

        // state: bind to current user; keep it simple for MVP (UUID + username)
        // (You can later sign this state to prevent tampering.)
        String state = username + ":" + UUID.randomUUID();
        return new FeishuOAuthUrlResponse(client.buildAuthorizeUrl(cred.getAppId(), state));
    }

    @Transactional
    public void handleOAuthCallback(String code, String state, String usernameFromState) {
        log.debug("Running function handleOAuthCallback()");
        log.debug("Code: {}", code);
        log.debug("State: {}", state);
        log.debug("Username from state: {}", usernameFromState);

        if (code == null || code.isBlank()) {
            throw new BadRequestException("缺少 code");
        }
        if (usernameFromState == null || usernameFromState.isBlank()) {
            throw new BadRequestException("无效 state：无法解析用户名");
        }

        User user = getUser(usernameFromState);
        var cred = userCredentialService.getRequired(user, UserCredentialService.PROVIDER_FEISHU);
        String appSecret = userCredentialService.decryptAppSecret(cred);
        FeishuOAuthTokenResponse resp = client.exchangeOAuthCode(cred.getAppId(), appSecret, code);

        if (resp == null) {
            throw new BadRequestException("飞书返回为空");
        }
        // v2 接口：成功时不返回 code 字段，只有出错时才返回非零 code
        // v1 接口：成功时返回 code=0
        if (resp.getCode() != null && resp.getCode() != 0) {
            throw new BadRequestException("飞书 OAuth 失败: " + resp.getMsg());
        }
        if (resp.getData() == null || resp.getData().getAccessToken() == null) {
            throw new BadRequestException("飞书 OAuth 响应缺少 access_token");
        }

        AesGcmStringEncryptor enc = encryptor();
        LocalDateTime expiresAt = null;
        if (resp.getData().getExpiresIn() != null) {
            expiresAt = LocalDateTime.now().plusSeconds(resp.getData().getExpiresIn());
        }

        FeishuOAuthToken token = tokenRepository.findByUser(user)
                .orElse(FeishuOAuthToken.builder().user(user).build());

        log.debug("Setting tenant key: {}", resp.getData().getTenantKey());
        log.debug("Setting feishu user id: {}", resp.getData().getUserId());
        log.debug("Setting feishu open id: {}", resp.getData().getOpenId());
        log.debug("Setting access token: {}", resp.getData().getAccessToken());
        log.debug("Setting refresh token: {}", resp.getData().getRefreshToken());
        log.debug("Setting expires at: {}", expiresAt);
        if (resp.getData().getRefreshTokenExpiresIn() != null) {
            log.info("Refresh token expires in: {} seconds ({} days)", 
                    resp.getData().getRefreshTokenExpiresIn(),
                    resp.getData().getRefreshTokenExpiresIn() / 86400);
        } else {
            log.warn("Feishu did not return refresh_token_expires_in, cannot determine refresh token expiration");
        }

        token.setTenantKey(resp.getData().getTenantKey());
        token.setFeishuUserId(resp.getData().getUserId());
        token.setFeishuOpenId(resp.getData().getOpenId());
        token.setAccessTokenEnc(enc.encrypt(resp.getData().getAccessToken()));
        // 只有当飞书返回新的refresh_token时才更新，否则保留原有的
        if (resp.getData().getRefreshToken() != null && !resp.getData().getRefreshToken().isBlank()) {
            log.info("Updating refresh token for user: {}, old refresh token will be replaced", user.getUsername());
            token.setRefreshTokenEnc(enc.encrypt(resp.getData().getRefreshToken()));
            log.debug("New refresh token received from Feishu API, encrypted and saved");
        } else {
            log.debug("No new refresh token received from Feishu API, keeping existing refresh token");
        }
        token.setExpiresAt(expiresAt);
        tokenRepository.save(token);
    }

    @Transactional
    public void disconnect(String username) {
        User user = getUser(username);
        tokenRepository.findByUser(user).ifPresent(tokenRepository::delete);
    }

    // 注意：不能标 readOnly=true —— requireAccessToken() 可能在这里面触发 token 刷新并写库，
    // readOnly 事务会导致 Hibernate 把加载出的 FeishuOAuthToken 标记为只读，刷新写入会被静默丢弃，
    // 进而导致下次用同一个（已被飞书消费掉的）refresh_token 再刷新时报 invalid_grant。
    @Transactional
    public List<FeishuWikiSpaceDto> listSpaces(String username) {
        String accessToken = requireAccessToken(username);
        FeishuWikiSpaceListResponse resp = client.listWikiSpaces(accessToken);
        if (resp == null) throw new BadRequestException("飞书返回为空");
        if (resp.getCode() == null || resp.getCode() != 0) throw new BadRequestException("飞书接口失败: " + resp.getMsg());
        if (resp.getData() == null || resp.getData().getItems() == null) return List.of();
        return resp.getData().getItems().stream()
                .map(i -> new FeishuWikiSpaceDto(i.getSpaceId(), i.getName()))
                .toList();
    }

    // 同上：不能 readOnly，见 listSpaces() 的说明
    @Transactional
    public List<FeishuWikiNodeDto> listNodes(String username, String spaceId, String pageToken, Integer pageSize, String parentNodeToken) {
        if (spaceId == null || spaceId.isBlank()) throw new BadRequestException("spaceId 不能为空");
        String accessToken = requireAccessToken(username);
        FeishuWikiNodeListResponse resp = client.listWikiNodes(accessToken, spaceId, pageToken, pageSize, parentNodeToken);
        if (resp == null) throw new BadRequestException("飞书返回为空");
        if (resp.getCode() == null || resp.getCode() != 0) throw new BadRequestException("飞书接口失败: " + resp.getMsg());
        if (resp.getData() == null || resp.getData().getItems() == null) return List.of();
        return resp.getData().getItems().stream()
                .map(i -> new FeishuWikiNodeDto(
                        i.getNodeToken(),
                        i.getObjToken(),
                        i.getTitle(),
                        i.getObjType(),
                        Boolean.TRUE.equals(i.getHasChild()),
                        i.getParentNodeToken(),
                        i.getObjCreateTime(),
                        i.getObjEditTime()
                ))
                .toList();
    }

    // ========== 云空间（我的文档资料库）相关方法 ==========

    /**
     * 列出云空间文件（我的文档资料库）
     *
     * @param username    用户名
     * @param folderToken 文件夹 token，null 表示根目录
     * @return 文件列表
     */
    // 同上：不能 readOnly，见 listSpaces() 的说明
    @Transactional
    public List<FeishuDriveFileDto> listDriveFiles(String username, String folderToken) {
        String accessToken = requireAccessToken(username);
        var resp = client.listDriveFiles(accessToken, folderToken, null, 50);

        if (resp == null) throw new BadRequestException("飞书返回为空");
        if (resp.getCode() != null && resp.getCode() != 0) {
            throw new BadRequestException("飞书接口失败: " + resp.getMsg());
        }
        if (resp.getData() == null || resp.getData().getFiles() == null) {
            return List.of();
        }

        return resp.getData().getFiles().stream()
                .map(f -> new FeishuDriveFileDto(
                        f.getToken(),
                        f.getName(),
                        f.getType(),
                        f.getParentToken(),
                        "folder".equals(f.getType()),
                        isImportableType(f.getType()),
                        f.getUrl(),
                        f.getCreatedTime(),
                        f.getModifiedTime()
                ))
                .toList();
    }

    /**
     * 获取用户个人空间（我的文档库）根目录的 folder token
     *
     * @param username 用户名
     * @return 根目录 folder token
     */
    // 同上：不能 readOnly，见 listSpaces() 的说明
    @Transactional
    public String getMyDocsRootToken(String username) {
        String accessToken = requireAccessToken(username);
        var resp = client.getRootFolderMeta(accessToken);

        if (resp == null) throw new BadRequestException("飞书返回为空");
        if (resp.getCode() != null && resp.getCode() != 0) {
            throw new BadRequestException("飞书接口失败: " + resp.getMsg());
        }
        if (resp.getData() == null || resp.getData().getToken() == null) {
            throw new BadRequestException("无法获取我的文档库根目录 token");
        }

        return resp.getData().getToken();
    }

    /**
     * 判断文件类型是否可导入
     */
    private boolean isImportableType(String type) {
        if (type == null) return false;
        return "docx".equals(type) || "doc".equals(type);
    }

    /**
     * 将飞书 Unix 秒级时间戳字符串转换为 LocalDateTime（UTC）。
     * 对 null、空字符串、0 等无效值返回 null，由调用方的 @PrePersist 降级为当前时间。
     */
    private java.time.LocalDateTime fromFeishuTimestamp(String epochSeconds) {
        if (epochSeconds == null || epochSeconds.isBlank()) return null;
        try {
            long seconds = Long.parseLong(epochSeconds.trim());
            if (seconds <= 0) return null;
            return java.time.LocalDateTime.ofEpochSecond(seconds, 0, java.time.ZoneOffset.UTC);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 导入云空间单个文档
     *
     * @param username 用户名
     * @param request  导入请求
     * @return 导入结果
     */
    @Transactional
    public FeishuWikiImportResponse importDriveDocument(String username, FeishuDriveImportRequest request) {
        if (request.getFileToken() == null || request.getFileToken().isBlank()) {
            throw new BadRequestException("fileToken 不能为空");
        }

        log.info("Starting drive document import: username={}, fileToken={}, fileName={}",
                username, request.getFileToken(), request.getFileName());

        // 1. 获取访问令牌
        String accessToken = requireAccessToken(username);
        User user = getUser(username);

        // 2. 准备基本信息
        String title = (request.getFileName() == null || request.getFileName().isBlank())
                ? "Feishu Drive Import"
                : request.getFileName();

        String sourceUrl = request.getFileUrl();
        String documentId = request.getFileToken();

        log.info("Import target: documentId={}, title={}", documentId, title);

        // 3. 获取原始数据
        RawDataResult rawData;
        try {
            rawData = fetchBothRawAndBlocks(accessToken, documentId);
        } catch (Exception e) {
            log.error("Failed to fetch raw data: documentId={}", documentId, e);
            throw new BadRequestException("获取飞书文档数据失败: " + e.getMessage());
        }

        if (!rawData.hasData()) {
            log.error("No data fetched from Feishu: documentId={}", documentId);
            throw new BadRequestException("从飞书获取的文档内容为空");
        }

        // 4. 转换为 markdown
        String markdown = convertRawDataToMarkdown(rawData);
        log.info("Markdown conversion completed: {} chars", markdown.length());

        // 5. 创建 Note
        NoteRequest noteRequest = new NoteRequest();
        noteRequest.setTitle(title);
        noteRequest.setContentType("markdown");
        noteRequest.setContent(markdown);
        noteRequest.setIsPublic(false);
        noteRequest.setCreatedAt(fromFeishuTimestamp(request.getCreatedTime()));
        noteRequest.setModifiedAt(fromFeishuTimestamp(request.getModifiedTime()));

        var noteResp = noteService.createNote(noteRequest, username);
        log.info("Note created: noteId={}", noteResp.getId());

        // 6. 保存快照（简化版，不创建 mapping）
        try {
            String blocksJson = serializeBlocksToJson(rawData.getBlocks());
            int contentSize = calculateSize(rawData);

            FeishuDocumentSnapshot snapshot = FeishuDocumentSnapshot.builder()
                    .note(noteRepository.getReferenceById(noteResp.getId()))
                    .user(user)
                    .documentId(documentId)
                    .rawMarkdown(rawData.getRawMarkdown())
                    .rawMarkdownTruncated(rawData.isTruncated())
                    .blocksJson(blocksJson)
                    .blocksCount(rawData.getBlocksCount())
                    .blocksPages(rawData.getBlocksPages())
                    .convertedMarkdown(markdown)
                    .conversionStrategy(rawData.getStrategy())
                    .contentSizeBytes(contentSize)
                    .importStatus("success")
                    .syncDirection("feishu_to_local")
                    .build();

            snapshotRepository.save(snapshot);
            log.info("Snapshot saved: snapshotId={}, size={} bytes", snapshot.getId(), contentSize);

            // 7. 下载并替换图片
            try {
                log.info("Starting image download: documentId={}", documentId);
                String updatedMarkdown = imageDownloadService.downloadAndReplaceImages(
                        accessToken,
                        documentId,
                        rawData.getBlocks(),
                        markdown,
                        snapshot,
                        noteRepository.getReferenceById(noteResp.getId()),
                        user
                );

                if (!markdown.equals(updatedMarkdown)) {
                    var note = noteRepository.getReferenceById(noteResp.getId());
                    note.setContent(updatedMarkdown);
                    noteRepository.save(note);
                    snapshot.setConvertedMarkdown(updatedMarkdown);
                    snapshotRepository.save(snapshot);
                    log.info("Note and snapshot updated with image URLs");
                }
            } catch (Exception e) {
                log.error("Image download failed (not blocking import): documentId={}, error={}",
                        documentId, e.getMessage(), e);
            }

        } catch (Exception e) {
            log.error("Failed to save snapshot: documentId={}", documentId, e);
        }

        return new FeishuWikiImportResponse(noteResp.getId(), title);
    }

    @Transactional
    public FeishuWikiImportResponse importSinglePage(String username, FeishuWikiImportRequest request) {
        if (request.getSpaceId() == null || request.getSpaceId().isBlank()) throw new BadRequestException("spaceId 不能为空");
        if (request.getNodeToken() == null || request.getNodeToken().isBlank()) throw new BadRequestException("nodeToken 不能为空");

        log.info("Starting import: username={}, spaceId={}, nodeToken={}",
                username, request.getSpaceId(), request.getNodeToken());

        // 1. 获取访问令牌
        String accessToken = requireAccessToken(username);
        User user = getUser(username);

        // 2. 准备基本信息
        String title = (request.getNodeTitle() == null || request.getNodeTitle().isBlank())
                ? "Feishu Wiki Import"
                : request.getNodeTitle();

        String sourceUrl = "https://www.feishu.cn/wiki/" + request.getNodeToken();

        // 3. 确定 documentId（obj_token 优先，否则使用 node_token）
        String documentId = (request.getObjToken() != null && !request.getObjToken().isBlank())
                ? request.getObjToken()
                : request.getNodeToken();

        log.info("Import target: documentId={}, title={}", documentId, title);

        // 4. 【Phase 0 核心】获取原始数据（raw_content + blocks）
        RawDataResult rawData;
        try {
            rawData = fetchBothRawAndBlocks(accessToken, documentId);
        } catch (Exception e) {
            log.error("Failed to fetch raw data: documentId={}", documentId, e);
            throw new BadRequestException("获取飞书文档数据失败: " + e.getMessage());
        }

        if (!rawData.hasData()) {
            log.error("No data fetched from Feishu: documentId={}", documentId);
            throw new BadRequestException("从飞书获取的文档内容为空");
        }

        // 5. 转换为 markdown
        String markdown = convertRawDataToMarkdown(rawData);
        log.info("Markdown conversion completed: {} chars", markdown.length());

        // 6. 创建 Note
        NoteRequest noteRequest = new NoteRequest();
        noteRequest.setTitle(title);
        noteRequest.setContentType("markdown");
        noteRequest.setContent(markdown);
        noteRequest.setIsPublic(false);
        noteRequest.setCreatedAt(fromFeishuTimestamp(request.getObjCreateTime()));
        noteRequest.setModifiedAt(fromFeishuTimestamp(request.getObjEditTime()));

        var noteResp = noteService.createNote(noteRequest, username);
        log.info("Note created: noteId={}", noteResp.getId());

        // 7. 保存或更新 mapping
        String hash = sha256Hex(markdown);

        FeishuWikiImportMapping mapping = mappingRepository
                .findByUserAndSpaceIdAndNodeToken(user, request.getSpaceId(), request.getNodeToken())
                .orElse(FeishuWikiImportMapping.builder()
                        .user(user)
                        .spaceId(request.getSpaceId())
                        .nodeToken(request.getNodeToken())
                        .totalSnapshots(0)
                        .syncEnabled(false)
                        .build());

        mapping.setNodeTitle(title);
        mapping.setObjType(request.getObjType());
        mapping.setSourceUrl(sourceUrl);
        mapping.setContentHash(hash);
        mapping.setNote(noteRepository.getReferenceById(noteResp.getId()));
        mappingRepository.save(mapping);

        log.debug("Mapping saved: mappingId={}", mapping.getId());

        // 8. 【Phase 0 核心】保存原始数据快照
        try {
            String blocksJson = serializeBlocksToJson(rawData.getBlocks());
            int contentSize = calculateSize(rawData);

            FeishuDocumentSnapshot snapshot = FeishuDocumentSnapshot.builder()
                    .mapping(mapping)
                    .note(noteRepository.getReferenceById(noteResp.getId()))
                    .user(user)
                    .documentId(documentId)
                    .rawMarkdown(rawData.getRawMarkdown())
                    .rawMarkdownTruncated(rawData.isTruncated())
                    .blocksJson(blocksJson)
                    .blocksCount(rawData.getBlocksCount())
                    .blocksPages(rawData.getBlocksPages())
                    .convertedMarkdown(markdown)
                    .conversionStrategy(rawData.getStrategy())
                    .contentSizeBytes(contentSize)
                    .importStatus("success")
                    .syncDirection("feishu_to_local")
                    .build();

            snapshotRepository.save(snapshot);
            log.info("Snapshot saved: snapshotId={}, size={} bytes, strategy={}",
                    snapshot.getId(), contentSize, rawData.getStrategy());

            // 9. 更新 mapping 的快照引用
            mapping.setLatestSnapshot(snapshot);
            // 处理旧数据：total_snapshots 可能为 null
            Integer currentTotal = mapping.getTotalSnapshots();
            mapping.setTotalSnapshots((currentTotal != null ? currentTotal : 0) + 1);
            mappingRepository.save(mapping);

            log.info("Import completed successfully: noteId={}, snapshotId={}, totalSnapshots={}",
                    noteResp.getId(), snapshot.getId(), mapping.getTotalSnapshots());

            // 10. 【Phase 0.7】下载并替换图片
            try {
                log.info("Starting image download: documentId={}", documentId);
                String updatedMarkdown = imageDownloadService.downloadAndReplaceImages(
                        accessToken,
                        documentId,
                        rawData.getBlocks(),
                        markdown,
                        snapshot,
                        noteRepository.getReferenceById(noteResp.getId()),
                        user
                );

                // 如果markdown有更新（图片被替换），更新Note和Snapshot
                if (!markdown.equals(updatedMarkdown)) {
                    log.info("Images downloaded, updating note and snapshot: originalLength={}, newLength={}",
                            markdown.length(), updatedMarkdown.length());

                    // 更新Note内容
                    var note = noteRepository.getReferenceById(noteResp.getId());
                    note.setContent(updatedMarkdown);
                    noteRepository.save(note);

                    // 更新Snapshot的converted_markdown
                    snapshot.setConvertedMarkdown(updatedMarkdown);
                    snapshotRepository.save(snapshot);

                    log.info("Note and snapshot updated with image URLs");
                } else {
                    log.info("No images to download or all downloads failed");
                }

            } catch (Exception e) {
                log.error("Image download failed (not blocking import): documentId={}, error={}",
                        documentId, e.getMessage(), e);
                // 图片下载失败不影响导入结果
            }

        } catch (Exception e) {
            log.error("Failed to save snapshot (import succeeded but snapshot failed): documentId={}", documentId, e);
            // 快照保存失败不影响导入结果，仅记录日志
        }

        return new FeishuWikiImportResponse(noteResp.getId(), title);
    }

    /**
     * 将原始数据转换为 markdown
     *
     * @param rawData 原始数据
     * @return markdown 字符串
     */
    private String convertRawDataToMarkdown(RawDataResult rawData) {
        String strategy = rawData.getStrategy();

        switch (strategy) {
            case "raw_content":
                // 使用 raw_content API 的结果
                log.debug("Converting using raw_content strategy");
                return rawData.getRawMarkdown();

            case "blocks_api":
                // 使用 blocks API 的结果
                log.debug("Converting using blocks_api strategy");
                return blocksConverter.convert(rawData.getBlocks());

            case "hybrid":
                // 混合策略（当前未使用，预留）
                log.debug("Converting using hybrid strategy");
                return blocksConverter.convert(rawData.getBlocks());

            case "failed":
                // 都失败了
                log.error("Both raw_content and blocks_api failed");
                return buildStubBody("获取文档内容失败：两种API均未返回数据");

            default:
                log.warn("Unknown strategy: {}, falling back to blocks_api", strategy);
                return blocksConverter.convert(rawData.getBlocks());
        }
    }

    @Transactional
    private String requireAccessToken(String username) {
        log.info("Starting access token retrieval for user: {}", username);
        
        User user = getUser(username);
        FeishuOAuthToken token = tokenRepository.findByUser(user)
                .orElseThrow(() -> {
                    log.warn("No Feishu OAuth token found for user: {}", username);
                    return new BadRequestException("未绑定飞书账号");
                });

        log.debug("Found existing token for user: {}, tenantKey: {}, expiresAt: {}", 
                  username, token.getTenantKey(), token.getExpiresAt());

        // 如果即将过期或已过期，则尝试使用 refresh_token 刷新
        if (token.getExpiresAt() != null) {
            LocalDateTime now = LocalDateTime.now();
            // 提前 60 秒刷新，避免边界时间导致的 401
            if (!now.plusSeconds(60).isBefore(token.getExpiresAt())) {
                log.info("Token expiring soon or expired for user: {}, current time: {}, expires at: {}", 
                         username, now, token.getExpiresAt());
                
                // 需要刷新 - 使用synchronized防止并发刷新导致refresh_token被重复使用
                synchronized (this) {
                    log.debug("Acquired lock for token refresh for user: {}", username);
                    
                    // 重新查询token，可能已被其他线程刷新
                    token = tokenRepository.findByUser(user)
                            .orElseThrow(() -> {
                                log.error("Token disappeared during refresh for user: {}", username);
                                return new BadRequestException("未绑定飞书账号");
                            });

                    // 再次检查是否仍需刷新（其他线程可能已刷新）
                    LocalDateTime nowAgain = LocalDateTime.now();
                    if (token.getExpiresAt() != null && !nowAgain.plusSeconds(60).isBefore(token.getExpiresAt())) {
                        String refreshTokenEnc = token.getRefreshTokenEnc();
                        if (refreshTokenEnc == null || refreshTokenEnc.isBlank()) {
                            log.error("No refresh token available for user: {}", username);
                            throw new BadRequestException("飞书授权已过期，请在设置中重新绑定飞书账号");
                        }

                        log.info("Attempting to refresh access token for user: {}", username);
                        
                        AesGcmStringEncryptor enc = encryptor();
                        String refreshToken = enc.decrypt(refreshTokenEnc);
                        String accessTokenEnc = token.getAccessTokenEnc();
                        String accessToken = encryptor().decrypt(accessTokenEnc);
                        log.debug("Access token: {}", accessToken);
                        log.debug("Refresh token: {}", refreshToken);
                        log.debug("Successfully decrypted refresh token for user: {}, refreshToken={}", username, refreshToken);

                        var cred = userCredentialService.getRequired(user, UserCredentialService.PROVIDER_FEISHU);
                        String appSecret = userCredentialService.decryptAppSecret(cred);
                        log.debug("Retrieved app credentials for user: {}", username);

                        try {
                            // 记录 refresh token 的创建时间，用于诊断
                            log.debug("Token metadata for refresh: createdAt={}, updatedAt={}, expiresAt={}", 
                                     token.getCreatedAt(), token.getUpdatedAt(), token.getExpiresAt());
                            
                            FeishuOAuthTokenResponse resp = client.refreshOAuthToken(cred.getAppId(), appSecret, refreshToken);
                            
                            if (resp == null) {
                                log.error("Feishu API returned null response for token refresh, user: {}", username);
                                throw new BadRequestException("飞书返回为空");
                            }
                            
                            if (resp.getCode() == null || resp.getCode() != 0) {
                                Integer errorCode = resp.getCode();
                                String errorMsg = resp.getMsg();
                                
                                log.error("Feishu token refresh failed for user: {}, code: {}, message: {}, " +
                                         "tokenCreatedAt: {}, tokenUpdatedAt: {}, currentExpiresAt: {}", 
                                         username, errorCode, errorMsg, 
                                         token.getCreatedAt(), token.getUpdatedAt(), token.getExpiresAt());
                                
                                // 针对特定错误码提供更详细的错误信息
                                if (errorCode != null && errorCode == 20026) {
                                    // 20026: refresh token is invalid, it may has been used
                                    // 可能的原因：
                                    // 1. Refresh token 已过期（飞书的 refresh token 通常有30天或更长的有效期）
                                    // 2. Refresh token 已被使用（某些 OAuth 实现中，refresh token 只能使用一次）
                                    // 3. Refresh token 已被撤销
                                    // 4. 时间同步问题
                                    
                                    long daysSinceCreation = token.getCreatedAt() != null ? 
                                        java.time.Duration.between(token.getCreatedAt(), LocalDateTime.now()).toDays() : -1;
                                    long daysSinceUpdate = token.getUpdatedAt() != null ? 
                                        java.time.Duration.between(token.getUpdatedAt(), LocalDateTime.now()).toDays() : -1;
                                    long hoursSinceCreation = token.getCreatedAt() != null ? 
                                        java.time.Duration.between(token.getCreatedAt(), LocalDateTime.now()).toHours() : -1;
                                    long hoursSinceUpdate = token.getUpdatedAt() != null ? 
                                        java.time.Duration.between(token.getUpdatedAt(), LocalDateTime.now()).toHours() : -1;
                                    
                                    log.warn("Refresh token invalid (code 20026) for user: {}. " +
                                            "Possible causes: token expired, already used, or revoked. " +
                                            "Token age: {} days ({} hours) since creation, {} days ({} hours) since last update. " +
                                            "CreatedAt: {}, UpdatedAt: {}, ExpiresAt: {}",
                                            username,
                                            daysSinceCreation, hoursSinceCreation,
                                            daysSinceUpdate, hoursSinceUpdate,
                                            token.getCreatedAt(), token.getUpdatedAt(), token.getExpiresAt());
                                    
                                    // 如果 token 是最近创建的（24小时内），很可能是 refresh token 轮换问题
                                    String detailedMessage;
                                    if (hoursSinceCreation >= 0 && hoursSinceCreation < 24) {
                                        detailedMessage = "飞书授权已过期或失效（错误码: 20026）。" +
                                                "检测到 token 是最近创建的（" + hoursSinceCreation + " 小时前），" +
                                                "很可能是 refresh token 轮换问题：飞书的 refresh token 可能只能使用一次，" +
                                                "每次刷新后旧的 refresh token 会失效。如果之前有刷新操作但新的 refresh token 没有正确保存，" +
                                                "就会导致此错误。请前往设置页面重新绑定飞书账号。";
                                    } else {
                                        detailedMessage = "飞书授权已过期或失效（错误码: 20026）。" +
                                                "请前往设置页面重新绑定飞书账号。可能原因：刷新令牌已过期、已被使用或已被撤销。";
                                    }
                                    
                                    throw new BadRequestException(detailedMessage);
                                } else {
                                    throw new BadRequestException("飞书 OAuth 刷新失败（错误码: " + errorCode + "）: " + errorMsg);
                                }
                            }
                            
                            if (resp.getData() == null || resp.getData().getAccessToken() == null) {
                                log.error("Feishu token refresh response missing access_token for user: {}", username);
                                throw new BadRequestException("飞书 OAuth 刷新响应缺少 access_token");
                            }

                            log.info("Successfully refreshed token for user: {}, new expiration: {}", 
                                    username, resp.getData().getExpiresIn());

                            LocalDateTime expiresAt = null;
                            if (resp.getData().getExpiresIn() != null) {
                                expiresAt = LocalDateTime.now().plusSeconds(resp.getData().getExpiresIn());
                            }

                            token.setAccessTokenEnc(enc.encrypt(resp.getData().getAccessToken()));
                            
                            // 检查飞书是否返回了新的 refresh token
                            String newRefreshToken = resp.getData().getRefreshToken();
                            boolean hasNewRefreshToken = newRefreshToken != null && !newRefreshToken.isBlank();
                            
                            if (hasNewRefreshToken) {
                                log.info("Feishu returned new refresh token for user: {}, updating database", username);
                                token.setRefreshTokenEnc(enc.encrypt(newRefreshToken));
                                
                                // 记录 refresh token 的过期时间
                                if (resp.getData().getRefreshTokenExpiresIn() != null) {
                                    long refreshTokenExpiresInDays = resp.getData().getRefreshTokenExpiresIn() / 86400;
                                    log.info("New refresh token expires in: {} seconds ({} days) for user: {}", 
                                            resp.getData().getRefreshTokenExpiresIn(),
                                            refreshTokenExpiresInDays,
                                            username);
                                } else {
                                    log.warn("Feishu did not return refresh_token_expires_in for user: {}, cannot determine refresh token expiration", username);
                                }
                            } else {
                                // 警告：飞书没有返回新的 refresh token
                                // 这可能意味着：
                                // 1. 飞书的 refresh token 可以重复使用（不太可能）
                                // 2. 飞书的 refresh token 只能使用一次，但这次刷新没有返回新的（可能是 bug）
                                // 3. 旧的 refresh token 已经失效，下次刷新会失败
                                log.warn("Feishu did NOT return new refresh token for user: {}. " +
                                        "This may cause issues if Feishu's refresh tokens are single-use. " +
                                        "Old refresh token will be kept in database.", username);
                            }
                            
                            token.setExpiresAt(expiresAt);
                            tokenRepository.save(token);
                            
                            log.info("Token refresh completed and saved for user: {}, new expiresAt: {}, hasNewRefreshToken: {}", 
                                    username, expiresAt, hasNewRefreshToken);
                            
                        } catch (Exception e) {
                            log.error("Exception during token refresh for user: {}", username, e);
                            throw e;
                        }
                    } else {
                        log.debug("Token was already refreshed by another thread for user: {}", username);
                    }
                }
            } else {
                log.debug("Token is still valid for user: {}, expires at: {}", username, token.getExpiresAt());
            }
        }

        String accessToken = encryptor().decrypt(token.getAccessTokenEnc());
        log.debug("Successfully retrieved access token for user: {}", username);
        return accessToken;
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户", "username", username));
    }

    private String safeGetMaskedAppId(User user) {
        try {
            var cred = userCredentialService.getRequired(user, UserCredentialService.PROVIDER_FEISHU);
            return maskAppId(cred.getAppId());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String maskAppId(String appId) {
        if (appId == null) return null;
        String s = appId.trim();
        if (s.length() <= 8) return s;
        return s.substring(0, 4) + "****" + s.substring(s.length() - 4);
    }

    private static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * 智能获取 Docx 文档 markdown 内容
     * <p>
     * 采用"先快后慢"的双策略方案：
     * 1. 优先使用 raw_content API（快速路径）获取内容
     * 2. 检测内容完整性，如疑似截断则自动切换到 blocks API（完整路径）
     * 3. 如 raw_content 返回空，直接使用 blocks API
     * <p>
     * 该策略确保小文档快速导入，大文档完整导入。
     *
     * @param accessToken 访问令牌
     * @param request 导入请求
     * @return markdown 内容
     */
    private String fetchDocxMarkdownIfPossible(String accessToken, FeishuWikiImportRequest request) {
        // 1. 检查文档类型
        String objType = request.getObjType();
        boolean isDocx = objType != null && "docx".equalsIgnoreCase(objType);

        if (!isDocx) {
            log.warn("Unsupported document type: {}", objType);
            return buildStubBody("暂不支持的文档类型：" + (objType == null ? "UNKNOWN" : objType));
        }

        // 2. 确定 documentId
        String documentId = request.getObjToken() != null && !request.getObjToken().isBlank()
                ? request.getObjToken()
                : request.getNodeToken();

        log.info("Starting docx import: documentId={}, strategy=auto", documentId);

        try {
            // 3. 第一阶段：尝试 raw_content 快速获取
            String content = tryRawContentApi(accessToken, documentId);

            // 4. 第二阶段：检测完整性
            if (content != null && !content.isEmpty()) {
                if (isSuspectTruncation(content)) {
                    log.warn("Content may be truncated (size={} chars, {} bytes), falling back to blocks API",
                            content.length(), content.getBytes(StandardCharsets.UTF_8).length);
                    // Fallback 到 blocks API
                    content = fetchViaBlocksApi(accessToken, documentId);
                } else {
                    log.info("Content appears complete (size={} chars, {} bytes), using raw_content result",
                            content.length(), content.getBytes(StandardCharsets.UTF_8).length);
                }
                return content;
            }

            // 5. 如果 raw_content 返回空，直接使用 blocks API
            log.warn("Raw content is empty or null, using blocks API");
            return fetchViaBlocksApi(accessToken, documentId);

        } catch (BadRequestException e) {
            // 业务异常直接重新抛出（如blocks API调用失败）
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch docx content: documentId={}", documentId, e);
            return buildStubBody("拉取 Docx 内容异常：" + e.getMessage());
        }
    }

    /**
     * 尝试使用 raw_content API 获取内容
     * <p>
     * 该方法不抛出异常，失败时返回 null。
     *
     * @param accessToken 访问令牌
     * @param documentId 文档ID
     * @return 内容字符串，失败时返回 null
     */
    private String tryRawContentApi(String accessToken, String documentId) {
        try {
            log.debug("Trying raw_content API: documentId={}", documentId);
            FeishuDocxContentResponse resp = client.getDocxMarkdown(accessToken, documentId);

            if (resp == null) {
                log.warn("Raw content API returned null");
                return null;
            }

            if (resp.getCode() == null || resp.getCode() != 0) {
                log.warn("Raw content API failed: code={}, msg={}", resp.getCode(), resp.getMsg());
                return null;
            }

            if (resp.getData() == null || resp.getData().getContent() == null ||
                    resp.getData().getContent().isBlank()) {
                log.warn("Raw content API returned empty content");
                return null;
            }

            String content = resp.getData().getContent();
            log.debug("Raw content API succeeded: {} chars", content.length());
            return content;

        } catch (Exception e) {
            log.warn("Raw content API threw exception: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 检测内容是否疑似被截断
     * <p>
     * 使用启发式规则判断从 raw_content API 获取的内容是否可能不完整。
     * 这些规则基于以下假设：
     * 1. API 可能在某个固定大小边界截断内容（如 30KB, 50KB）
     * 2. 完整的 markdown 文档通常以换行符或标点符号结尾
     * 3. 截断的内容可能以省略号结尾
     *
     * @param content 要检测的内容
     * @return true 如果内容疑似被截断
     */
    private boolean isSuspectTruncation(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }

        int length = content.length();
        int byteLength = content.getBytes(StandardCharsets.UTF_8).length;

        // 启发式规则1: 内容接近某个整数KB边界
        // 飞书可能有 30KB、50KB 等未文档化的限制
        if (byteLength > 30000) {
            int remainder = byteLength % 1024;
            // 如果字节数距离 KB 边界很近（前后 100 字节内），可能被截断
            if (remainder < 100 || remainder > 924) {
                log.warn("Content size {} bytes is suspiciously close to KB boundary (remainder: {})",
                        byteLength, remainder);
                return true;
            }
        }

        // 启发式规则2: 内容不以完整句子结尾
        String trimmed = content.trim();
        if (!trimmed.isEmpty()) {
            char lastChar = trimmed.charAt(trimmed.length() - 1);
            // Markdown 通常以换行、句号、感叹号、问号等结尾
            // 也可能以代码块标记 ``` 或列表项结尾
            boolean endsWithProperTermination =
                    lastChar == '\n' ||
                    lastChar == '.' ||
                    lastChar == '!' ||
                    lastChar == '?' ||
                    lastChar == '。' ||  // 中文句号
                    lastChar == '！' ||  // 中文感叹号
                    lastChar == '？' ||  // 中文问号
                    lastChar == '`' ||   // 代码块
                    lastChar == '*' ||   // 列表或强调
                    lastChar == '-' ||   // 列表或分隔线
                    lastChar == '>' ||   // 引用块
                    lastChar == '#' ||   // 标题（某些情况）
                    lastChar == ')' ||   // 括号结束
                    lastChar == ']' ||   // 方括号结束
                    lastChar == '}';     // 花括号结束

            if (!endsWithProperTermination) {
                log.warn("Content ends abruptly without proper termination: lastChar='{}'", lastChar);
                return true;
            }
        }

        // 启发式规则3: 检查是否有明显的截断标志
        // 某些 API 会在截断时添加省略号或特殊标记
        if (trimmed.endsWith("...") || trimmed.endsWith("…")) {
            log.warn("Content ends with ellipsis, likely truncated");
            return true;
        }

        // 启发式规则4: 检查内容是否以不完整的 markdown 结构结尾
        // 例如：未闭合的代码块、表格等
        long backticksCount = trimmed.chars().filter(ch -> ch == '`').count();
        if (backticksCount % 2 != 0) {
            log.warn("Content has unmatched backticks ({}), may be truncated", backticksCount);
            // 注意：这个规则可能有误报，因为某些文档确实可能有奇数个反引号
            // 因此我们只在字节数也较大时才认为是截断
            if (byteLength > 20000) {
                return true;
            }
        }

        log.debug("Content appears complete: length={} chars, bytes={}", length, byteLength);
        return false;
    }

    /**
     * 使用 blocks API 获取完整文档内容（支持分页）
     * <p>
     * 当 raw_content API 返回的内容可能被截断时，使用此方法确保获取完整内容。
     * 该方法会循环获取所有页，直到 has_more 为 false。
     *
     * @param accessToken 访问令牌
     * @param documentId 文档ID
     * @return 完整的 markdown 内容
     */
    private String fetchViaBlocksApi(String accessToken, String documentId) {
        log.info("Fetching document via blocks API: documentId={}", documentId);
        long startTime = System.currentTimeMillis();

        List<FeishuDocxBlocksResponse.BlockItem> allBlocks = new ArrayList<>();
        String pageToken = null;
        int pageCount = 0;
        int maxPages = 100;  // 防止无限循环（100页 * 500块/页 = 最多5万块）

        try {
            do {
                pageCount++;
                if (pageCount > maxPages) {
                    log.warn("Reached max page limit ({}) for document: {}", maxPages, documentId);
                    break;
                }

                log.debug("Fetching blocks page {}: documentId={}, pageToken={}",
                        pageCount, documentId, pageToken);

                FeishuDocxBlocksResponse resp = client.getDocxBlocks(
                        accessToken, documentId, pageToken, 500);

                if (resp == null) {
                    log.error("Blocks API returned null response on page {}", pageCount);
                    throw new BadRequestException("飞书 Blocks API 返回为空");
                }

                if (resp.getCode() == null || resp.getCode() != 0) {
                    log.error("Blocks API error on page {}: code={}, msg={}",
                            pageCount, resp.getCode(), resp.getMsg());
                    throw new BadRequestException("飞书 Blocks API 失败: " + resp.getMsg());
                }

                if (resp.getData() != null && resp.getData().getItems() != null) {
                    int itemCount = resp.getData().getItems().size();
                    allBlocks.addAll(resp.getData().getItems());
                    log.debug("Page {} fetched: {} blocks added, total so far: {}",
                            pageCount, itemCount, allBlocks.size());

                    // 更新 pageToken 以获取下一页
                    pageToken = resp.getData().getPageToken();

                    // 检查是否还有更多
                    if (resp.getData().getHasMore() == null || !resp.getData().getHasMore()) {
                        log.debug("No more pages, stopping pagination");
                        break;
                    }
                } else {
                    log.debug("Page {} has no items, stopping pagination", pageCount);
                    break;
                }

            } while (pageToken != null && !pageToken.isBlank());

            log.info("Blocks API completed: totalBlocks={}, pages={}, time={}ms",
                    allBlocks.size(), pageCount, System.currentTimeMillis() - startTime);

            // 转换为 markdown
            String markdown = blocksConverter.convert(allBlocks);
            log.info("Markdown conversion completed: {} blocks -> {} chars",
                    allBlocks.size(), markdown.length());

            return markdown;

        } catch (BadRequestException e) {
            // 重新抛出业务异常
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error in blocks API: documentId={}", documentId, e);
            throw new BadRequestException("获取文档内容时发生异常：" + e.getMessage());
        }
    }

    private String buildStubBody(String reason) {
        return String.format(
                "*(未能拉取飞书文档正文 - %s)\n\n" +
                "**原因**：%s\n\n" +
                "**请检查**：\n" +
                "1. 文档权限是否正确\n" +
                "2. 文档类型是否支持（仅支持docx类型）\n" +
                "3. 飞书应用权限配置是否完整\n" +
                "4. 文档是否为空或已删除\n\n" +
                "**建议**：\n" +
                "- 尝试在飞书中打开该文档确认可访问\n" +
                "- 检查应用是否有 wiki:wiki:readonly 和 docx:document:readonly 权限\n" +
                "- 若问题持续，请联系管理员并提供以下时间：%s*\n",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                reason,
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
    }

    // ========== Phase 0: 原始数据获取与保存 ==========

    /**
     * 同时获取两种格式的原始数据
     * <p>
     * 该方法会：
     * 1. 尝试获取 raw_content API 的 markdown（可能被截断）
     * 2. 获取 blocks API 的完整数据（支持分页，确保完整）
     * 3. 决定最佳转换策略
     *
     * @param accessToken 访问令牌
     * @param documentId 文档ID
     * @return 原始数据结果（包含两种格式和元数据）
     */
    private RawDataResult fetchBothRawAndBlocks(String accessToken, String documentId) {
        log.info("Fetching both raw and blocks data: documentId={}", documentId);
        long startTime = System.currentTimeMillis();

        // 1. 获取 raw_content（可能被截断，但速度快）
        String rawMarkdown = tryRawContentApi(accessToken, documentId);
        boolean truncated = rawMarkdown != null && isSuspectTruncation(rawMarkdown);

        if (truncated) {
            log.warn("Raw markdown appears truncated: size={} bytes",
                    rawMarkdown != null ? rawMarkdown.getBytes(StandardCharsets.UTF_8).length : 0);
        }

        // 2. 获取 blocks（完整，支持分页）
        List<FeishuDocxBlocksResponse.BlockItem> blocks = fetchAllBlocks(accessToken, documentId);
        int blocksPages = blocks.isEmpty() ? 0 : (blocks.size() / 500) + 1; // 估算页数

        // 3. 检测是否包含图片块（block_type=27）
        boolean hasImageBlocks = blocks.stream()
                .anyMatch(block -> block.getBlockType() != null && block.getBlockType() == 27);

        if (hasImageBlocks) {
            log.info("Document contains {} image blocks, will use blocks_api for complete conversion",
                    blocks.stream().filter(b -> b.getBlockType() != null && b.getBlockType() == 27).count());
        }

        // 4. 决定转换策略
        // 注意：raw_content API 返回的是纯文本（无格式信息），不是 markdown。
        // 因此只要 blocks 数据可用，始终优先使用 blocks_api 以保留标题、加粗、列表、链接等格式。
        // raw_content 仅在 blocks 获取失败时作为兜底（退化为纯文本）。
        String strategy;
        if (!blocks.isEmpty()) {
            strategy = "blocks_api";
            log.info("Using blocks_api strategy (preserves formatting)");
        } else if (rawMarkdown != null && !rawMarkdown.isEmpty()) {
            // 兜底：blocks 获取失败，退而使用 raw_content 纯文本
            strategy = "raw_content";
            log.warn("Using raw_content strategy (fallback: blocks API returned empty, formatting will be lost)");
        } else {
            strategy = "failed";
            log.error("Both raw_content and blocks_api failed or returned empty");
        }

        RawDataResult result = RawDataResult.builder()
                .rawMarkdown(rawMarkdown)
                .truncated(truncated)
                .blocks(blocks)
                .blocksPages(blocksPages)
                .strategy(strategy)
                .build();

        log.info("Fetch completed: strategy={}, rawSize={} bytes, blocksCount={}, time={}ms",
                strategy,
                rawMarkdown != null ? rawMarkdown.getBytes(StandardCharsets.UTF_8).length : 0,
                blocks.size(),
                System.currentTimeMillis() - startTime);

        return result;
    }

    /**
     * 获取所有 blocks（处理分页）
     * <p>
     * 该方法会循环调用 blocks API 直到获取所有数据
     *
     * @param accessToken 访问令牌
     * @param documentId 文档ID
     * @return 所有 blocks 的列表
     */
    private List<FeishuDocxBlocksResponse.BlockItem> fetchAllBlocks(String accessToken, String documentId) {
        log.debug("Fetching all blocks: documentId={}", documentId);

        List<FeishuDocxBlocksResponse.BlockItem> allBlocks = new ArrayList<>();
        String pageToken = null;
        int pageCount = 0;
        int maxPages = 100;  // 防止无限循环

        try {
            do {
                pageCount++;
                if (pageCount > maxPages) {
                    log.warn("Reached max page limit ({}) for document: {}", maxPages, documentId);
                    break;
                }

                log.debug("Fetching blocks page {}: documentId={}, pageToken={}",
                        pageCount, documentId, pageToken);

                FeishuDocxBlocksResponse resp = client.getDocxBlocks(
                        accessToken, documentId, pageToken, 500);

                if (resp == null) {
                    log.error("Blocks API returned null response on page {}", pageCount);
                    break;
                }

                if (resp.getCode() == null || resp.getCode() != 0) {
                    log.error("Blocks API error on page {}: code={}, msg={}",
                            pageCount, resp.getCode(), resp.getMsg());
                    break;
                }

                if (resp.getData() != null && resp.getData().getItems() != null) {
                    int itemCount = resp.getData().getItems().size();
                    allBlocks.addAll(resp.getData().getItems());
                    log.debug("Page {} fetched: {} blocks added, total so far: {}",
                            pageCount, itemCount, allBlocks.size());

                    // 更新 pageToken 以获取下一页
                    pageToken = resp.getData().getPageToken();

                    // 检查是否还有更多
                    if (resp.getData().getHasMore() == null || !resp.getData().getHasMore()) {
                        log.debug("No more pages, stopping pagination");
                        break;
                    }
                } else {
                    log.debug("Page {} has no items, stopping pagination", pageCount);
                    break;
                }

            } while (pageToken != null && !pageToken.isBlank());

            log.info("All blocks fetched: totalBlocks={}, pages={}", allBlocks.size(), pageCount);
            return allBlocks;

        } catch (Exception e) {
            log.error("Error fetching blocks: documentId={}", documentId, e);
            return allBlocks; // 返回已获取的部分数据
        }
    }

    /**
     * 将 blocks 列表序列化为 JSON 字符串
     * <p>
     * 用于保存到数据库的 blocks_json 字段
     *
     * @param blocks blocks 列表
     * @return JSON 字符串
     */
    private String serializeBlocksToJson(List<FeishuDocxBlocksResponse.BlockItem> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return "[]";
        }

        try {
            // 使用 Jackson ObjectMapper 序列化
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(blocks);
        } catch (Exception e) {
            log.error("Failed to serialize blocks to JSON", e);
            return "[]";
        }
    }

    /**
     * 计算原始数据的总大小（字节）
     *
     * @param rawData 原始数据结果
     * @return 总字节数
     */
    private int calculateSize(RawDataResult rawData) {
        int size = 0;

        // raw_markdown 的大小
        if (rawData.getRawMarkdown() != null) {
            size += rawData.getRawMarkdown().getBytes(StandardCharsets.UTF_8).length;
        }

        // blocks 的大小（通过序列化后的 JSON 计算）
        if (rawData.getBlocks() != null && !rawData.getBlocks().isEmpty()) {
            String blocksJson = serializeBlocksToJson(rawData.getBlocks());
            size += blocksJson.getBytes(StandardCharsets.UTF_8).length;
        }

        return size;
    }
}


