/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NoteServiceTest {

    @Test
    void rewritesSingleUploadUrlPrefix() {
        String content = "![img](http://localhost:8081/api/uploads/worknotesimage/public/a.png)";

        String result = NoteService.rewriteUploadUrls(content, "https://cdn.example.com/api");

        assertThat(result).isEqualTo(
                "![img](https://cdn.example.com/api/uploads/worknotesimage/public/a.png)");
    }

    @Test
    void doesNotSwallowContentBetweenMultipleImageUrls() {
        // 回归用例：飞书导入文档里多张图片之间夹杂着含双引号的文本（如 JSON 示例）时，
        // 旧的贪婪正则 [^"]+ 会把两张图片之间的所有正文当成 URL 的一部分吞掉。
        String content = "![img1](http://localhost:8081/api/uploads/a.png)\n"
                + "这是图片之间的重要正文内容，不应该丢失。\n"
                + "示例 JSON：{msg: \"登录成功\"}\n"
                + "![img2](http://localhost:8081/api/uploads/b.png)\n"
                + "图片之后的内容也不应该丢失。";

        String result = NoteService.rewriteUploadUrls(content, "https://cdn.example.com/api");

        assertThat(result)
                .contains("![img1](https://cdn.example.com/api/uploads/a.png)")
                .contains("![img2](https://cdn.example.com/api/uploads/b.png)")
                .contains("这是图片之间的重要正文内容，不应该丢失。")
                .contains("示例 JSON：{msg: \"登录成功\"}")
                .contains("图片之后的内容也不应该丢失。");
    }

    @Test
    void leavesContentUnchangedWhenNoMatchingUrls() {
        String content = "普通文本，没有需要替换的 URL。";

        String result = NoteService.rewriteUploadUrls(content, "https://cdn.example.com/api");

        assertThat(result).isEqualTo(content);
    }
}
