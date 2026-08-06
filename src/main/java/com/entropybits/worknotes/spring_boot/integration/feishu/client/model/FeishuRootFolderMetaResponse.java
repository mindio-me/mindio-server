/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.feishu.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 飞书个人空间根目录元信息响应
 *
 * @see <a href="https://open.feishu.cn/document/server-docs/docs/drive-v1/folder/get-root-folder-meta">飞书文档</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeishuRootFolderMetaResponse extends FeishuBaseResponse {

    private DataNode data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataNode {
        /**
         * 根目录 folder token（即"我的文档库"根）
         */
        private String token;

        /**
         * 根目录 ID
         */
        private String id;

        /**
         * 用户 ID
         */
        @JsonProperty("user_id")
        private String userId;
    }
}
