/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.feishu.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 飞书批量获取临时下载链接 API 响应模型
 * @see <a href="https://open.feishu.cn/document/server-docs/docs/drive-v1/media/batch_get_tmp_download_url">飞书文档</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeishuBatchDownloadUrlResponse extends FeishuBaseResponse {
    private DataNode data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataNode {
        /**
         * 临时下载链接列表
         */
        @JsonProperty("tmp_download_urls")
        private List<TmpDownloadUrl> tmpDownloadUrls;
    }

    /**
     * 单个文件的临时下载链接
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TmpDownloadUrl {
        /**
         * 文件token
         */
        @JsonProperty("file_token")
        private String fileToken;

        /**
         * 临时下载链接（有效期通常为4小时）
         */
        @JsonProperty("tmp_download_url")
        private String tmpDownloadUrl;
    }
}
