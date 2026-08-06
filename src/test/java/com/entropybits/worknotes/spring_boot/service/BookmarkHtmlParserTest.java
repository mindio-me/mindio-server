/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.service;

import com.entropybits.worknotes.spring_boot.entity.ImportItem;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BookmarkHtmlParserTest {

    private final BookmarkHtmlParser parser = new BookmarkHtmlParser();

    private String loadFixture() throws IOException {
        return Files.readString(
                Path.of("src/test/resources/fixtures/sample-bookmarks.html"), StandardCharsets.UTF_8);
    }

    @Test
    void parse_extractsAllAnchorsWithTitleAndUrl() throws IOException {
        List<ParsedBookmark> result = parser.parse(loadFixture());

        assertThat(result).hasSize(5);
        assertThat(result).extracting(ParsedBookmark::title)
                .contains("Example Post", "PHP 手册");
        assertThat(result).extracting(ParsedBookmark::rawUrl)
                .contains("https://example.com/post/1");
    }

    @Test
    void parse_extractsAddDateAsUtcLocalDateTime() throws IOException {
        List<ParsedBookmark> result = parser.parse(loadFixture());

        ParsedBookmark examplePost = result.stream()
                .filter(b -> b.rawUrl().equals("https://example.com/post/1"))
                .findFirst().orElseThrow();

        assertThat(examplePost.addedAt())
                .isEqualTo(java.time.LocalDateTime.ofEpochSecond(1503358931L, 0, ZoneOffset.UTC));
    }

    @Test
    void parse_resolvesTopLevelFolderPath() throws IOException {
        List<ParsedBookmark> result = parser.parse(loadFixture());

        ParsedBookmark examplePost = result.stream()
                .filter(b -> b.rawUrl().equals("https://example.com/post/1"))
                .findFirst().orElseThrow();

        assertThat(examplePost.folderPath()).isEqualTo("书签栏");
    }

    @Test
    void parse_resolvesNestedFolderPath() throws IOException {
        List<ParsedBookmark> result = parser.parse(loadFixture());

        ParsedBookmark phpManual = result.stream()
                .filter(b -> b.rawUrl().equals("https://php.net/manual/zh/index.php"))
                .findFirst().orElseThrow();

        assertThat(phpManual.folderPath()).isEqualTo("书签栏/工具");
    }

    @Test
    void isNoise_flagsJavascriptScheme() {
        assertThat(parser.isNoise("javascript:void(0)")).isEqualTo(ImportItem.NoiseReason.JS_BOOKMARKLET);
    }

    @Test
    void isNoise_flagsNonHttpScheme() {
        assertThat(parser.isNoise("chrome://bookmarks/")).isEqualTo(ImportItem.NoiseReason.INTERNAL_SCHEME);
    }

    @Test
    void isNoise_returnsNullForHttpUrl() {
        assertThat(parser.isNoise("https://example.com")).isNull();
    }
}
