/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.feishu.converter;

import com.entropybits.worknotes.spring_boot.integration.feishu.client.model.FeishuDocxBlocksResponse.BlockItem;
import com.entropybits.worknotes.spring_boot.integration.feishu.client.model.FeishuDocxBlocksResponse.TextElement;
import com.entropybits.worknotes.spring_boot.integration.feishu.client.model.FeishuDocxBlocksResponse.TextRun;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 飞书 Blocks 到 Markdown 转换器
 * <p>
 * 将飞书文档的 blocks 结构转换为 markdown 格式文本。
 * 支持标题、列表、代码块、引用等常见块类型。
 */
@Slf4j
@Component
public class FeishuBlocksToMarkdownConverter {

    /**
     * 将 blocks 列表转换为 markdown 文本
     *
     * @param blocks 飞书文档块列表
     * @return markdown 格式文本
     */
    public String convert(List<BlockItem> blocks) {

        log.debug("Run into function convert()");
        log.debug("Converting {} blocks", blocks.size());

        if (blocks == null || blocks.isEmpty()) {
            log.debug("Empty blocks list, returning empty string");
            return "";
        }

        log.debug("Converting {} blocks to markdown", blocks.size());
        StringBuilder markdown = new StringBuilder();

        for (BlockItem block : blocks) {
            String line = convertBlock(block);
            if (line != null && !line.isEmpty()) {
                markdown.append(line).append("\n");
            }
        }

        String result = markdown.toString();
        log.debug("Conversion complete: {} blocks -> {} chars", blocks.size(), result.length());
        return result;
    }

    /**
     * 转换单个 block 为 markdown 行
     *
     * @param block 飞书文档块
     * @return markdown 行文本
     */
    private String convertBlock(BlockItem block) {

        log.debug("Run into function convertBlock()");

        if (block == null) {
            return "";
        }

        Integer blockType = block.getBlockType();
        if (blockType == null) {
            log.warn("Block has null blockType, skipping: blockId={}", block.getBlockId());
            return "";
        }

        // 优先处理不需要文本内容的块类型（如分隔线、图片等）
        switch (blockType) {
            case 22: // 分割线 Block
                return "---";

            case 27: // 图片 Block
                return convertImageBlock(block);

            case 28: // 开放平台小组件 Block
                log.debug("Widget block not supported, skipping: {}", block.getBlockId());
                return "";

            case 29: // 思维笔记 Block
                log.debug("Mindnote block not supported, skipping: {}", block.getBlockId());
                return "";

            case 30: // 电子表格 Block
                log.debug("Spreadsheet block not supported, outputting placeholder: {}", block.getBlockId());
                return "*[电子表格]*";
        }

        // 提取文本内容（其余块类型都需要文本内容）
        String content = extractContent(block);
        if (content == null || content.isEmpty()) {
            log.trace("Block has no text content: blockId={}, type={}", block.getBlockId(), blockType);
            return "";
        }

        // 根据 blockType 转换（需要文本内容的块类型）
        switch (blockType) {
            case 1:  // 页面 Block
                log.trace("Skipping page block: {}", block.getBlockId());
                return "";

            case 2:  // 文本 Block
                return content;

            case 3:  // 标题 1 Block
                return "# " + content;

            case 4:  // 标题 2 Block
                return "## " + content;

            case 5:  // 标题 3 Block
                return "### " + content;

            case 6:  // 标题 4 Block
                return "#### " + content;

            case 7:  // 标题 5 Block
                return "##### " + content;

            case 8:  // 标题 6 Block
                return "###### " + content;

            case 9:  // 标题 7 Block
                return "###### " + content;  // Markdown 最多支持 6 级标题

            case 10: // 标题 8 Block
                return "###### " + content;

            case 11: // 标题 9 Block
                return "###### " + content;

            case 12: // 无序列表 Block
                return "- " + content;

            case 13: // 有序列表 Block
                return "1. " + content;

            case 14: // 代码块 Block
                return "```\n" + content + "\n```";

            case 15: // 引用 Block
                return "> " + content;

            case 17: // 待办事项 Block（注意：官方定义中无 16）
                return "- [ ] " + content;

            case 18: // 多维表格 Block
                log.debug("Bitable block not fully supported, outputting as text: {}", block.getBlockId());
                return content;

            case 19: // 高亮块 Block
                log.debug("Callout block, outputting as quoted text: {}", block.getBlockId());
                return "> " + content;  // 高亮块转换为引用格式

            case 20: // 会话卡片 Block
                log.debug("Chat card block not supported, skipping: {}", block.getBlockId());
                return "";

            case 21: // 流程图 & UML Block
                log.debug("Diagram block not supported, outputting placeholder: {}", block.getBlockId());
                return "*[流程图/UML]*";

            case 23: // 文件 Block
                return "*[附件: " + content + "]*";

            case 24: // 分栏 Block
                log.debug("Column block not supported, skipping: {}", block.getBlockId());
                return "";

            case 25: // 分栏列 Block
                log.debug("Column item block not supported, skipping: {}", block.getBlockId());
                return "";

            case 26: // 内嵌网页 Block
                log.debug("Embedded web page not supported, outputting placeholder: {}", block.getBlockId());
                return "*[内嵌网页: " + content + "]*";

            case 31: // 表格 Block
                log.debug("Table block not fully supported, outputting as text: {}", block.getBlockId());
                return content;

            case 32: // 表格单元格 Block
                log.debug("Table cell block, outputting as text: {}", block.getBlockId());
                return content;

            case 33: // 视图 Block
                log.debug("View block not supported, skipping: {}", block.getBlockId());
                return "";

            case 34: // 引用容器 Block
                log.debug("Quote container block, outputting as quoted text: {}", block.getBlockId());
                return "> " + content;

            case 35: // 任务 Block
                log.debug("Task block, outputting as todo: {}", block.getBlockId());
                return "- [ ] " + content;

            case 36: // OKR Block
            case 37: // OKR Objective Block
            case 38: // OKR Key Result Block
            case 39: // OKR 进展 Block
                log.debug("OKR block not fully supported, outputting as text: {}", block.getBlockId());
                return content;

            case 40: // 文档小组件 Block
                log.debug("Doc widget block not supported, skipping: {}", block.getBlockId());
                return "";

            case 41: // Jira 问题 Block
                log.debug("Jira issue block not supported, outputting as text: {}", block.getBlockId());
                return "*[Jira: " + content + "]*";

            case 42: // Wiki 子目录 Block
                log.debug("Wiki subdirectory block not supported, skipping: {}", block.getBlockId());
                return "";

            case 43: // 画板 Block
                log.debug("Board block not supported, outputting placeholder: {}", block.getBlockId());
                return "*[画板]*";

            case 44: // 议程 Block
            case 45: // 议程项 Block
            case 46: // 议程项标题 Block
            case 47: // 议程项内容 Block
                log.debug("Agenda block, outputting as text: {}", block.getBlockId());
                return content;

            case 48: // 链接预览 Block
                log.debug("Link preview block, outputting as text: {}", block.getBlockId());
                return content;

            case 49: // 源同步块（仅支持查询）
            case 50: // 引用同步块（仅支持查询）
                log.debug("Sync block (read-only), outputting as text: {}", block.getBlockId());
                return content;

            case 51: // Wiki 新版子目录
                log.debug("New wiki subdirectory block not supported, skipping: {}", block.getBlockId());
                return "";

            case 52: // AI 模板 Block（仅支持查询）
                log.debug("AI template block (read-only), outputting as text: {}", block.getBlockId());
                return content;

            case 999: // 未支持 Block
                log.warn("Unsupported block type 999: blockId={}", block.getBlockId());
                return content;

            default:
                log.warn("Unknown block type: {}, blockId={}, treating as text", blockType, block.getBlockId());
                return content;
        }
    }

    /**
     * 从 block 中提取文本内容
     * <p>
     * 支持富文本元素（text_run），并处理样式（粗体、斜体、行内代码等）。
     *
     * @param block 飞书文档块
     * @return 提取的文本内容
     */
    private String extractContent(BlockItem block) {
        TextRun textRun = block.resolveTextRun();
        if (textRun == null) {
            return "";
        }

        // 如果有 elements（富文本结构），优先使用
        if (textRun.getElements() != null && !textRun.getElements().isEmpty()) {
            return extractRichText(textRun.getElements());
        }

        // 否则使用简单的 content 字段
        String content = textRun.getContent();
        return content != null ? content : "";
    }

    /**
     * 从富文本元素中提取内容并应用样式
     *
     * @param elements 文本元素列表
     * @return 格式化后的文本
     */
    private String extractRichText(List<TextElement> elements) {
        StringBuilder result = new StringBuilder();

        for (TextElement element : elements) {
            // 处理 mention_doc 内联元素（引用飞书文档）
            if (element.getMentionDoc() != null) {
                var doc = element.getMentionDoc();
                String title = doc.getTitle() != null ? doc.getTitle() : "文档";
                String url = doc.getUrl();
                if (url != null && !url.isBlank()) {
                    result.append("[").append(title).append("](").append(url).append(")");
                } else {
                    result.append(title);
                }
                continue;
            }

            if (element.getTextRun() == null || element.getTextRun().getContent() == null) {
                continue;
            }

            String text = element.getTextRun().getContent();
            var style = element.getTextRun().getTextElementStyle();

            // 应用样式
            if (style != null) {
                // 行内代码优先级最高（不叠加其他样式）
                if (Boolean.TRUE.equals(style.getInline_code())) {
                    text = "`" + text + "`";
                } else {
                    // 粗体
                    if (Boolean.TRUE.equals(style.getBold())) {
                        text = "**" + text + "**";
                    }
                    // 斜体
                    if (Boolean.TRUE.equals(style.getItalic())) {
                        text = "*" + text + "*";
                    }
                    // 删除线
                    if (Boolean.TRUE.equals(style.getStrikethrough())) {
                        text = "~~" + text + "~~";
                    }
                    // 下划线（Markdown 原生不支持，使用 HTML 标签）
                    if (Boolean.TRUE.equals(style.getUnderline())) {
                        text = "<u>" + text + "</u>";
                    }
                    // 超链接：转换为 [text](url) 格式
                    if (style.getLink() != null && style.getLink().getUrl() != null
                            && !style.getLink().getUrl().isBlank()) {
                        try {
                            String decodedUrl = java.net.URLDecoder.decode(
                                    style.getLink().getUrl(), java.nio.charset.StandardCharsets.UTF_8);
                            text = "[" + text + "](" + decodedUrl + ")";
                        } catch (Exception e) {
                            text = "[" + text + "](" + style.getLink().getUrl() + ")";
                        }
                    }
                }
            }

            result.append(text);
        }

        return result.toString();
    }

    /**
     * 转换图片块为 markdown
     * <p>
     * 飞书图片块（block_type=27）包含图片的 file_token。
     * 由于图片需要通过飞书API下载（需要access_token），这里使用特殊的URL格式标记图片token。
     * <p>
     * 未来可以扩展：
     * - 前端自动下载飞书图片并上传到自己的存储
     * - 后端在导入时批量下载图片
     *
     * @param block 图片块
     * @return markdown 图片语法
     */
    private String convertImageBlock(BlockItem block) {

        log.debug("Run into function convertImageBlock()");
        log.debug("Converting image block: blockId={}", block.getBlockId());
        
        if (block.getImage() == null) {
            log.warn("Image block has no image data: blockId={}", block.getBlockId());
            return "*[图片加载失败]*";
        }
        log.debug("Image data: {}", block.getImage().toString());
        
        String token = block.getImage().getToken();
        if (token == null || token.isEmpty()) {
            log.warn("Image block has no token: blockId={}", block.getBlockId());
            return "*[图片token为空]*";
        }

        // 使用特殊的URL scheme标记飞书图片
        // 格式: feishu-image:{token}
        // 前端可以识别这个格式，自动调用API下载图片
        String imageUrl = "feishu-image:" + token;

        // 可选：添加尺寸信息到alt文本
        String altText = "image";
        if (block.getImage().getWidth() != null && block.getImage().getHeight() != null) {
            altText = String.format("image (%dx%d)",
                    block.getImage().getWidth(),
                    block.getImage().getHeight());
        }

        log.debug("Converted image block: blockId={}, token={}, size={}x{}",
                block.getBlockId(), token,
                block.getImage().getWidth(), block.getImage().getHeight());

        return "![" + altText + "](" + imageUrl + ")";
    }
}
