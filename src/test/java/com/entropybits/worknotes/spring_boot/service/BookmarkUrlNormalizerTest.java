/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BookmarkUrlNormalizerTest {

    private final BookmarkUrlNormalizer normalizer = new BookmarkUrlNormalizer();

    @Test
    void normalize_lowercasesHost() {
        assertThat(normalizer.normalize("https://EXAMPLE.com/path"))
                .isEqualTo("https://example.com/path");
    }

    @Test
    void normalize_unifiesHttpToHttps() {
        assertThat(normalizer.normalize("http://example.com/path"))
                .isEqualTo(normalizer.normalize("https://example.com/path"));
    }

    @Test
    void normalize_stripsTrailingSlash() {
        assertThat(normalizer.normalize("https://example.com/path/"))
                .isEqualTo(normalizer.normalize("https://example.com/path"));
    }

    @Test
    void normalize_stripsTrackingParams() {
        assertThat(normalizer.normalize("https://example.com/path?utm_source=x&id=5&fbclid=abc"))
                .isEqualTo("https://example.com/path?id=5");
    }

    @Test
    void normalize_removesQueryStringEntirelyWhenOnlyTrackingParams() {
        assertThat(normalizer.normalize("https://example.com/path?utm_source=x&gclid=y"))
                .isEqualTo("https://example.com/path");
    }

    @Test
    void normalize_returnsOriginalTrimmedOnMalformedUrl() {
        assertThat(normalizer.normalize("not a url")).isEqualTo("not a url");
    }
}
