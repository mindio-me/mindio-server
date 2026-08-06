/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.ai.service;

import java.util.List;

public interface AiTranslationService {

    /**
     * 批量翻译文本片段（忠实翻译）。
     * 返回列表长度与输入相同，顺序一一对应。
     */
    List<String> translateTexts(List<String> texts, String targetLanguage) throws Exception;

    /**
     * 以原文为素材，改写为 LinkedIn 风格英文文章。
     * 返回改写后的正文文本（不含标题）。
     */
    String rewriteForLinkedIn(String title, String bodyText) throws Exception;

    /**
     * Generate 3–5 LinkedIn hashtags for the given English article.
     * Returns tag words without the '#' prefix, e.g. ["AI","SaaS","Dev"].
     */
    List<String> generateLinkedInHashtags(String title, String bodyText) throws Exception;

    /**
     * 给每个标题（书签/网页标题，多为中文）推荐 2~4 个候选主题词，用于聚类分组。
     * 返回列表长度、顺序与输入 titles 一致；每个元素是该标题的候选主题词列表。
     */
    List<List<String>> classifyTopics(List<String> titles) throws Exception;

    /**
     * 给定一个主题下的标题列表，只返回一句话中文归纳（不含链接列表——链接列表由调用方
     * BookmarkAgentService.buildLinkListMarkdown 根据真实 SourceClip 数据确定性拼接）。
     */
    String summarizeCluster(String topicLabel, List<String> titlesInCluster) throws Exception;

    /**
     * 给定一个时间段（如"2023 年"）内的标题列表，返回一段叙述性文字，描述这段时间收藏的内容。
     */
    String summarizeTimelineBucket(String bucketLabel, List<String> titlesInBucket) throws Exception;
}
