/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.ai.config;

import com.entropybits.worknotes.spring_boot.ai.service.AiTranslationService;
import com.entropybits.worknotes.spring_boot.ai.service.OpenAiCompatibleTranslationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiTranslationConfig {

    @Bean("openAiTranslationService")
    AiTranslationService openAiTranslationService(AiProperties props, ObjectMapper objectMapper) {
        return new OpenAiCompatibleTranslationService(props.getOpenai(), "OpenAI", objectMapper);
    }

    @Bean("deepseekTranslationService")
    AiTranslationService deepseekTranslationService(AiProperties props, ObjectMapper objectMapper) {
        return new OpenAiCompatibleTranslationService(props.getDeepseek(), "DeepSeek", objectMapper);
    }

    @Bean("doubaoTranslationService")
    AiTranslationService doubaoTranslationService(AiProperties props, ObjectMapper objectMapper) {
        return new OpenAiCompatibleTranslationService(props.getDoubao(), "Doubao", objectMapper);
    }
}
