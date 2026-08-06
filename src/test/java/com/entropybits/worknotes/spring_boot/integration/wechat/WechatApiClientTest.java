/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.wechat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WechatApiClientTest {

    @Mock
    private WechatTokenManager tokenManager;

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private WechatConfig config;
    private WechatApiClient apiClient;

    @BeforeEach
    void setUp() throws Exception {
        config = new WechatConfig();
        config.setAppId("pub-app-id");
        config.setAppSecret("pub-app-secret");

        lenient().when(tokenManager.getAccessToken("pub-app-id", "pub-app-secret")).thenReturn("test-token");
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());
        apiClient = new WechatApiClient(tokenManager, config, new ObjectMapper(), httpClient);
    }

    @Test
    void uploadPermanentImageReturnsResult() throws Exception {
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"media_id\":\"media-123\",\"url\":\"https://mmbiz.qpic.cn/abc\"}");

        UploadResult result = apiClient.uploadPermanentImage(new byte[]{1, 2, 3}, "test.jpg");

        assertThat(result.mediaId()).isEqualTo("media-123");
        assertThat(result.url()).isEqualTo("https://mmbiz.qpic.cn/abc");
    }

    @Test
    void createDraftReturnsMediaId() throws Exception {
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"media_id\":\"draft-456\"}");

        String mediaId = apiClient.createDraft("Title", "Author", "Digest", "<p>content</p>", "thumb-id");

        assertThat(mediaId).isEqualTo("draft-456");
    }

    @Test
    void submitPublishReturnsPublishId() throws Exception {
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"errcode\":0,\"publish_id\":\"pub-789\"}");

        String publishId = apiClient.submitPublish("draft-456");

        assertThat(publishId).isEqualTo("pub-789");
    }

    @Test
    void throwsOnApiError() throws Exception {
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"errcode\":45009,\"errmsg\":\"reach max api daily quota limit\"}");

        assertThatThrownBy(() -> apiClient.createDraft("T", "", "", "", "thumb"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("reach max api daily quota limit");
    }

    @Test
    void sendCustomerServiceTextPostsExpectedPayload() throws Exception {
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"errcode\":0,\"errmsg\":\"ok\"}");

        apiClient.sendCustomerServiceText("inbound-token", "openid-123", "已保存《测试标题》");

        verify(httpClient).send(argThat((HttpRequest req) ->
            req.uri().toString().contains("/message/custsend")
                && req.uri().toString().contains("access_token=inbound-token")
        ), any());
    }

    @Test
    void sendCustomerServiceTextThrowsOnApiError() throws Exception {
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"errcode\":45015,\"errmsg\":\"response out of time limit\"}");

        assertThatThrownBy(() -> apiClient.sendCustomerServiceText("inbound-token", "openid-123", "text"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("response out of time limit");
    }
}
