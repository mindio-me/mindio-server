/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.wechat.service;

import com.entropybits.worknotes.spring_boot.dto.ClipImportUrlRequest;
import com.entropybits.worknotes.spring_boot.dto.SourceClipDraft;
import com.entropybits.worknotes.spring_boot.dto.SourceClipRequest;
import com.entropybits.worknotes.spring_boot.entity.SourceClip;
import com.entropybits.worknotes.spring_boot.entity.User;
import com.entropybits.worknotes.spring_boot.integration.wechat.WechatApiClient;
import com.entropybits.worknotes.spring_boot.integration.wechat.WechatInboundConfig;
import com.entropybits.worknotes.spring_boot.integration.wechat.WechatInboundMessage;
import com.entropybits.worknotes.spring_boot.integration.wechat.WechatPassiveReplyBuilder;
import com.entropybits.worknotes.spring_boot.integration.wechat.WechatTokenManager;
import com.entropybits.worknotes.spring_boot.integration.wechat.WechatXmlMessageParser;
import com.entropybits.worknotes.spring_boot.service.ClipImportService;
import com.entropybits.worknotes.spring_boot.service.SourceClipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class WechatInboundMessageService {

    private static final Pattern URL_PATTERN = Pattern.compile("^https?://\\S+$");
    private static final DateTimeFormatter TITLE_TIME_FORMAT = DateTimeFormatter.ofPattern("MM-dd HH:mm");
    private static final int DEDUP_CAPACITY = 200;
    private static final String PROCESSING_REPLY = "收到，正在处理…";

    private final WechatBindingService bindingService;
    private final ClipImportService clipImportService;
    private final SourceClipService sourceClipService;
    private final WechatApiClient apiClient;
    private final WechatTokenManager tokenManager;
    private final WechatInboundConfig inboundConfig;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    private final Set<String> recentMsgIds = Collections.newSetFromMap(
        Collections.synchronizedMap(new LinkedHashMap<>(DEDUP_CAPACITY, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                return size() > DEDUP_CAPACITY;
            }
        }));

    public String handleIncoming(String xmlBody) {
        WechatInboundMessage msg = WechatXmlMessageParser.parse(xmlBody);

        if (isDuplicate(msg.msgId())) {
            return WechatPassiveReplyBuilder.text(msg.fromUserName(), msg.toUserName(), PROCESSING_REPLY);
        }

        Optional<User> boundUser = bindingService.findBoundUser(msg.fromUserName());
        if (boundUser.isEmpty()) {
            if ("text".equals(msg.msgType())) {
                Optional<User> justBound = bindingService.tryConsumeBindCode(msg.fromUserName(), msg.content());
                if (justBound.isPresent()) {
                    return WechatPassiveReplyBuilder.text(msg.fromUserName(), msg.toUserName(),
                        "绑定成功！以后发链接/文字/图片过来就会自动存进 mindio");
                }
            }
            return WechatPassiveReplyBuilder.text(msg.fromUserName(), msg.toUserName(),
                "请先在 mindio 设置页生成绑定码");
        }

        User user = boundUser.get();
        String openid = msg.fromUserName();

        switch (msg.msgType()) {
            case "link":
                executor.submit(() -> saveLink(user, openid, msg.url()));
                return WechatPassiveReplyBuilder.text(openid, msg.toUserName(), PROCESSING_REPLY);
            case "text":
                if (isUrl(msg.content())) {
                    executor.submit(() -> saveLink(user, openid, msg.content().trim()));
                } else {
                    executor.submit(() -> saveText(user, openid, msg.content()));
                }
                return WechatPassiveReplyBuilder.text(openid, msg.toUserName(), PROCESSING_REPLY);
            case "image":
                executor.submit(() -> saveImage(user, openid, msg.picUrl()));
                return WechatPassiveReplyBuilder.text(openid, msg.toUserName(), PROCESSING_REPLY);
            default:
                return WechatPassiveReplyBuilder.text(openid, msg.toUserName(),
                    "暂不支持该类型，目前支持文字/链接/图片");
        }
    }

    private void saveLink(User user, String openid, String url) {
        try {
            ClipImportUrlRequest importRequest = new ClipImportUrlRequest();
            importRequest.setUrl(url);
            importRequest.setExtractionMode(SourceClip.ExtractionMode.FULL);
            SourceClipDraft draft = clipImportService.fetchFromUrl(importRequest);

            String title = notBlank(draft.getSuggestedTitle()) ? draft.getSuggestedTitle() : url;
            title = truncate(title, 200);

            SourceClipRequest saveRequest = new SourceClipRequest();
            saveRequest.setSourceType(draft.getSourceType());
            saveRequest.setSourceUrl(draft.getSourceUrl());
            saveRequest.setSourceTitle(draft.getSourceTitle());
            saveRequest.setSourceAuthor(draft.getSourceAuthor());
            saveRequest.setExtractionMode(draft.getExtractionMode());
            saveRequest.setExtractionStatus(draft.getExtractionStatus());
            saveRequest.setTitle(title);
            saveRequest.setContent(draft.getContent());
            saveRequest.setContentFormat(draft.getContentFormat());

            sourceClipService.createClip(saveRequest, user.getUsername());

            boolean fetchOk = draft.getExtractionStatus() == SourceClip.ExtractionStatus.SUCCESS;
            String confirmMsg = fetchOk ? "已保存《" + title + "》" : "抓取失败，已仅保存链接：《" + title + "》";
            sendConfirmation(openid, confirmMsg);
        } catch (Exception e) {
            log.error("Failed to save WeChat chat link {}: {}", url, e.getMessage());
            sendConfirmation(openid, "保存失败：" + e.getMessage());
        }
    }

    private void saveText(User user, String openid, String text) {
        try {
            String title = truncate(text.trim(), 30);
            if (title.isBlank()) {
                title = "微信速记 " + LocalDateTime.now().format(TITLE_TIME_FORMAT);
            }

            SourceClipRequest request = new SourceClipRequest();
            request.setSourceType(SourceClip.SourceType.WECHAT_CHAT_TEXT);
            request.setExtractionMode(SourceClip.ExtractionMode.FULL);
            request.setExtractionStatus(SourceClip.ExtractionStatus.SUCCESS);
            request.setTitle(title);
            request.setContent(text);
            request.setContentFormat("text");

            sourceClipService.createClip(request, user.getUsername());
            sendConfirmation(openid, "已保存《" + title + "》");
        } catch (Exception e) {
            log.error("Failed to save WeChat chat text: {}", e.getMessage());
            sendConfirmation(openid, "保存失败：" + e.getMessage());
        }
    }

    private void saveImage(User user, String openid, String picUrl) {
        try {
            String localPath = clipImportService.downloadAndSaveImage(picUrl, picUrl);
            String title = "微信图片 " + LocalDateTime.now().format(TITLE_TIME_FORMAT);

            SourceClipRequest request = new SourceClipRequest();
            request.setSourceType(SourceClip.SourceType.WECHAT_CHAT_IMAGE);
            request.setExtractionMode(SourceClip.ExtractionMode.FULL);
            request.setTitle(title);
            request.setContentFormat("html");

            if (localPath != null) {
                request.setExtractionStatus(SourceClip.ExtractionStatus.SUCCESS);
                request.setContent("<img src=\"" + localPath + "\"/>");
            } else {
                request.setExtractionStatus(SourceClip.ExtractionStatus.FAILED);
            }

            sourceClipService.createClip(request, user.getUsername());
            sendConfirmation(openid, localPath != null ? "已保存《" + title + "》" : "图片下载失败，请稍后重试");
        } catch (Exception e) {
            log.error("Failed to save WeChat chat image: {}", e.getMessage());
            sendConfirmation(openid, "保存失败：" + e.getMessage());
        }
    }

    private void sendConfirmation(String openid, String text) {
        try {
            String token = tokenManager.getAccessToken(inboundConfig.getAppId(), inboundConfig.getAppSecret());
            apiClient.sendCustomerServiceText(token, openid, text);
        } catch (Exception e) {
            log.warn("Failed to send WeChat confirmation to {}: {}", openid, e.getMessage());
        }
    }

    private boolean isDuplicate(String msgId) {
        if (msgId == null) return false;
        synchronized (recentMsgIds) {
            if (recentMsgIds.contains(msgId)) return true;
            recentMsgIds.add(msgId);
            return false;
        }
    }

    private boolean isUrl(String text) {
        return text != null && URL_PATTERN.matcher(text.trim()).matches();
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        String trimmed = s.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max) + "…";
    }
}
