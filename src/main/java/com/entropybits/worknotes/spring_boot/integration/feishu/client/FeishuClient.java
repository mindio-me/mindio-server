/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.feishu.client;

import com.entropybits.worknotes.spring_boot.integration.feishu.FeishuProperties;
import com.entropybits.worknotes.spring_boot.integration.feishu.client.model.FeishuBatchDownloadUrlResponse;
import com.entropybits.worknotes.spring_boot.integration.feishu.client.model.FeishuDocxBlocksResponse;
import com.entropybits.worknotes.spring_boot.integration.feishu.client.model.FeishuDocxContentResponse;
import com.entropybits.worknotes.spring_boot.integration.feishu.client.model.FeishuDriveFileListResponse;
import com.entropybits.worknotes.spring_boot.integration.feishu.client.model.FeishuOAuthTokenResponse;
import com.entropybits.worknotes.spring_boot.integration.feishu.client.model.FeishuRootFolderMetaResponse;
import com.entropybits.worknotes.spring_boot.integration.feishu.client.model.FeishuWikiNodeListResponse;
import com.entropybits.worknotes.spring_boot.integration.feishu.client.model.FeishuWikiSpaceListResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * Minimal Feishu OpenAPI client for OAuth + Wiki listing.
 * <p>
 * Note: endpoints may evolve; keep them centralized here for easy adjustments.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings("null")
public class FeishuClient {

    @Value("${spring.profiles.active:}")
    private String activeProfile;
    
    // 或者检查是否包含特定profile
    public boolean isProd() {
        return activeProfile.equals("prod");
    }
    
    private final RestTemplate restTemplate;
    private final FeishuProperties props;

    public String buildAuthorizeUrl(String appId, String state) {
        // https://open.feishu.cn/open-apis/authen/v1/authorize?app_id=...&redirect_uri=...&state=...&scope=...
        // scope 参数用于请求用户授权特定权限（多个scope用空格分隔，URL编码后为%20）
        // 注意：必须包含 offline_access 才能获取 refresh_token
        // 参考：https://open.feishu.cn/document/server-docs/authentication-management/access-token/create
        String scope = "wiki:wiki:readonly docx:document:readonly offline_access docs:document.media:download drive:file:download drive:drive:readonly drive:drive.metadata:readonly";
        return UriComponentsBuilder
                .fromHttpUrl(props.getBaseUrl())
                .path("/open-apis/authen/v1/authorize")
                .queryParam("app_id", appId)
                .queryParam("redirect_uri", props.getOauthRedirectUri())
                .queryParam("state", state)
                .queryParam("scope", scope)
                .build()
                .encode()
                .toUriString();
    }

    /**
     * OAuth: exchange authorization code for user access token.
     * <p>
     * 使用飞书官方 v2 接口：POST /open-apis/authen/v2/oauth/token
     * 这是飞书官方推荐的统一 OAuth token 端点，用于获取和刷新用户 access_token。
     * v2 接口是标准的 OAuth 2.0 token 端点，相比老接口有更好的稳定性和官方支持。
     *
     * @see <a href="https://open.feishu.cn/open-apis/authen/v2/oauth/token">飞书 v2 OAuth token 接口</a>
     * @see <a href="https://open.feishu.cn/document/authentication-management/access-token/get-user-access-token">飞书官方文档</a>
     */
    public FeishuOAuthTokenResponse exchangeOAuthCode(String appId, String appSecret, String code) {
        Map<String, Object> body = new HashMap<>();
        body.put("grant_type", "authorization_code");
        body.put("code", code);
        body.put("client_id", appId);
        body.put("client_secret", appSecret);
        body.put("redirect_uri", props.getOauthRedirectUri());

        return executeApiCall(
            HttpMethod.POST,
            "/open-apis/authen/v2/oauth/token",
            body,
            null,
            FeishuOAuthTokenResponse.class,
            "exchangeOAuthCode"
        );
    }

    /**
     * OAuth: refresh user access token by refresh_token.
     * <p>
     * 使用飞书官方 v2 接口：POST /open-apis/authen/v2/oauth/token
     * 这是飞书官方推荐的统一 OAuth token 端点，用于获取和刷新用户 access_token。
     * v2 接口是标准的 OAuth 2.0 token 端点，相比老接口有更好的稳定性和官方支持。
     * 
     * @see <a href="https://open.feishu.cn/open-apis/authen/v2/oauth/token">飞书 v2 OAuth token 接口</a>
     * @see <a href="https://open.feishu.cn/document/server-docs/authentication-management/access-token/create">飞书老接口文档（已废弃）</a>
     */
    public FeishuOAuthTokenResponse refreshOAuthToken(String appId, String appSecret, String refreshToken) {
        Map<String, Object> body = new HashMap<>();
        body.put("grant_type", "refresh_token");
        body.put("refresh_token", refreshToken);
        body.put("client_id", appId);
        body.put("client_secret", appSecret);

        return executeApiCall(
            HttpMethod.POST,
            "/open-apis/authen/v2/oauth/token",
            body,
            null,
            FeishuOAuthTokenResponse.class,
            "refreshOAuthToken"
        );
    }

    public FeishuWikiSpaceListResponse listWikiSpaces(String accessToken) {
        return executeApiCall(
            HttpMethod.GET,
            "/open-apis/wiki/v2/spaces",
            null,
            accessToken,
            FeishuWikiSpaceListResponse.class,
            "listWikiSpaces"
        );
    }

    public FeishuWikiNodeListResponse listWikiNodes(String accessToken, String spaceId, String pageToken, Integer pageSize, String parentNodeToken) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromPath("/open-apis/wiki/v2/spaces/{space_id}/nodes")
                .queryParam("page_size", pageSize == null ? 50 : pageSize);

        if (pageToken != null && !pageToken.isBlank()) {
            builder.queryParam("page_token", pageToken);
        }
        if (parentNodeToken != null && !parentNodeToken.isBlank()) {
            builder.queryParam("parent_node_token", parentNodeToken);
        }

        // 在这里就替换路径参数，避免在 executeApiCall 中处理已编码的 URL
        String path = builder.buildAndExpand(spaceId).toUriString();

        return executeApiCall(
            HttpMethod.GET,
            path,
            null,
            accessToken,
            FeishuWikiNodeListResponse.class,
            "listWikiNodes"
        );
    }

    /**
     * 获取 Docx 文档原始内容。
     * @see <a href="https://open.feishu.cn/document/ukTMukTMukTM/uUDN04SN0QjL1QDN/document-docx/docx-v1/document/raw_content">飞书文档</a>
     */
    public FeishuDocxContentResponse getDocxMarkdown(String accessToken, String documentId) {
        log.debug("Fetching docx content: documentId={}", documentId);
        long startTime = System.currentTimeMillis();

        Map<String, Object> pathParams = new HashMap<>();
        pathParams.put("document_id", documentId);

        FeishuDocxContentResponse resp = executeApiCall(
            HttpMethod.GET,
            "/open-apis/docx/v1/documents/{document_id}/raw_content",
            null,
            accessToken,
            FeishuDocxContentResponse.class,
            "getDocxMarkdown",
            pathParams
        );

        if (resp != null && resp.getData() != null) {
            int contentSize = resp.getData().getContent() != null
                    ? resp.getData().getContent().length() : 0;
            log.info("Docx content fetched: documentId={}, size={} chars, time={}ms",
                    documentId, contentSize, System.currentTimeMillis() - startTime);
        } else {
            log.warn("Docx content fetch returned null or empty: documentId={}, time={}ms",
                    documentId, System.currentTimeMillis() - startTime);
        }

        return resp;
    }

    /**
     * 获取 Docx 文档 blocks（支持分页）
     * <p>
     * 用于获取完整文档内容，支持大文档的分页处理。
     *
     * @param accessToken 访问令牌
     * @param documentId 文档ID
     * @param pageToken 分页token，首次调用传null
     * @param pageSize 每页大小，最大500
     * @return blocks响应
     * @see <a href="https://open.feishu.cn/document/server-docs/docs/docs/docx-v1/document-block/get-2">飞书文档</a>
     */
    public FeishuDocxBlocksResponse getDocxBlocks(
            String accessToken,
            String documentId,
            String pageToken,
            Integer pageSize) {

        log.debug("Fetching docx blocks: documentId={}, pageToken={}, pageSize={}",
                documentId, pageToken, pageSize);
        long startTime = System.currentTimeMillis();

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromPath("/open-apis/docx/v1/documents/{document_id}/blocks")
                .queryParam("document_revision_id", -1)
                .queryParam("page_size", pageSize != null ? pageSize : 500);

        if (pageToken != null && !pageToken.isBlank()) {
            builder.queryParam("page_token", pageToken);
        }

        // 在这里就替换路径参数，避免在 executeApiCall 中处理已编码的 URL
        String path = builder.buildAndExpand(documentId).toUriString();

        FeishuDocxBlocksResponse resp = executeApiCall(
            HttpMethod.GET,
            path,
            null,
            accessToken,
            FeishuDocxBlocksResponse.class,
            "getDocxBlocks"
        );

        if (resp != null && resp.getData() != null) {
            int itemCount = resp.getData().getItems() != null ? resp.getData().getItems().size() : 0;
            log.info("Docx blocks fetched: documentId={}, items={}, hasMore={}, time={}ms",
                    documentId, itemCount, resp.getData().getHasMore(),
                    System.currentTimeMillis() - startTime);
        } else {
            log.warn("Docx blocks fetch returned null or empty: documentId={}, time={}ms",
                    documentId, System.currentTimeMillis() - startTime);
        }

        return resp;
    }

    /**
     * 批量获取图片临时下载链接
     * <p>
     * 用于下载飞书文档中的图片。一次最多支持5个file_token。
     * 临时下载链接有效期约4小时。
     *
     * @param accessToken 访问令牌
     * @param fileTokens 图片file_token列表（最多5个）
     * @return 批量下载URL响应
     * @see <a href="https://open.feishu.cn/document/server-docs/docs/drive-v1/media/batch_get_tmp_download_url">飞书文档</a>
     */
    public FeishuBatchDownloadUrlResponse getBatchTmpDownloadUrl(
            String accessToken,
            java.util.List<String> fileTokens) {

        log.debug("Fetching batch tmp download urls: fileTokens={}", fileTokens);
        long startTime = System.currentTimeMillis();

        if (fileTokens == null || fileTokens.isEmpty()) {
            log.warn("fileTokens is null or empty");
            return null;
        }

        if (fileTokens.size() > 5) {
            log.warn("fileTokens size {} exceeds limit 5, will be truncated", fileTokens.size());
            fileTokens = fileTokens.subList(0, 5);
        }

        // 构建查询参数：file_tokens=token1&file_tokens=token2&...
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromPath("/open-apis/drive/v1/medias/batch_get_tmp_download_url");

        for (String token : fileTokens) {
            builder.queryParam("file_tokens", token);
        }

        String path = builder.toUriString();

        FeishuBatchDownloadUrlResponse resp = executeApiCall(
            HttpMethod.GET,
            path,
            null,
            accessToken,
            FeishuBatchDownloadUrlResponse.class,
            "getBatchTmpDownloadUrl"
        );

        if (resp != null && resp.getData() != null && resp.getData().getTmpDownloadUrls() != null) {
            int urlCount = resp.getData().getTmpDownloadUrls().size();
            log.info("Batch tmp download urls fetched: requested={}, received={}, time={}ms",
                    fileTokens.size(), urlCount, System.currentTimeMillis() - startTime);
        } else {
            log.warn("Batch tmp download urls fetch returned null or empty: time={}ms",
                    System.currentTimeMillis() - startTime);
        }

        return resp;
    }

    /**
     * 获取云空间文件列表（我的文档资料库）
     * <p>
     * 用于浏览和导入用户的云空间文件。
     *
     * @param accessToken 访问令牌
     * @param folderToken 文件夹 token，null 表示根目录
     * @param pageToken   分页 token
     * @param pageSize    每页大小
     * @return 文件列表响应
     * @see <a href="https://open.feishu.cn/document/server-docs/docs/drive-v1/folder/list">飞书文档</a>
     */
    public FeishuDriveFileListResponse listDriveFiles(
            String accessToken,
            String folderToken,
            String pageToken,
            Integer pageSize) {

        log.debug("Listing drive files: folderToken={}, pageToken={}, pageSize={}",
                folderToken, pageToken, pageSize);
        long startTime = System.currentTimeMillis();

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromPath("/open-apis/drive/v1/files")
                .queryParam("page_size", pageSize != null ? pageSize : 50);

        if (folderToken != null && !folderToken.isBlank()) {
            builder.queryParam("folder_token", folderToken);
        }
        if (pageToken != null && !pageToken.isBlank()) {
            builder.queryParam("page_token", pageToken);
        }

        String path = builder.toUriString();

        FeishuDriveFileListResponse resp = executeApiCall(
                HttpMethod.GET,
                path,
                null,
                accessToken,
                FeishuDriveFileListResponse.class,
                "listDriveFiles"
        );

        if (resp != null && resp.getData() != null && resp.getData().getFiles() != null) {
            int fileCount = resp.getData().getFiles().size();
            log.info("Drive files listed: folderToken={}, count={}, hasMore={}, time={}ms",
                    folderToken, fileCount, resp.getData().getHasMore(),
                    System.currentTimeMillis() - startTime);
        } else {
            log.warn("Drive files list returned null or empty: folderToken={}, time={}ms",
                    folderToken, System.currentTimeMillis() - startTime);
        }

        return resp;
    }

    /**
     * 获取用户个人空间（我的文档库）根目录元信息
     *
     * @param accessToken 访问令牌
     * @return 根目录元信息（含 folder token）
     * @see <a href="https://open.feishu.cn/document/server-docs/docs/drive-v1/folder/get-root-folder-meta">飞书文档</a>
     */
    public FeishuRootFolderMetaResponse getRootFolderMeta(String accessToken) {
        log.debug("Getting root folder meta for my docs space");
        long startTime = System.currentTimeMillis();

        FeishuRootFolderMetaResponse resp = executeApiCall(
                HttpMethod.GET,
                "/open-apis/drive/explorer/v2/root_folder/meta",
                null,
                accessToken,
                FeishuRootFolderMetaResponse.class,
                "getRootFolderMeta"
        );

        if (resp != null && resp.getData() != null) {
            log.info("Root folder meta fetched: token={}, time={}ms",
                    resp.getData().getToken(), System.currentTimeMillis() - startTime);
        } else {
            log.warn("Root folder meta returned null: time={}ms", System.currentTimeMillis() - startTime);
        }

        return resp;
    }

    /**
     * 统一的API调用入口方法
     *
     * @param method HTTP方法
     * @param path API路径
     * @param requestBody 请求体（POST/PUT请求）
     * @param accessToken 访问令牌（GET请求需要）
     * @param responseType 响应类型
     * @param methodName 方法名称（用于日志）
     * @param pathParams 路径参数
     * @return API响应
     */
    private <T> T executeApiCall(
            HttpMethod method,
            String path,
            Map<String, Object> requestBody,
            String accessToken,
            Class<T> responseType,
            String methodName,
            Map<String, Object>... pathParams) {

        // 构建完整URL
        String baseUrl = props.getBaseUrl();
        String fullUrl = baseUrl + path;

        // 处理路径参数
        if (pathParams != null && pathParams.length > 0 && pathParams[0] != null) {
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(fullUrl);
            fullUrl = uriBuilder.buildAndExpand(pathParams[0]).toUriString();
        }

        // 准备请求头
        HttpHeaders headers = new HttpHeaders();
        if (accessToken != null) {
            headers.setBearerAuth(accessToken);
        }
        headers.setAccept(MediaType.parseMediaTypes("application/json"));
        
        if (method == HttpMethod.POST || method == HttpMethod.PUT) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }

        // 创建请求实体
        HttpEntity<?> requestEntity = (requestBody != null) 
            ? new HttpEntity<>(requestBody, headers)
            : new HttpEntity<>(headers);

        // 记录API调用详情
        logApiCall(method, fullUrl, requestBody, methodName, accessToken);

        // 执行API调用
        ResponseEntity<T> response = restTemplate.exchange(
            fullUrl, method, requestEntity, responseType);

        // 记录API响应详情
        logApiResponse(response, methodName);

        return response.getBody();
    }

    /**
     * 记录API调用详情（不包含敏感信息）
     */
    private void logApiCall(HttpMethod method, String url, Map<String, Object> requestBody, String methodName, String accessToken) {
        String maskedToken = (accessToken != null) ? maskToken(accessToken) : "None";
        log.info("[Feishu API Call] Method: {}, URL: {}, MethodName: {}, AccessToken: {}", 
                method, url, methodName, maskedToken);
        
        if (requestBody != null && !requestBody.isEmpty()) {
            log.info("[Feishu API Request Body] Method: {}, Body: {}", 
                    methodName, maskSensitiveFields(requestBody));
        }
    }

    /**
     * 记录API响应详情
     */
    private <T> void logApiResponse(ResponseEntity<T> response, String methodName) {
        if (response != null) {
            log.info("[Feishu API Response] Method: {}, Status: {}, Headers: {}", 
                    methodName, response.getStatusCode(), response.getHeaders());
            
            T body = response.getBody();
            if (body != null) {
                handleResponseBody(body, methodName);
            }
        } else {
            log.warn("[Feishu API Response] Method: {}, Response is null", methodName);
        }
    }

    /**
     * 处理不同类型的响应体
     */
    private <T> void handleResponseBody(T body, String methodName) {
        String bodyStr = body.toString();
        
        if (body instanceof FeishuOAuthTokenResponse) {
            // OAuth响应特殊处理
            FeishuOAuthTokenResponse tokenResp = (FeishuOAuthTokenResponse) body;
            if (tokenResp.getData() != null) {
                log.info("[Feishu API Response Body] Method: {}, Body: {{}={}, {}={}, {}={}, {}={}, {}={}, {}={}, {}={}, {}={}, {}={}}",
                        methodName,
                        "code", tokenResp.getCode(),
                        "msg", tokenResp.getMsg(),
                        "access_token", maskToken(tokenResp.getData().getAccessToken()),
                        "refresh_token", maskToken(tokenResp.getData().getRefreshToken()),
                        "expires_in", tokenResp.getData().getExpiresIn(),
                        "refresh_token_expires_in", tokenResp.getData().getRefreshTokenExpiresIn(),
                        "token_type", tokenResp.getData().getTokenType(),
                        "scope", tokenResp.getData().getScope());
            }
        } else {
            // 普通响应体处理
            String limitedBody = bodyStr.length() > 1000 ? bodyStr.substring(0, 1000) + "...(truncated)" : bodyStr;
            log.info("[Feishu API Response Body] Method: {}, Body: {}", methodName, limitedBody);
        }
    }

    /**
     * 对令牌进行脱敏处理
     */
    private String maskToken(String token) {
        if (token == null || token.isEmpty()) {
            return "[EMPTY]";
        }
        if (token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 4) + "***" + token.substring(token.length() - 4);
    }

    /**
     * 对敏感数据进行脱敏处理
     */
    private String maskSensitiveData(String data) {
        if (data == null || data.isEmpty()) {
            return "[EMPTY]";
        }
        return "***";
    }

    /**
     * 对请求体中的敏感字段进行脱敏处理
     */
    private Map<String, Object> maskSensitiveFields(Map<String, Object> requestBody) {
        if (requestBody == null) return null;
        
        Map<String, Object> masked = new HashMap<>(requestBody);
        
        // 脱敏敏感字段
        // 支持 v2 接口的 client_secret 和旧版本的 app_secret
        if (masked.containsKey("client_secret")) {
            if (isProd()) {
                masked.put("client_secret", maskSensitiveData((String) masked.get("client_secret")));
            }
            else {
                masked.put("client_secret", masked.get("client_secret"));
            }
        }
        if (masked.containsKey("app_secret")) {
            if (isProd()) {
                masked.put("app_secret", maskSensitiveData((String) masked.get("app_secret")));
            }
            else {
                masked.put("app_secret", masked.get("app_secret"));
            }
        }
        if (masked.containsKey("refresh_token")) {
            if (isProd()) {
                masked.put("refresh_token", maskToken((String) masked.get("refresh_token")));
            }
            else {
                masked.put("refresh_token", masked.get("refresh_token"));
            }
        }
        if (masked.containsKey("code")) {
            String code = (String) masked.get("code");
            if (isProd()) {
                masked.put("code", maskToken(code));
            }
            else {
                masked.put("code", masked.get("code"));
            }
        }
        
        return masked;
    }
}