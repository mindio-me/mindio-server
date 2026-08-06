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
 * 飞书 Docx Blocks API 响应模型
 * @see <a href="https://open.feishu.cn/document/server-docs/docs/docs/docx-v1/document-block/get-2">飞书文档</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeishuDocxBlocksResponse extends FeishuBaseResponse {
    private DataNode data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataNode {
        private List<BlockItem> items;

        @JsonProperty("page_token")
        private String pageToken;

        @JsonProperty("has_more")
        private Boolean hasMore;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BlockItem {
        @JsonProperty("block_id")
        private String blockId;

        @JsonProperty("block_type")
        private Integer blockType;

        @JsonProperty("parent_id")
        private String parentId;

        @JsonProperty("children")
        private List<String> children;

        @JsonProperty("text")
        private TextRun text;

        // 图片块字段 (block_type = 27)
        @JsonProperty("image")
        private ImageBlock image;

        // 标题/列表/代码/引用/待办块字段 (block_type = 3~11, 12, 13, 14, 15, 17)
        // 飞书 API 不会把这些块的富文本内容放进通用的 "text" 字段，
        // 而是放进以块类型命名的独立字段中（结构与 text 相同）。
        @JsonProperty("heading1")
        private TextRun heading1;
        @JsonProperty("heading2")
        private TextRun heading2;
        @JsonProperty("heading3")
        private TextRun heading3;
        @JsonProperty("heading4")
        private TextRun heading4;
        @JsonProperty("heading5")
        private TextRun heading5;
        @JsonProperty("heading6")
        private TextRun heading6;
        @JsonProperty("heading7")
        private TextRun heading7;
        @JsonProperty("heading8")
        private TextRun heading8;
        @JsonProperty("heading9")
        private TextRun heading9;
        @JsonProperty("bullet")
        private TextRun bullet;
        @JsonProperty("ordered")
        private TextRun ordered;
        @JsonProperty("code")
        private TextRun code;
        @JsonProperty("quote")
        private TextRun quote;
        @JsonProperty("todo")
        private TextRun todo;

        /**
         * 根据 block_type 返回该块自身携带富文本内容的字段。
         * 飞书 API 把标题/列表/代码/引用/待办的内容放在各自同名字段中，而不是统一的 "text" 字段。
         */
        public TextRun resolveTextRun() {
            if (blockType == null) {
                return text;
            }
            switch (blockType) {
                case 3: return heading1;
                case 4: return heading2;
                case 5: return heading3;
                case 6: return heading4;
                case 7: return heading5;
                case 8: return heading6;
                case 9: return heading7;
                case 10: return heading8;
                case 11: return heading9;
                case 12: return bullet;
                case 13: return ordered;
                case 14: return code;
                case 15: return quote;
                case 17: return todo;
                default: return text;
            }
        }
    }

    /**
     * 图片块结构 (block_type = 27)
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ImageBlock {
        /**
         * 图片的 file_token（需要通过飞书API下载）
         */
        @JsonProperty("token")
        private String token;

        /**
         * 图片宽度（像素）
         */
        @JsonProperty("width")
        private Integer width;

        /**
         * 图片高度（像素）
         */
        @JsonProperty("height")
        private Integer height;

        /**
         * 对齐方式
         * 1: 居左排版
         * 2: 居中排版
         * 3: 居右排版
         */
        @JsonProperty("align")
        private Integer align;

        /**
         * 图片描述
         */
        @JsonProperty("caption")
        private Caption caption;
    }

    /**
     * 图片描述结构
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Caption {
        /**
         * 描述的文本内容
         */
        @JsonProperty("content")
        private String content;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TextRun {
        private String content;

        @JsonProperty("elements")
        private List<TextElement> elements;

        @JsonProperty("style")
        private TextStyle style;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TextElement {
        @JsonProperty("text_run")
        private TextContent textRun;

        /** 引用文档（mention_doc）内联元素 */
        @JsonProperty("mention_doc")
        private MentionDoc mentionDoc;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TextContent {
        private String content;

        @JsonProperty("text_element_style")
        private TextElementStyle textElementStyle;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TextElementStyle {
        private Boolean bold;
        private Boolean italic;
        private Boolean strikethrough;
        private Boolean underline;
        private Boolean inline_code;

        /** 超链接，存在时文本应渲染为 [text](url) */
        private LinkStyle link;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LinkStyle {
        /** 链接目标 URL（可能经过 URL 编码） */
        private String url;
    }

    /**
     * mention_doc 内联元素：引用飞书文档
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MentionDoc {
        /** 文档 token */
        private String token;

        /** 文档标题 */
        private String title;

        /** 文档 URL */
        private String url;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TextStyle {
        private Integer align;
    }
}
