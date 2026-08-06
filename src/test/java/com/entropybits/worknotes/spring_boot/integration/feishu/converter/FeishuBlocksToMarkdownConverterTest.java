/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.feishu.converter;

import com.entropybits.worknotes.spring_boot.integration.feishu.client.model.FeishuDocxBlocksResponse;
import com.entropybits.worknotes.spring_boot.integration.feishu.client.model.FeishuDocxBlocksResponse.BlockItem;
import com.entropybits.worknotes.spring_boot.integration.feishu.client.model.FeishuDocxBlocksResponse.TextContent;
import com.entropybits.worknotes.spring_boot.integration.feishu.client.model.FeishuDocxBlocksResponse.TextElement;
import com.entropybits.worknotes.spring_boot.integration.feishu.client.model.FeishuDocxBlocksResponse.TextRun;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FeishuBlocksToMarkdownConverterTest {

    private final FeishuBlocksToMarkdownConverter converter = new FeishuBlocksToMarkdownConverter();

    private static TextRun textRunOf(String content) {
        TextContent textContent = new TextContent();
        textContent.setContent(content);
        TextElement element = new TextElement();
        element.setTextRun(textContent);
        TextRun run = new TextRun();
        run.setElements(List.of(element));
        return run;
    }

    @Test
    void convertsHeading3BlockUsingHeading3Field() {
        // 飞书真实响应里，标题3(block_type=5) 的富文本内容在 "heading3" 字段中，
        // 不在通用的 "text" 字段中 —— text 字段为 null。
        BlockItem block = new BlockItem();
        block.setBlockId("b1");
        block.setBlockType(5);
        block.setHeading3(textRunOf("参数到底是什么？"));

        String markdown = converter.convert(List.of(block));

        assertThat(markdown.trim()).isEqualTo("### 参数到底是什么？");
    }

    @Test
    void convertsBulletBlockUsingBulletField() {
        // 无序列表(block_type=12) 的内容在 "bullet" 字段中，text 字段为 null。
        BlockItem block = new BlockItem();
        block.setBlockId("b2");
        block.setBlockType(12);
        block.setBullet(textRunOf("权重 (Weights)"));

        String markdown = converter.convert(List.of(block));

        assertThat(markdown.trim()).isEqualTo("- 权重 (Weights)");
    }

    @Test
    void convertsTextBlockUsingTextFieldAsBefore() {
        BlockItem block = new BlockItem();
        block.setBlockId("b3");
        block.setBlockType(2);
        block.setText(textRunOf("简单来说，你的理解是非常准确的。"));

        String markdown = converter.convert(List.of(block));

        assertThat(markdown.trim()).isEqualTo("简单来说，你的理解是非常准确的。");
    }

    @Test
    void deserializesRealFeishuBlocksJsonShapeAndPreservesHeadingsAndLists() throws Exception {
        // 真实飞书 Blocks API 响应结构：标题3的内容在 "heading3" 字段中，
        // 无序列表的内容在 "bullet" 字段中，而不是通用的 "text" 字段（该字段为 null）。
        // 见: https://open.feishu.cn/document/server-docs/docs/docs/docx-v1/data-structure/block
        String json = """
                [
                  {
                    "block_id": "h1",
                    "block_type": 5,
                    "text": null,
                    "heading3": {
                      "elements": [
                        {"text_run": {"content": "参数到底是什么？", "text_element_style": {}}}
                      ]
                    }
                  },
                  {
                    "block_id": "l1",
                    "block_type": 12,
                    "text": null,
                    "bullet": {
                      "elements": [
                        {"text_run": {"content": "权重 (Weights)", "text_element_style": {"bold": true}}}
                      ]
                    }
                  },
                  {
                    "block_id": "l2",
                    "block_type": 12,
                    "text": null,
                    "bullet": {
                      "elements": [
                        {"text_run": {"content": "偏置 (Biases)", "text_element_style": {"bold": true}}}
                      ]
                    }
                  }
                ]
                """;

        List<BlockItem> blocks = new ObjectMapper().readValue(json,
                new com.fasterxml.jackson.core.type.TypeReference<List<BlockItem>>() {});

        String markdown = converter.convert(blocks);

        assertThat(markdown)
                .contains("### 参数到底是什么？")
                .contains("- **权重 (Weights)**")
                .contains("- **偏置 (Biases)**");
    }
}
