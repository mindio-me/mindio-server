/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.wechat.service;

import com.entropybits.worknotes.spring_boot.entity.Note;
import com.entropybits.worknotes.spring_boot.entity.User;
import com.entropybits.worknotes.spring_boot.exception.BadRequestException;
import com.entropybits.worknotes.spring_boot.exception.ResourceNotFoundException;
import com.entropybits.worknotes.spring_boot.exception.UnauthorizedException;
import com.entropybits.worknotes.spring_boot.integration.wechat.*;
import com.entropybits.worknotes.spring_boot.integration.wechat.dto.WechatPublishLogResponse;
import com.entropybits.worknotes.spring_boot.integration.wechat.dto.WechatPublishRequest;
import com.entropybits.worknotes.spring_boot.integration.wechat.entity.WechatPublishLog;
import com.entropybits.worknotes.spring_boot.integration.wechat.repository.WechatPublishLogRepository;
import com.entropybits.worknotes.spring_boot.repository.NoteRepository;
import com.entropybits.worknotes.spring_boot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WechatIntegrationService {

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;
    private final WechatPublishLogRepository logRepository;
    private final WechatApiClient apiClient;
    private final EditorJsToHtmlConverter converter;
    private final WechatConfig config;

    /** 上传封面图到微信永久素材库，返回 mediaId 和 url */
    public UploadResult uploadCoverImage(MultipartFile file) throws Exception {
        byte[] bytes = file.getBytes();
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "cover.jpg";
        return apiClient.uploadPermanentImage(bytes, filename);
    }

    @Transactional
    public WechatPublishLogResponse pushDraft(Long noteId, WechatPublishRequest request, String username) {
        Note note = getAuthorizedNote(noteId, username);
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        try {
            // 1. 确定 thumbMediaId：前端传入 > 配置默认值
            String thumbMediaId = notBlank(request.getThumbMediaId())
                ? request.getThumbMediaId()
                : (notBlank(config.getDefaultThumbMediaId()) ? config.getDefaultThumbMediaId() : "");

            // 2. 前端传入本地图片 URL → 后端下载并上传微信获取 thumbMediaId
            String coverUploadError = null;
            if (thumbMediaId.isBlank() && notBlank(request.getCoverImageUrl())) {
                try {
                    String coverUrl = request.getCoverImageUrl();
                    log.info("Downloading cover image from {}", coverUrl);
                    byte[] bytes = downloadBytes(coverUrl);
                    String filename = coverUrl.substring(coverUrl.lastIndexOf('/') + 1);
                    if (filename.isBlank()) filename = "cover.jpg";
                    UploadResult r = apiClient.uploadPermanentImage(bytes, filename);
                    thumbMediaId = r.mediaId();
                    log.info("Cover image uploaded, thumbMediaId={}", thumbMediaId);
                } catch (Exception e) {
                    coverUploadError = e.getMessage();
                    log.warn("Failed to download/upload coverImageUrl [{}]: {}", request.getCoverImageUrl(), e.getMessage());
                }
            }

            // 3. 提取内容图片并上传（用于替换 HTML 中的本地链接；若 thumbMediaId 仍空则用第一张兜底）
            List<String> imageUrls = converter.extractImageUrls(note.getContent());
            Map<String, String> imageUrlMapping = new HashMap<>();

            for (int i = 0; i < imageUrls.size(); i++) {
                String localUrl = imageUrls.get(i);
                try {
                    byte[] bytes = downloadBytes(localUrl);
                    String filename = localUrl.substring(localUrl.lastIndexOf('/') + 1);
                    UploadResult result = apiClient.uploadPermanentImage(bytes, filename);
                    imageUrlMapping.put(localUrl, result.url());
                    if (thumbMediaId.isBlank() && i == 0) {
                        thumbMediaId = result.mediaId();
                    }
                } catch (Exception e) {
                    log.warn("Skipping image {}: {}", localUrl, e.getMessage());
                }
            }

            // 4. 最终检查
            if (thumbMediaId.isBlank()) {
                String msg = coverUploadError != null
                    ? "封面图上传到微信失败：" + coverUploadError + "。请检查微信配置或重试。"
                    : "缺少封面图片素材 ID（thumb_media_id）。请在对话框中上传封面图，或在笔记内容中包含至少一张图片。";
                throw new BadRequestException(msg);
            }

            // 5. 确定标题/作者/摘要
            String title = notBlank(request.getTitle()) ? request.getTitle() : note.getTitle();
            String author = request.getAuthor() != null ? request.getAuthor() : "";
            String digest = notBlank(request.getDigest()) ? request.getDigest()
                : converter.extractPlainText(note.getContent(), 120);

            // 6. 转换内容：EditorJS → HTML，再将外链替换为二维码图片
            String html = converter.convert(note.getContent(), imageUrlMapping);
            html = replaceLinksWithQrCodes(html);
            log.debug("WeChat article HTML (first 2000 chars): {}", html.length() > 2000 ? html.substring(0, 2000) : html);
            String mediaId = apiClient.createDraft(title, author, digest, html, thumbMediaId);

            // 7. 记录成功日志
            WechatPublishLog logEntry = WechatPublishLog.builder()
                .note(note).user(user)
                .wxTitle(title).wxAuthor(author)
                .mediaId(mediaId)
                .mode("DRAFT").status("SUCCESS")
                .build();
            logRepository.save(logEntry);
            return WechatPublishLogResponse.fromEntity(logEntry);

        } catch (Exception e) {
            log.error("WeChat push draft failed for note {}: {}", noteId, e.getMessage());
            WechatPublishLog failEntry = WechatPublishLog.builder()
                .note(note).user(user)
                .wxTitle(note.getTitle())
                .mode("DRAFT").status("FAILED")
                .errorMessage(e.getMessage())
                .build();
            logRepository.save(failEntry);
            throw new BadRequestException("发布失败：" + e.getMessage());
        }
    }

    @Transactional
    public WechatPublishLogResponse publish(Long noteId, WechatPublishRequest request, String username) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrowStart = LocalDate.now().plusDays(1).atStartOfDay();
        long todayCount = logRepository.countByModeAndStatusAndCreatedAtBetween(
            "PUBLISHED", "SUCCESS", todayStart, tomorrowStart);
        if (todayCount > 0) {
            throw new BadRequestException("今日已群发，每天限1次，请明天再试");
        }

        WechatPublishLogResponse draftResponse = pushDraft(noteId, request, username);

        try {
            String publishId = apiClient.submitPublish(draftResponse.getMediaId());
            WechatPublishLog logEntry = logRepository.findById(draftResponse.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Log not found"));
            logEntry.setPublishId(publishId);
            logEntry.setMode("PUBLISHED");
            logRepository.save(logEntry);
            return WechatPublishLogResponse.fromEntity(logEntry);
        } catch (Exception e) {
            log.error("WeChat submit publish failed: {}", e.getMessage());
            throw new BadRequestException("群发提交失败：" + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<WechatPublishLogResponse> getLogs(Long noteId, String username) {
        getAuthorizedNote(noteId, username);
        return logRepository.findByNoteIdOrderByCreatedAtDesc(noteId)
            .stream().map(WechatPublishLogResponse::fromEntity)
            .collect(Collectors.toList());
    }

    private Note getAuthorizedNote(Long noteId, String username) {
        Note note = noteRepository.findById(noteId)
            .orElseThrow(() -> new ResourceNotFoundException("笔记不存在"));
        if (!note.getOwner().getUsername().equals(username)) {
            throw new UnauthorizedException("无权限操作此笔记");
        }
        return note;
    }

    /** 用 Java 11 HttpClient 下载指定 URL 的字节内容 */
    private byte[] downloadBytes(String url) throws Exception {
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(15))
            .GET()
            .build();
        HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() >= 400) {
            throw new RuntimeException("HTTP " + resp.statusCode() + " downloading " + url);
        }
        return resp.body();
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    // ── 外链 → 二维码 ──────────────────────────────────────────────────────────

    private static final Pattern EXTERNAL_LINK_PATTERN = Pattern.compile(
        "<a\\s+[^>]*href\\s*=\\s*\"(https?://[^\"]+)\"[^>]*>(.*?)</a>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    /** 将 HTML 中所有外链 <a href="http..."> 替换为可扫描的二维码卡片 */
    private String replaceLinksWithQrCodes(String html) {
        Matcher matcher = EXTERNAL_LINK_PATTERN.matcher(html);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String url = matcher.group(1);
            String rawText = matcher.group(2).replaceAll("<[^>]+>", "").trim();
            String displayText = rawText.isEmpty() ? url : rawText;
            String replacement;
            try {
                byte[] qrBytes = generateQrCode(url);
                UploadResult result = apiClient.uploadPermanentImage(
                    qrBytes, "qr-" + System.currentTimeMillis() + ".png");
                replacement = buildQrBlock(displayText, result.url());
                log.info("Link converted to QR code: {}", url);
            } catch (Exception e) {
                log.warn("QR code generation/upload failed for {}: {}", url, e.getMessage());
                replacement = displayText;
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private byte[] generateQrCode(String url) throws Exception {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);
        BitMatrix matrix = new MultiFormatWriter()
            .encode(url, BarcodeFormat.QR_CODE, 300, 300, hints);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return out.toByteArray();
    }

    private String buildQrBlock(String linkText, String qrUrl) {
        return String.format(
            "<section style=\"margin:16px 0;padding:16px 12px;background:#f7f7f7;"
            + "border:1px solid #e8e8e8;border-radius:8px;text-align:center;\">"
            + "<p style=\"font-size:14px;color:#333;line-height:1.6;margin:0 0 12px;"
            + "font-weight:bold;\">%s</p>"
            + "<img src=\"%s\" style=\"width:180px;height:180px;display:block;"
            + "margin:0 auto;\" />"
            + "<p style=\"font-size:12px;color:#999;margin:8px 0 0;\">长按或扫描二维码访问</p>"
            + "</section>",
            linkText, qrUrl);
    }
}
