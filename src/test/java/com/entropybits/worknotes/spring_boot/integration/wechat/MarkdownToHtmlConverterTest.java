/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.wechat;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownToHtmlConverterTest {

    private final MarkdownToHtmlConverter converter = new MarkdownToHtmlConverter();

    @Test
    void convertsHeadingAndParagraph() {
        String html = converter.convert("# Hello\n\nWorld");
        assertThat(html).contains("<h1>Hello</h1>").contains("<p>World</p>");
    }

    @Test
    void convertsImageReference() {
        String html = converter.convert("![alt](/uploads/worknotesimage/public/notes/a.png)");
        assertThat(html).contains("<img src=\"/uploads/worknotesimage/public/notes/a.png\"");
    }

    @Test
    void returnsEmptyStringForBlankInput() {
        assertThat(converter.convert(null)).isEmpty();
        assertThat(converter.convert("  ")).isEmpty();
    }
}
