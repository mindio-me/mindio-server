/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.service;

import com.entropybits.worknotes.spring_boot.ai.config.AiProperties;
import com.entropybits.worknotes.spring_boot.ai.service.AiTranslationService;
import com.entropybits.worknotes.spring_boot.entity.*;
import com.entropybits.worknotes.spring_boot.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookmarkAgentServiceTest {

    @Mock BookmarkAgentJobRepository jobRepository;
    @Mock SourceClipRepository clipRepository;
    @Mock NoteRepository noteRepository;
    @Mock NoteClipRefRepository noteClipRefRepository;
    @Mock UserRepository userRepository;
    @Mock AiProperties aiProperties;
    @Mock AiTranslationService anthropicService;
    @Mock AiTranslationService openAiService;
    @Mock AiTranslationService deepseekService;
    @Mock AiTranslationService doubaoService;

    private BookmarkAgentService service;

    private void setUp() {
        service = new BookmarkAgentService(jobRepository, clipRepository, noteRepository, noteClipRefRepository,
                userRepository, aiProperties, anthropicService, openAiService, deepseekService, doubaoService);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "self", service);
    }

    private SourceClip clip(long id, String title) {
        return SourceClip.builder().id(id).title(title).sourceType(SourceClip.SourceType.WEBPAGE).build();
    }

    @Test
    void partition_splitsListIntoChunksOfGivenSize() {
        setUp();
        List<SourceClip> clips = List.of(clip(1, "a"), clip(2, "b"), clip(3, "c"));

        List<List<SourceClip>> result = BookmarkAgentService.partition(clips, 2);

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).extracting(SourceClip::getId).containsExactly(1L, 2L);
        assertThat(result.get(1)).extracting(SourceClip::getId).containsExactly(3L);
    }

    @Test
    void groupByTopic_groupsClipsByFirstCandidateTopic() {
        setUp();
        SourceClip c1 = clip(1, "标题一");
        SourceClip c2 = clip(2, "标题二");
        SourceClip c3 = clip(3, "标题三");
        List<SourceClip> clips = List.of(c1, c2, c3);
        List<List<String>> topics = List.of(List.of("AI", "编程"), List.of("AI"), List.of("旅行"));

        LinkedHashMap<String, List<SourceClip>> groups = BookmarkAgentService.groupByTopic(clips, topics);

        assertThat(groups.get("AI")).containsExactly(c1, c2);
    }

    @Test
    void groupByTopic_mergesGroupsWithFewerThanTwoMembersIntoOther() {
        setUp();
        SourceClip c1 = clip(1, "标题一");
        SourceClip c2 = clip(2, "标题二");
        SourceClip c3 = clip(3, "标题三");
        SourceClip c4 = clip(4, "标题四");
        // "AI" 有 2 条；"旅行"、"美食" 各只有 1 条 —— 两个 singleton 分组都应该并入同一个"其他"
        List<SourceClip> clips = List.of(c1, c2, c3, c4);
        List<List<String>> topics = List.of(List.of("AI"), List.of("AI"), List.of("旅行"), List.of("美食"));

        LinkedHashMap<String, List<SourceClip>> groups = BookmarkAgentService.groupByTopic(clips, topics);

        assertThat(groups).containsKey("AI");
        assertThat(groups).doesNotContainKey("旅行");
        assertThat(groups).doesNotContainKey("美食");
        assertThat(groups.get("其他")).containsExactlyInAnyOrder(c3, c4);
    }

    @Test
    void groupByTopic_treatsClipWithNoTopicsAsOther() {
        setUp();
        SourceClip c1 = clip(1, "标题一");
        List<SourceClip> clips = List.of(c1);
        List<List<String>> topics = List.of(List.of());

        LinkedHashMap<String, List<SourceClip>> groups = BookmarkAgentService.groupByTopic(clips, topics);

        assertThat(groups.get("其他")).containsExactly(c1);
    }

    @Test
    void groupByYear_bucketsClipsByOriginalBookmarkedAtYearAscending() {
        setUp();
        SourceClip c2019 = clip(1, "2019年的收藏");
        c2019.setOriginalBookmarkedAt(LocalDateTime.of(2019, 5, 1, 0, 0));
        SourceClip c2023 = clip(2, "2023年的收藏");
        c2023.setOriginalBookmarkedAt(LocalDateTime.of(2023, 1, 1, 0, 0));
        List<SourceClip> clips = List.of(c2023, c2019); // 故意乱序输入

        Map<Integer, List<SourceClip>> byYear = BookmarkAgentService.groupByYear(clips);

        assertThat(byYear.keySet()).containsExactly(2019, 2023); // TreeMap 保证升序
        assertThat(byYear.get(2019)).containsExactly(c2019);
    }

    @Test
    void groupByYear_fallsBackToCreatedAtWhenOriginalBookmarkedAtIsNull() {
        setUp();
        SourceClip clip = clip(1, "旧数据");
        clip.setOriginalBookmarkedAt(null);
        clip.setCreatedAt(LocalDateTime.of(2021, 6, 1, 0, 0));

        Map<Integer, List<SourceClip>> byYear = BookmarkAgentService.groupByYear(List.of(clip));

        assertThat(byYear).containsOnlyKeys(2021);
    }

    @Test
    void runCluster_classifiesGroupsSummarizesAndReplacesNote() throws Exception {
        setUp();
        User owner = User.builder().id(1L).build();
        SourceClip c1 = clip(1, "标题一");
        c1.setSourceUrl("https://example.com/a");
        SourceClip c2 = clip(2, "标题二");
        c2.setSourceUrl("https://example.com/b");
        List<SourceClip> clips = List.of(c1, c2);

        when(aiProperties.getProvider()).thenReturn("anthropic");
        when(anthropicService.classifyTopics(List.of("标题一", "标题二")))
                .thenReturn(List.of(List.of("AI"), List.of("AI")));
        when(anthropicService.summarizeCluster(eq("AI"), any()))
                .thenReturn("这是一组 AI 相关收藏。");
        when(noteRepository.findByOwnerAndGeneratedType(owner, Note.GeneratedType.CLUSTER))
                .thenReturn(java.util.Optional.empty());
        when(noteRepository.save(any())).thenAnswer(inv -> {
            Note n = inv.getArgument(0);
            n.setId(100L);
            return n;
        });

        Note result = service.runCluster(1L, owner, clips);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getContentType()).isEqualTo("markdown");
        assertThat(result.getContent())
                .contains("## AI")
                .contains("这是一组 AI 相关收藏。")
                .contains("- [标题一](https://example.com/a)")
                .contains("- [标题二](https://example.com/b)");
        verify(jobRepository).updateTotalSteps(1L, 1); // 1 个批次
        verify(jobRepository).updateTotalSteps(1L, 2); // 1 个批次 + 1 个分组
        verify(jobRepository, times(2)).incrementCompletedSteps(1L); // 1 个批次 + 1 个分组
        verify(noteClipRefRepository, times(2)).save(any());
    }

    @Test
    void runTimeline_bucketsByYearSummarizesAndReplacesNote() throws Exception {
        setUp();
        User owner = User.builder().id(1L).build();
        SourceClip c2023 = clip(1, "标题A");
        c2023.setOriginalBookmarkedAt(LocalDateTime.of(2023, 1, 1, 0, 0));
        c2023.setSourceUrl("https://example.com/a2023");
        List<SourceClip> clips = List.of(c2023);

        when(aiProperties.getProvider()).thenReturn("anthropic");
        when(anthropicService.summarizeTimelineBucket(eq("2023 年"), any()))
                .thenReturn("这一年收藏了不少内容。");
        when(noteRepository.findByOwnerAndGeneratedType(owner, Note.GeneratedType.TIMELINE))
                .thenReturn(java.util.Optional.empty());
        when(noteRepository.save(any())).thenAnswer(inv -> {
            Note n = inv.getArgument(0);
            n.setId(200L);
            return n;
        });

        Note result = service.runTimeline(1L, owner, clips);

        assertThat(result.getId()).isEqualTo(200L);
        assertThat(result.getContent())
                .contains("## 2023 年")
                .contains("这一年收藏了不少内容")
                .contains("- [标题A](https://example.com/a2023)");
        verify(jobRepository).updateTotalSteps(1L, 1);
        verify(jobRepository, times(1)).incrementCompletedSteps(1L);
    }

    @Test
    void generate_returnsExistingRunningJobInsteadOfCreatingNew() {
        setUp();
        User owner = User.builder().id(1L).build();
        BookmarkAgentJob running = BookmarkAgentJob.builder().id(9L).owner(owner)
                .type(BookmarkAgentJob.Type.CLUSTER).status(BookmarkAgentJob.Status.RUNNING).build();
        when(jobRepository.findTopByOwnerAndTypeOrderByCreatedAtDesc(owner, BookmarkAgentJob.Type.CLUSTER))
                .thenReturn(java.util.Optional.of(running));

        BookmarkAgentJob result = service.generate(BookmarkAgentJob.Type.CLUSTER, owner);

        assertThat(result).isSameAs(running);
        verify(jobRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void generate_createsNewRunningJobWhenNonePending() {
        setUp();
        User owner = User.builder().id(1L).build();
        when(jobRepository.findTopByOwnerAndTypeOrderByCreatedAtDesc(owner, BookmarkAgentJob.Type.TIMELINE))
                .thenReturn(java.util.Optional.empty());
        when(jobRepository.save(any())).thenAnswer(inv -> {
            BookmarkAgentJob j = inv.getArgument(0);
            j.setId(10L);
            return j;
        });

        BookmarkAgentJob result = service.generate(BookmarkAgentJob.Type.TIMELINE, owner);

        assertThat(result.getStatus()).isEqualTo(BookmarkAgentJob.Status.RUNNING);
        assertThat(result.getType()).isEqualTo(BookmarkAgentJob.Type.TIMELINE);
    }

    @Test
    void failJob_marksJobFailedWithTruncatedErrorMessage() {
        setUp();
        BookmarkAgentJob job = BookmarkAgentJob.builder().id(1L).status(BookmarkAgentJob.Status.RUNNING).build();
        when(jobRepository.findById(1L)).thenReturn(java.util.Optional.of(job));
        when(jobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.failJob(1L, "boom");

        verify(jobRepository).save(argThat(j ->
                j.getStatus() == BookmarkAgentJob.Status.FAILED && "boom".equals(j.getErrorMessage())));
    }

    @Test
    void completeJob_marksJobDoneWithResultNoteId() {
        setUp();
        BookmarkAgentJob job = BookmarkAgentJob.builder().id(1L).status(BookmarkAgentJob.Status.RUNNING).build();
        when(jobRepository.findById(1L)).thenReturn(java.util.Optional.of(job));
        when(jobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.completeJob(1L, 77L);

        verify(jobRepository).save(argThat(j ->
                j.getStatus() == BookmarkAgentJob.Status.DONE && Long.valueOf(77L).equals(j.getResultNoteId())));
    }

    @Test
    void runGenerate_dispatchesToRunClusterAndCompletesJobOnSuccess() throws Exception {
        setUp();
        User owner = User.builder().id(1L).build();
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(owner));
        when(clipRepository.findByOwnerAndSourceType(eq(owner), eq(SourceClip.SourceType.WEBPAGE), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));
        when(aiProperties.getProvider()).thenReturn("anthropic");
        when(noteRepository.findByOwnerAndGeneratedType(owner, Note.GeneratedType.CLUSTER))
                .thenReturn(java.util.Optional.empty());
        when(noteRepository.save(any())).thenAnswer(inv -> {
            Note n = inv.getArgument(0);
            n.setId(300L);
            return n;
        });
        when(jobRepository.findById(1L)).thenReturn(java.util.Optional.of(
                BookmarkAgentJob.builder().id(1L).status(BookmarkAgentJob.Status.RUNNING).build()));

        service.runGenerate(1L, BookmarkAgentJob.Type.CLUSTER, 1L);

        verify(jobRepository).save(argThat(j ->
                j.getStatus() == BookmarkAgentJob.Status.DONE && Long.valueOf(300L).equals(j.getResultNoteId())));
    }

    @Test
    void runGenerate_catchesExceptionAndCallsFailJob() throws Exception {
        setUp();
        User owner = User.builder().id(1L).build();
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(owner));
        when(clipRepository.findByOwnerAndSourceType(eq(owner), eq(SourceClip.SourceType.WEBPAGE), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(clip(1, "标题一"))));
        when(aiProperties.getProvider()).thenReturn("anthropic");
        when(anthropicService.classifyTopics(any())).thenThrow(new RuntimeException("AI 调用失败"));
        when(jobRepository.findById(1L)).thenReturn(java.util.Optional.of(
                BookmarkAgentJob.builder().id(1L).status(BookmarkAgentJob.Status.RUNNING).build()));

        service.runGenerate(1L, BookmarkAgentJob.Type.CLUSTER, 1L);

        verify(jobRepository).save(argThat(j ->
                j.getStatus() == BookmarkAgentJob.Status.FAILED && "AI 调用失败".equals(j.getErrorMessage())));
    }

    @Test
    void buildLinkListMarkdown_buildsOneBulletLinkPerClip() {
        SourceClip c1 = clip(1, "标题一");
        c1.setSourceUrl("https://example.com/a");
        SourceClip c2 = clip(2, "标题二");
        c2.setSourceUrl("https://example.com/b");

        String markdown = BookmarkAgentService.buildLinkListMarkdown(List.of(c1, c2));

        assertThat(markdown).isEqualTo(
                "- [标题一](https://example.com/a)\n"
                + "- [标题二](https://example.com/b)\n");
    }

    @Test
    void buildLinkListMarkdown_sanitizesTitleCharactersThatBreakLinkSyntax() {
        SourceClip c1 = clip(1, "标题[带方括号]和\n换行");
        c1.setSourceUrl("https://example.com/a");

        String markdown = BookmarkAgentService.buildLinkListMarkdown(List.of(c1));

        assertThat(markdown).isEqualTo("- [标题(带方括号)和 换行](https://example.com/a)\n");
    }
}
