/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.ai.service;

import com.entropybits.worknotes.spring_boot.ai.config.AiProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 通用 OpenAI Chat Completions 兼容实现。
 * 支持 OpenAI、DeepSeek、豆包（火山引擎）等兼容 OpenAI 格式的服务商。
 */
@Slf4j
public class OpenAiCompatibleTranslationService implements AiTranslationService {

    private final AiProperties.ProviderConfig config;
    private final String providerName;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build();

    public OpenAiCompatibleTranslationService(AiProperties.ProviderConfig config,
                                               String providerName,
                                               ObjectMapper objectMapper) {
        this.config = config;
        this.providerName = providerName;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<String> translateTexts(List<String> texts, String targetLanguage) throws Exception {
        if (texts.isEmpty()) return List.of();

        String inputJson = objectMapper.writeValueAsString(texts);
        String prompt = "Translate each element of the following JSON string array from Chinese to " + targetLanguage + ".\n"
            + "Return ONLY a valid JSON array with exactly " + texts.size() + " string elements, in the same order.\n"
            + "No explanation, no markdown, no extra text outside the JSON array.\n"
            + inputJson;

        String result = callApi(prompt).trim();

        // Strip markdown code fences if model wrapped the JSON
        if (result.startsWith("```")) {
            result = result.replaceAll("(?s)^```[a-z]*\\n?", "").replaceAll("```$", "").trim();
        }

        List<String> translated = objectMapper.readValue(result, new TypeReference<List<String>>() {});

        // Pad or truncate to match input size as safety net
        if (translated.size() < texts.size()) {
            for (int i = translated.size(); i < texts.size(); i++) translated.add(texts.get(i));
        } else if (translated.size() > texts.size()) {
            translated = translated.subList(0, texts.size());
        }
        return translated;
    }

    @Override
    public String rewriteForLinkedIn(String title, String bodyText) throws Exception {
        String prompt = "Based on the following Chinese article, write an engaging LinkedIn post in English.\n"
            + "Requirements: strong opening hook, professional yet personal tone, short paragraphs,\n"
            + "key insights highlighted, end with a question or call-to-action. Length: 200-400 words.\n"
            + "Return only the LinkedIn post text, no title.\n\n"
            + "Article title: " + title + "\n\n"
            + "Article content:\n" + bodyText;

        return callApi(prompt).trim();
    }

    @Override
    public List<String> generateLinkedInHashtags(String title, String bodyText) throws Exception {
        String prompt = "Based on the following English article, generate 3 to 5 relevant LinkedIn hashtags.\n"
            + "Return ONLY a valid JSON array of strings without the '#' symbol.\n"
            + "Example: [\"AI\",\"SaaS\",\"Productivity\"]\n"
            + "No explanation, no markdown, no extra text outside the JSON array.\n\n"
            + "Title: " + title + "\n\n"
            + "Content:\n" + bodyText;

        String result = callApi(prompt).trim();
        if (result.startsWith("```")) {
            result = result.replaceAll("(?s)^```[a-z]*\\n?", "").replaceAll("```$", "").trim();
        }
        List<String> tags = objectMapper.readValue(result, new TypeReference<List<String>>() {});
        if (tags.size() > 5) tags = tags.subList(0, 5);
        return tags;
    }

    @Override
    public List<List<String>> classifyTopics(List<String> titles) throws Exception {
        if (titles.isEmpty()) return List.of();

        String inputJson = objectMapper.writeValueAsString(titles);
        String prompt = "For each title in the following JSON string array (bookmark/webpage titles, mostly Chinese), "
            + "suggest 2 to 4 short topic keywords (2-6 Chinese characters each) that categorize it.\n"
            + "Return ONLY a valid JSON array with exactly " + titles.size() + " elements, in the same order, "
            + "where each element is itself a JSON array of topic keyword strings.\n"
            + "Example: [[\"人工智能\",\"编程\"],[\"旅行\"]]\n"
            + "No explanation, no markdown, no extra text outside the JSON array.\n"
            + inputJson;

        String result = callApi(prompt).trim();
        if (result.startsWith("```")) {
            result = result.replaceAll("(?s)^```[a-z]*\\n?", "").replaceAll("```$", "").trim();
        }

        List<List<String>> topics = objectMapper.readValue(result, new TypeReference<List<List<String>>>() {});
        if (topics.size() < titles.size()) {
            for (int i = topics.size(); i < titles.size(); i++) topics.add(List.of());
        } else if (topics.size() > titles.size()) {
            topics = topics.subList(0, titles.size());
        }
        return topics;
    }

    @Override
    public String summarizeCluster(String topicLabel, List<String> titlesInCluster) throws Exception {
        String inputJson = objectMapper.writeValueAsString(titlesInCluster);
        String prompt = "The following is a JSON array of webpage/bookmark titles that all belong to the topic \""
            + topicLabel + "\".\n"
            + "Write ONE summary sentence in Chinese describing what this group of bookmarks is about.\n"
            + "Return ONLY that single sentence, no bullet list, no markdown formatting, no extra commentary.\n\n"
            + inputJson;

        return callApi(prompt).trim();
    }

    @Override
    public String summarizeTimelineBucket(String bucketLabel, List<String> titlesInBucket) throws Exception {
        String inputJson = objectMapper.writeValueAsString(titlesInBucket);
        String prompt = "The following is a JSON array of webpage/bookmark titles collected during \""
            + bucketLabel + "\".\n"
            + "Write one short narrative paragraph in Chinese (3-5 sentences) describing what the person seemed "
            + "to be interested in or working on during this period, based on these titles.\n"
            + "Return ONLY the paragraph text, no markdown, no code fences, no extra commentary.\n\n"
            + inputJson;

        return callApi(prompt).trim();
    }

    private String callApi(String prompt) throws Exception {
        String url = config.getBaseUrl() + config.getChatPath();

        Map<String, Object> body = Map.of(
            "model", config.getModel(),
            "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        String json = objectMapper.writeValueAsString(body);
        log.debug("{} request: model={}", providerName, config.getModel());

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + config.getApiKey())
            .header("content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
            .timeout(Duration.ofSeconds(120))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        log.debug("{} response status={}", providerName, response.statusCode());

        if (response.statusCode() >= 400) {
            throw new RuntimeException(providerName + " API error: HTTP " + response.statusCode() + " " + response.body());
        }

        Map<String, Object> parsed = objectMapper.readValue(response.body(), new TypeReference<>() {});
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choices = (List<Map<String, Object>>) parsed.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException(providerName + " API returned empty choices");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }
}
