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

class AnthropicTranslationServiceTest {

    private HttpServer httpServer;
    private AnthropicTranslationService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private void setUp(String modelReplyText) throws Exception {
        httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext("/v1/messages", ex -> {
            String body = "{\"content\":[{\"text\":" + objectMapper.writeValueAsString(modelReplyText) + "}]}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(200, bytes.length);
            ex.getResponseBody().write(bytes);
            ex.close();
        });
        httpServer.start();

        AiProperties props = new AiProperties();
        props.getAnthropic().setApiKey("test-key");
        props.getAnthropic().setModel("test-model");
        props.getAnthropic().setBaseUrl("http://127.0.0.1:" + httpServer.getAddress().getPort());

        service = new AnthropicTranslationService(props, objectMapper);
    }

    @AfterEach
    void tearDown() {
        if (httpServer != null) httpServer.stop(0);
    }

    @Test
    void classifyTopics_parsesAnthropicResponseEnvelope() throws Exception {
        setUp("[[\"AI\",\"编程\"],[\"旅行\"]]");

        List<List<String>> result = service.classifyTopics(List.of("标题一", "标题二"));

        assertThat(result).containsExactly(List.of("AI", "编程"), List.of("旅行"));
    }
}
