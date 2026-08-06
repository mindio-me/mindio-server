/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.ai.dto;

import lombok.Data;

@Data
public class TranslationRequest {
    /** FAITHFUL = 忠实逐块翻译; LINKEDIN = LinkedIn 风格改写 */
    private String mode = "FAITHFUL";
    private String targetLanguage = "en";
}
