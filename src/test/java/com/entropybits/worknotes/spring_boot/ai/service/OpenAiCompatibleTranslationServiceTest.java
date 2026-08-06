/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.ai.service;

import com.entropybits.worknotes.spring_boot.ai.config.AiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiCompatibleTranslationServiceTest {

    private HttpServer httpServer;
    private OpenAiCompatibleTranslationService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private void setUp(String modelReplyContent) throws Exception {
        httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext("/v1/chat/completions", ex -> {
            String body = "{\"choices\":[{\"message\":{\"content\":"
                + objectMapper.writeValueAsString(modelReplyContent) + "}}]}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(200, bytes.length);
            ex.getResponseBody().write(bytes);
            ex.close();
        });
        httpServer.start();

        AiProperties.ProviderConfig config = new AiProperties.ProviderConfig();
        config.setApiKey("test-key");
        config.setModel("test-model");
        config.setBaseUrl("http://127.0.0.1:" + httpServer.getAddress().getPort());
        config.setChatPath("/v1/chat/completions");

        service = new OpenAiCompatibleTranslationService(config, "TestProvider", objectMapper);
    }

    @AfterEach
    void tearDown() {
        if (httpServer != null) httpServer.stop(0);
    }

    @Test
    void classifyTopics_parsesNestedJsonArrayResponseInOrder() throws Exception {
        setUp("[[\"AI\",\"编程\"],[\"旅行\"]]");

        List<List<String>> result = service.classifyTopics(List.of("标题一", "标题二"));

        assertThat(result).containsExactly(List.of("AI", "编程"), List.of("旅行"));
    }

    @Test
    void classifyTopics_padsMissingEntriesWithEmptyListWhenModelReturnsFewer() throws Exception {
        setUp("[[\"AI\"]]");

        List<List<String>> result = service.classifyTopics(List.of("标题一", "标题二"));

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).containsExactly("AI");
        assertThat(result.get(1)).isEmpty();
    }

    @Test
    void summarizeCluster_returnsRawMarkdownText() throws Exception {
        setUp("这是一组关于 AI 的收藏。\n- 标题一\n- 标题二");

        String result = service.summarizeCluster("AI", List.of("标题一", "标题二"));

        assertThat(result).isEqualTo("这是一组关于 AI 的收藏。\n- 标题一\n- 标题二");
    }

    @Test
    void summarizeTimelineBucket_returnsRawParagraphText() throws Exception {
        setUp("这一年主要收藏了关于人工智能的内容。");

        String result = service.summarizeTimelineBucket("2023 年", List.of("标题一"));

        assertThat(result).isEqualTo("这一年主要收藏了关于人工智能的内容。");
    }
}
