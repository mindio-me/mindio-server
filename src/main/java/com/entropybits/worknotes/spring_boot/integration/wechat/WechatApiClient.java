/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.wechat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class WechatApiClient {

    private static final String BASE_URL = "https://api.weixin.qq.com/cgi-bin";

    private final WechatTokenManager tokenManager;
    private final WechatConfig config;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public WechatApiClient(WechatTokenManager tokenManager, WechatConfig config, ObjectMapper objectMapper) {
        this(tokenManager, config, objectMapper,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build());
    }

    /** Package-private constructor for unit tests — allows injecting a mock HttpClient. */
    WechatApiClient(WechatTokenManager tokenManager, WechatConfig config, ObjectMapper objectMapper, HttpClient httpClient) {
        this.tokenManager = tokenManager;
        this.config = config;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    private String accessToken() {
        return tokenManager.getAccessToken(config.getAppId(), config.getAppSecret());
    }

    /** 上传永久图片素材，返回 media_id 和 CDN url */
    public UploadResult uploadPermanentImage(byte[] imageBytes, String filename) throws Exception {
        String url = BASE_URL + "/material/add_material?access_token="
            + accessToken() + "&type=image";

        String boundary = "Boundary" + System.currentTimeMillis();
        String fileContentType = filename.toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";
        byte[] multipartBody = buildMultipart(boundary, filename, fileContentType, imageBytes);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
            .timeout(Duration.ofSeconds(30))
            .build();

        return sendAndParse(request, response -> {
            String mediaId = (String) response.get("media_id");
            String cdnUrl  = (String) response.get("url");
            return new UploadResult(mediaId, cdnUrl);
        }, "Upload image");
    }

    /** 创建草稿，返回草稿 media_id */
    public String createDraft(String title, String author, String digest,
                               String content, String thumbMediaId) throws Exception {
        String url = BASE_URL + "/draft/add?access_token=" + accessToken();

        // LinkedHashMap 保证字段顺序，且允许空字符串值
        Map<String, Object> article = new LinkedHashMap<>();
        article.put("title",               title);
        article.put("author",              author != null ? author : "");
        article.put("digest",              digest != null ? digest : "");
        article.put("content",             content);
        article.put("thumb_media_id",      thumbMediaId);
        article.put("content_source_url",  "");
        article.put("need_open_comment",   0);
        article.put("only_fans_can_comment", 0);

        Map<String, Object> body = Map.of("articles", List.of(article));

        log.info("createDraft: title={}, thumbMediaId={}, contentLen={}",
            title, thumbMediaId, content != null ? content.length() : 0);

        HttpRequest request = buildJsonPost(url, body);
        return sendAndParse(request, response -> (String) response.get("media_id"), "Create draft");
    }

    /** 提交群发任务，返回 publish_id */
    public String submitPublish(String mediaId) throws Exception {
        String url = BASE_URL + "/freepublish/submit?access_token=" + accessToken();

        HttpRequest request = buildJsonPost(url, Map.of("media_id", mediaId));
        return sendAndParse(request, response -> {
            Object publishId = response.get("publish_id");
            return publishId != null ? publishId.toString() : "";
        }, "Submit publish");
    }

    /** 发送客服消息文本（用户 48 小时内有过互动即可调用），accessToken 由调用方显式传入 */
    public void sendCustomerServiceText(String accessToken, String openid, String text) throws Exception {
        String url = BASE_URL + "/message/custsend?access_token=" + accessToken;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("touser", openid);
        body.put("msgtype", "text");
        body.put("text", Map.of("content", text));

        HttpRequest request = buildJsonPost(url, body);
        sendAndParse(request, response -> null, "Send customer service text");
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private HttpRequest buildJsonPost(String url, Object body) throws Exception {
        String json = objectMapper.writeValueAsString(body);
        log.debug("POST {} body={}", url.replaceAll("access_token=[^&]+", "access_token=***"), json);
        return HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json; charset=utf-8")
            .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
            .timeout(Duration.ofSeconds(30))
            .build();
    }

    @FunctionalInterface
    private interface ResponseMapper<T> {
        T map(Map<String, Object> response) throws Exception;
    }

    private <T> T sendAndParse(HttpRequest request, ResponseMapper<T> mapper,
                                String operation) throws Exception {
        HttpResponse<String> httpResponse = httpClient.send(request,
            HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        log.debug("{} response status={} body={}", operation,
            httpResponse.statusCode(), httpResponse.body());

        if (httpResponse.statusCode() >= 400) {
            throw new RuntimeException(operation + " failed: HTTP " + httpResponse.statusCode()
                + " body=" + httpResponse.body());
        }

        Map<String, Object> response = objectMapper.readValue(httpResponse.body(),
            new TypeReference<Map<String, Object>>() {});
        checkError(response, operation);
        return mapper.map(response);
    }

    private byte[] buildMultipart(String boundary, String filename,
                                   String contentType, byte[] data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String CRLF = "\r\n";
        out.write(("--" + boundary + CRLF).getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"media\"; filename=\"" + filename + "\"" + CRLF)
            .getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Type: " + contentType + CRLF + CRLF).getBytes(StandardCharsets.UTF_8));
        out.write(data);
        out.write((CRLF + "--" + boundary + "--" + CRLF).getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    private void checkError(Map<String, Object> response, String operation) {
        if (response == null) {
            throw new RuntimeException(operation + " failed: null response from WeChat API");
        }
        Object errcode = response.get("errcode");
        if (errcode != null && ((Number) errcode).intValue() != 0) {
            throw new RuntimeException(operation + " failed: "
                + response.get("errmsg") + " (errcode=" + errcode + ")");
        }
    }
}
