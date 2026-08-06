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

@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeishuWikiNodeListResponse extends FeishuBaseResponse {

    private DataNode data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataNode {
        @JsonProperty("items")
        private List<Item> items;

        @JsonProperty("page_token")
        private String pageToken;

        @JsonProperty("has_more")
        private Boolean hasMore;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        @JsonProperty("node_token")
        private String nodeToken;

        /**
         * 实际文档对象 token，例如 Docx 文档的 doc_token。
         * 用于后续调用 Docx 内容接口。
         */
        @JsonProperty("obj_token")
        private String objToken;

        @JsonProperty("title")
        private String title;

        @JsonProperty("obj_type")
        private String objType;

        @JsonProperty("has_child")
        private Boolean hasChild;

        @JsonProperty("parent_node_token")
        private String parentNodeToken;

        @JsonProperty("obj_create_time")
        private String objCreateTime;

        @JsonProperty("obj_edit_time")
        private String objEditTime;
    }
}


