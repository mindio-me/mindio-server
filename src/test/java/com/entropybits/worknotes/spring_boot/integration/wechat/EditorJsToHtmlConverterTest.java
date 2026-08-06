/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.wechat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class EditorJsToHtmlConverterTest {

    private EditorJsToHtmlConverter converter;

    @BeforeEach
    void setUp() {
        converter = new EditorJsToHtmlConverter(new ObjectMapper());
    }

    @Test
    void convertsHeaderBlock() throws Exception {
        String json = "{\"blocks\":[{\"type\":\"header\",\"data\":{\"text\":\"Hello World\",\"level\":2}}]}";
        String html = converter.convert(json, Map.of());
        assertThat(html).contains("Hello World").contains("font-weight:bold").contains("<h2");
    }

    @Test
    void convertsParagraphBlock() throws Exception {
        String json = "{\"blocks\":[{\"type\":\"paragraph\",\"data\":{\"text\":\"Some text\"}}]}";
        String html = converter.convert(json, Map.of());
        assertThat(html).contains("<p ").contains("Some text").contains("line-height:1.8");
    }

    @Test
    void replacesImageUrlWithMappedWechatUrl() throws Exception {
        String localUrl = "http://localhost:8080/api/uploads/img.jpg";
        String wechatUrl = "https://mmbiz.qpic.cn/abc123";
        String json = "{\"blocks\":[{\"type\":\"image\",\"data\":{\"file\":{\"url\":\""
            + localUrl + "\"},\"caption\":\"\"}}]}";
        String html = converter.convert(json, Map.of(localUrl, wechatUrl));
        assertThat(html).contains(wechatUrl).doesNotContain(localUrl);
    }

    @Test
    void convertsCodeBlock() throws Exception {
        String json = "{\"blocks\":[{\"type\":\"code\",\"data\":{\"code\":\"int x = 1;\"}}]}";
        String html = converter.convert(json, Map.of());
        assertThat(html).contains("int x = 1;").contains("background:#1e1e1e").contains("<pre ");
    }

    @Test
    void convertsQuoteBlock() throws Exception {
        String json = "{\"blocks\":[{\"type\":\"quote\",\"data\":{\"text\":\"A wise quote\",\"caption\":\"Author\"}}]}";
        String html = converter.convert(json, Map.of());
        assertThat(html).contains("A wise quote").contains("border-left:4px solid").contains("— Author");
    }

    @Test
    void convertsUnorderedList() throws Exception {
        String json = "{\"blocks\":[{\"type\":\"list\",\"data\":{\"style\":\"unordered\",\"items\":[\"Item 1\",\"Item 2\"]}}]}";
        String html = converter.convert(json, Map.of());
        assertThat(html).contains("<ul ").contains("Item 1").contains("Item 2");
    }

    @Test
    void convertsDelimiter() throws Exception {
        String json = "{\"blocks\":[{\"type\":\"delimiter\",\"data\":{}}]}";
        String html = converter.convert(json, Map.of());
        assertThat(html).contains("* * *");
    }

    @Test
    void convertsTableWithHeadings() throws Exception {
        String json = "{\"blocks\":[{\"type\":\"table\",\"data\":{\"withHeadings\":true," +
            "\"content\":[[\"H1\",\"H2\"],[\"R1C1\",\"R1C2\"]]}}]}";
        String html = converter.convert(json, Map.of());
        assertThat(html).contains("<table ").contains("<th ").contains("<td ").contains("H1");
    }

    @Test
    void extractsImageUrlsFromBlocks() throws Exception {
        String json = "{\"blocks\":["
            + "{\"type\":\"image\",\"data\":{\"file\":{\"url\":\"http://localhost/img1.jpg\"},\"caption\":\"\"}},"
            + "{\"type\":\"paragraph\",\"data\":{\"text\":\"text\"}},"
            + "{\"type\":\"image\",\"data\":{\"file\":{\"url\":\"http://localhost/img2.jpg\"},\"caption\":\"\"}}"
            + "]}";
        List<String> urls = converter.extractImageUrls(json);
        assertThat(urls).containsExactly("http://localhost/img1.jpg", "http://localhost/img2.jpg");
    }

    @Test
    void extractsPlainText() throws Exception {
        String json = "{\"blocks\":["
            + "{\"type\":\"header\",\"data\":{\"text\":\"Title\",\"level\":2}},"
            + "{\"type\":\"paragraph\",\"data\":{\"text\":\"Hello world\"}}"
            + "]}";
        String text = converter.extractPlainText(json, 200);
        assertThat(text).contains("Title").contains("Hello world");
    }

    @Test
    void returnsEmptyForNullOrBlankInput() throws Exception {
        assertThat(converter.convert("", Map.of())).isEmpty();
        assertThat(converter.convert(null, Map.of())).isEmpty();
        assertThat(converter.extractImageUrls(null)).isEmpty();
    }

    @Test
    void convertsMarkdownBlockWithLink() throws Exception {
        String json = "{\"blocks\":[{\"type\":\"markdown\",\"data\":"
            + "{\"markdown\":\"Visit [example](https://example.com) for info\",\"mode\":\"split\"}}]}";
        String html = converter.convert(json, Map.of());
        assertThat(html).contains("<a href=\"https://example.com\">example</a>");
        assertThat(html).contains("Visit");
        assertThat(html).doesNotContain("[example](https://example.com)");
    }

    @Test
    void convertsMarkdownBlockBoldAndItalic() throws Exception {
        String json = "{\"blocks\":[{\"type\":\"markdown\",\"data\":"
            + "{\"markdown\":\"**bold** and *italic*\",\"mode\":\"edit\"}}]}";
        String html = converter.convert(json, Map.of());
        assertThat(html).contains("<strong>bold</strong>");
        assertThat(html).contains("<em>italic</em>");
    }

    @Test
    void convertsMarkdownBlockHeadings() throws Exception {
        String json = "{\"blocks\":[{\"type\":\"markdown\",\"data\":"
            + "{\"markdown\":\"# H1\\n## H2\",\"mode\":\"preview\"}}]}";
        String html = converter.convert(json, Map.of());
        assertThat(html).contains("<h1 ").contains("H1");
        assertThat(html).contains("<h2 ").contains("H2");
    }

    @Test
    void convertsMarkdownBlockUnorderedList() throws Exception {
        String json = "{\"blocks\":[{\"type\":\"markdown\",\"data\":"
            + "{\"markdown\":\"- Item A\\n- Item B\",\"mode\":\"split\"}}]}";
        String html = converter.convert(json, Map.of());
        assertThat(html).contains("<li ").contains("Item A").contains("Item B");
        assertThat(html).contains("<ul ");
    }

    @Test
    void emptyMarkdownBlockReturnsEmpty() throws Exception {
        String json = "{\"blocks\":[{\"type\":\"markdown\",\"data\":"
            + "{\"markdown\":\"  \",\"mode\":\"split\"}}]}";
        String html = converter.convert(json, Map.of());
        assertThat(html).isEmpty();
    }
}
