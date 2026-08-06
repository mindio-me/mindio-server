/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.wechat.service;

import com.entropybits.worknotes.spring_boot.dto.ClipImportUrlRequest;
import com.entropybits.worknotes.spring_boot.dto.SourceClipDraft;
import com.entropybits.worknotes.spring_boot.dto.SourceClipRequest;
import com.entropybits.worknotes.spring_boot.dto.SourceClipResponse;
import com.entropybits.worknotes.spring_boot.entity.SourceClip;
import com.entropybits.worknotes.spring_boot.entity.User;
import com.entropybits.worknotes.spring_boot.integration.wechat.WechatApiClient;
import com.entropybits.worknotes.spring_boot.integration.wechat.WechatInboundConfig;
import com.entropybits.worknotes.spring_boot.integration.wechat.WechatTokenManager;
import com.entropybits.worknotes.spring_boot.service.ClipImportService;
import com.entropybits.worknotes.spring_boot.service.SourceClipService;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WechatInboundMessageServiceTest {

    @Mock private WechatBindingService bindingService;
    @Mock private ClipImportService clipImportService;
    @Mock private SourceClipService sourceClipService;
    @Mock private WechatApiClient apiClient;
    @Mock private WechatTokenManager tokenManager;

    private WechatInboundConfig inboundConfig;
    private WechatInboundMessageService service;

    private final User alice = User.builder().id(1L).username("alice").build();

    @BeforeEach
    void setUp() {
        inboundConfig = new WechatInboundConfig();
        inboundConfig.setAppId("inbound-app");
        inboundConfig.setAppSecret("inbound-secret");
        service = new WechatInboundMessageService(
            bindingService, clipImportService, sourceClipService, apiClient, tokenManager, inboundConfig);
        lenient().when(tokenManager.getAccessToken("inbound-app", "inbound-secret")).thenReturn("token-x");
    }

    private String textXml(String openid, String content, String msgId) {
        return "<xml><ToUserName><![CDATA[gh_1]]></ToUserName>"
            + "<FromUserName><![CDATA[" + openid + "]]></FromUserName>"
            + "<CreateTime>1</CreateTime><MsgType><![CDATA[text]]></MsgType>"
            + "<Content><![CDATA[" + content + "]]></Content>"
            + "<MsgId>" + msgId + "</MsgId></xml>";
    }

    private String linkXml(String openid, String url, String msgId) {
        return "<xml><ToUserName><![CDATA[gh_1]]></ToUserName>"
            + "<FromUserName><![CDATA[" + openid + "]]></FromUserName>"
            + "<CreateTime>1</CreateTime><MsgType><![CDATA[link]]></MsgType>"
            + "<Title><![CDATA[标题]]></Title><Url><![CDATA[" + url + "]]></Url>"
            + "<MsgId>" + msgId + "</MsgId></xml>";
    }

    @Test
    void promptsBindingWhenOpenidUnboundAndNotAValidCode() {
        when(bindingService.findBoundUser("openid-1")).thenReturn(Optional.empty());
        when(bindingService.tryConsumeBindCode("openid-1", "hello")).thenReturn(Optional.empty());

        String reply = service.handleIncoming(textXml("openid-1", "hello", "msg-1"));

        assertThat(reply).contains("请先在 mindio 设置页生成绑定码");
    }

    @Test
    void confirmsBindingWhenValidCodeConsumed() {
        when(bindingService.findBoundUser("openid-1")).thenReturn(Optional.empty());
        when(bindingService.tryConsumeBindCode("openid-1", "654321")).thenReturn(Optional.of(alice));

        String reply = service.handleIncoming(textXml("openid-1", "654321", "msg-2"));

        assertThat(reply).contains("绑定成功");
    }

    @Test
    void savesPlainTextAsQuickNoteForBoundUser() {
        when(bindingService.findBoundUser("openid-1")).thenReturn(Optional.of(alice));
        when(sourceClipService.createClip(any(), eq("alice")))
            .thenReturn(SourceClipResponse.builder().id(100L).title("速记").build());

        String reply = service.handleIncoming(textXml("openid-1", "今天读到一篇好文章", "msg-3"));

        assertThat(reply).contains("收到，正在处理");

        ArgumentCaptor<SourceClipRequest> captor = ArgumentCaptor.forClass(SourceClipRequest.class);
        Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() ->
            verify(sourceClipService).createClip(captor.capture(), eq("alice")));
        assertThat(captor.getValue().getSourceType()).isEqualTo(SourceClip.SourceType.WECHAT_CHAT_TEXT);
        assertThat(captor.getValue().getContent()).isEqualTo("今天读到一篇好文章");

        Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() ->
            verify(apiClient).sendCustomerServiceText(eq("token-x"), eq("openid-1"), contains("已保存")));
    }

    @Test
    void savesLinkMessageViaClipImportService() {
        when(bindingService.findBoundUser("openid-1")).thenReturn(Optional.of(alice));
        SourceClipDraft draft = SourceClipDraft.builder()
            .sourceType(SourceClip.SourceType.WEBPAGE)
            .sourceUrl("https://example.com/a")
            .suggestedTitle("示例文章")
            .content("<p>正文</p>")
            .contentFormat("html")
            .extractionMode(SourceClip.ExtractionMode.FULL)
            .extractionStatus(SourceClip.ExtractionStatus.SUCCESS)
            .fetchSuccess(true)
            .build();
        when(clipImportService.fetchFromUrl(any(ClipImportUrlRequest.class))).thenReturn(draft);
        when(sourceClipService.createClip(any(), eq("alice")))
            .thenReturn(SourceClipResponse.builder().id(101L).title("示例文章").build());

        String reply = service.handleIncoming(linkXml("openid-1", "https://example.com/a", "msg-4"));

        assertThat(reply).contains("收到，正在处理");

        Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() ->
            verify(apiClient).sendCustomerServiceText(eq("token-x"), eq("openid-1"), contains("已保存《示例文章》")));
    }

    private String imageXml(String openid, String picUrl, String msgId) {
        return "<xml><ToUserName><![CDATA[gh_1]]></ToUserName>"
            + "<FromUserName><![CDATA[" + openid + "]]></FromUserName>"
            + "<CreateTime>1</CreateTime><MsgType><![CDATA[image]]></MsgType>"
            + "<PicUrl><![CDATA[" + picUrl + "]]></PicUrl>"
            + "<MsgId>" + msgId + "</MsgId></xml>";
    }

    @Test
    void savesImageMessageByDownloadingPicUrl() {
        when(bindingService.findBoundUser("openid-1")).thenReturn(Optional.of(alice));
        when(clipImportService.downloadAndSaveImage("http://mmbiz.qpic.cn/pic.jpg", "http://mmbiz.qpic.cn/pic.jpg"))
            .thenReturn("/uploads/worknotesimage/public/clip_image/2026/07/22/abc.jpg");
        when(sourceClipService.createClip(any(), eq("alice")))
            .thenReturn(SourceClipResponse.builder().id(103L).title("图片").build());

        String reply = service.handleIncoming(imageXml("openid-1", "http://mmbiz.qpic.cn/pic.jpg", "msg-5"));

        assertThat(reply).contains("收到，正在处理");

        ArgumentCaptor<SourceClipRequest> captor = ArgumentCaptor.forClass(SourceClipRequest.class);
        Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() ->
            verify(sourceClipService).createClip(captor.capture(), eq("alice")));
        assertThat(captor.getValue().getSourceType()).isEqualTo(SourceClip.SourceType.WECHAT_CHAT_IMAGE);
        assertThat(captor.getValue().getContent())
            .contains("/uploads/worknotesimage/public/clip_image/2026/07/22/abc.jpg");

        Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() ->
            verify(apiClient).sendCustomerServiceText(eq("token-x"), eq("openid-1"), contains("已保存")));
    }

    @Test
    void reportsFailureWhenImageDownloadFails() {
        when(bindingService.findBoundUser("openid-1")).thenReturn(Optional.of(alice));
        when(clipImportService.downloadAndSaveImage(anyString(), anyString())).thenReturn(null);
        when(sourceClipService.createClip(any(), eq("alice")))
            .thenReturn(SourceClipResponse.builder().id(104L).title("图片").build());

        service.handleIncoming(imageXml("openid-1", "http://mmbiz.qpic.cn/broken.jpg", "msg-6"));

        Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() ->
            verify(apiClient).sendCustomerServiceText(eq("token-x"), eq("openid-1"), contains("下载失败")));
        verify(sourceClipService).createClip(any(), eq("alice"));
    }

    @Test
    void deduplicatesRetriedMessageByMsgId() {
        when(bindingService.findBoundUser("openid-1")).thenReturn(Optional.of(alice));
        when(sourceClipService.createClip(any(), eq("alice")))
            .thenReturn(SourceClipResponse.builder().id(102L).title("速记").build());

        service.handleIncoming(textXml("openid-1", "重复消息", "msg-dup"));
        service.handleIncoming(textXml("openid-1", "重复消息", "msg-dup"));

        Awaitility.await().atMost(Duration.ofSeconds(2)).untilAsserted(() ->
            verify(sourceClipService, times(1)).createClip(any(), eq("alice")));
    }
}
