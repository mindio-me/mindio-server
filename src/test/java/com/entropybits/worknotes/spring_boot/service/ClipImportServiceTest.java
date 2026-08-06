/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.service;

import com.entropybits.worknotes.spring_boot.config.UploadPathConfig;
import com.entropybits.worknotes.spring_boot.dto.SourceClipDraft;
import com.entropybits.worknotes.spring_boot.entity.SourceClip;
import com.sun.net.httpserver.HttpServer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClipImportServiceTest {

    @Mock UploadPathConfig uploadPathConfig;

    @TempDir Path tempDir;

    private ClipImportService service;
    private HttpServer httpServer;

    @BeforeEach
    void setUp() {
        service = new ClipImportService();
        ReflectionTestUtils.setField(service, "uploadPathConfig", uploadPathConfig);
        when(uploadPathConfig.getUploadPath()).thenReturn(tempDir.toString());
    }

    @AfterEach
    void tearDown() {
        if (httpServer != null) httpServer.stop(0);
    }

    @Test
    void extractContent_genericPage_usesReadabilityForMainArticle() {
        String html = "<html><head><title>Ignored</title></head><body>"
                + "<nav>Home About Contact</nav>"
                + "<article><h1>Real Title</h1><p>"
                + "This is the real article body. ".repeat(20)
                + "</p></article>"
                + "<footer>Copyright 2026</footer>"
                + "</body></html>";
        Document doc = Jsoup.parse(html, "https://example.com/post");

        String content = service.extractContent(doc, false, "https://example.com/post");

        assertThat(content).contains("real article body");
        assertThat(content).doesNotContain("Copyright 2026");
    }

    @Test
    void extractContent_wechatArticle_usesJsContentSelector() {
        String otherContent = "其他无关内容".repeat(50);
        String html = "<html><body>"
                + "<div id=\"js_content\"><p>公众号正文内容</p></div>"
                + "<div>" + otherContent + "</div>"
                + "</body></html>";
        Document doc = Jsoup.parse(html, "https://mp.weixin.qq.com/s/abc");

        String content = service.extractContent(doc, true, "https://mp.weixin.qq.com/s/abc");

        assertThat(content).contains("公众号正文内容");
    }

    @Test
    void extractContent_returnsNull_whenNoSubstantialContent() {
        String html = "<html><body><p>Too short</p></body></html>";
        Document doc = Jsoup.parse(html, "https://example.com/empty");

        String content = service.extractContent(doc, false, "https://example.com/empty");

        assertThat(content).isNull();
    }

    @Test
    void rewriteImages_downloadsImagesLocally_forNonWechatPages() throws IOException {
        byte[] fakeImageBytes = {1, 2, 3, 4};
        httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext("/photo.jpg", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "image/jpeg");
            exchange.sendResponseHeaders(200, fakeImageBytes.length);
            exchange.getResponseBody().write(fakeImageBytes);
            exchange.close();
        });
        httpServer.start();
        int port = httpServer.getAddress().getPort();
        String baseUrl = "http://127.0.0.1:" + port + "/article";
        String html = "<p>text</p><img src=\"/photo.jpg\">";

        String rewritten = service.rewriteImages(html, baseUrl, false);

        assertThat(rewritten).contains("/uploads/");
        assertThat(rewritten).doesNotContain("127.0.0.1:" + port + "/photo.jpg");
    }

    @Test
    void fetchMetadataOnly_doesNotExtractContentOrDownloadImages() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext("/page", exchange -> {
            String html = "<html><head><title>Page Title</title></head>"
                    + "<body><article><p>Body</p><img src=\"/pic.jpg\"></article></body></html>";
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        httpServer.start();
        int port = httpServer.getAddress().getPort();
        String url = "http://127.0.0.1:" + port + "/page";

        SourceClipDraft draft = service.fetchMetadataOnly(url, SourceClip.SourceType.WEBPAGE);

        assertThat(draft.getContent()).isNull();
        assertThat(draft.getSuggestedTitle()).isEqualTo("Page Title");
        assertThat(draft.getExtractionMode()).isEqualTo(SourceClip.ExtractionMode.LINK_ONLY);
        try (var files = Files.list(tempDir)) {
            assertThat(files.findAny()).isEmpty();
        }
    }
}
