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
 * 飞书云空间文件列表响应
 *
 * @see <a href="https://open.feishu.cn/document/server-docs/docs/drive-v1/folder/list">飞书文档</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeishuDriveFileListResponse extends FeishuBaseResponse {

    private DataNode data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataNode {
        private List<FileItem> files;

        @JsonProperty("has_more")
        private Boolean hasMore;

        @JsonProperty("page_token")
        private String pageToken;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FileItem {
        /**
         * 文件/文件夹 token
         */
        private String token;

        /**
         * 文件/文件夹名称
         */
        private String name;

        /**
         * 类型：folder/docx/doc/sheet/bitable/mindnote 等
         */
        private String type;

        /**
         * 父文件夹 token
         */
        @JsonProperty("parent_token")
        private String parentToken;

        /**
         * 创建时间（Unix 时间戳，秒）
         */
        @JsonProperty("created_time")
        private String createdTime;

        /**
         * 修改时间（Unix 时间戳，秒）
         */
        @JsonProperty("modified_time")
        private String modifiedTime;

        /**
         * 文件所有者 ID
         */
        @JsonProperty("owner_id")
        private String ownerId;

        /**
         * 文件 URL
         */
        private String url;
    }
}
