/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.service;

import com.entropybits.worknotes.spring_boot.ai.config.AiProperties;
import com.entropybits.worknotes.spring_boot.ai.service.AiTranslationService;
import com.entropybits.worknotes.spring_boot.entity.*;
import com.entropybits.worknotes.spring_boot.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class BookmarkAgentService {

    private final BookmarkAgentJobRepository jobRepository;
    private final SourceClipRepository clipRepository;
    private final NoteRepository noteRepository;
    private final NoteClipRefRepository noteClipRefRepository;
    private final UserRepository userRepository;
    private final AiProperties aiProperties;
    private final AiTranslationService anthropicService;
    private final AiTranslationService openAiService;
    private final AiTranslationService deepseekService;
    private final AiTranslationService doubaoService;

    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    @Autowired
    @Lazy
    private BookmarkAgentService self;

    public BookmarkAgentService(
            BookmarkAgentJobRepository jobRepository,
            SourceClipRepository clipRepository,
            NoteRepository noteRepository,
            NoteClipRefRepository noteClipRefRepository,
            UserRepository userRepository,
            AiProperties aiProperties,
            @Qualifier("anthropicTranslationService") AiTranslationService anthropicService,
            @Qualifier("openAiTranslationService") AiTranslationService openAiService,
            @Qualifier("deepseekTranslationService") AiTranslationService deepseekService,
            @Qualifier("doubaoTranslationService") AiTranslationService doubaoService) {
        this.jobRepository = jobRepository;
        this.clipRepository = clipRepository;
        this.noteRepository = noteRepository;
        this.noteClipRefRepository = noteClipRefRepository;
        this.userRepository = userRepository;
        this.aiProperties = aiProperties;
        this.anthropicService = anthropicService;
        this.openAiService = openAiService;
        this.deepseekService = deepseekService;
        this.doubaoService = doubaoService;
    }

    /** 把 clips 按每批 size 条切分，用于分批调用 classifyTopics */
    static List<List<SourceClip>> partition(List<SourceClip> list, int size) {
        List<List<SourceClip>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return result;
    }

    /**
     * 每个 clip 取候选主题词列表的第一个作为主题词，按主题词精确字符串匹配分组；
     * 成员数 < 2 的分组（含没有候选主题词的 clip）一律并入统一的"其他"分组。
     */
    static LinkedHashMap<String, List<SourceClip>> groupByTopic(List<SourceClip> clips, List<List<String>> topicsPerClip) {
        LinkedHashMap<String, List<SourceClip>> byTopic = new LinkedHashMap<>();
        for (int i = 0; i < clips.size(); i++) {
            List<String> topics = topicsPerClip.get(i);
            String topic = (topics != null && !topics.isEmpty()) ? topics.get(0).trim() : "";
            if (topic.isEmpty()) topic = "其他";
            byTopic.computeIfAbsent(topic, k -> new ArrayList<>()).add(clips.get(i));
        }

        LinkedHashMap<String, List<SourceClip>> result = new LinkedHashMap<>();
        List<SourceClip> misc = new ArrayList<>();
        for (Map.Entry<String, List<SourceClip>> entry : byTopic.entrySet()) {
            if ("其他".equals(entry.getKey()) || entry.getValue().size() < 2) {
                misc.addAll(entry.getValue());
            } else {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        if (!misc.isEmpty()) {
            result.put("其他", misc);
        }
        return result;
    }

    /** 按 originalBookmarkedAt（为空则退回 createdAt）的年份分桶，TreeMap 天然按年份升序 */
    static Map<Integer, List<SourceClip>> groupByYear(List<SourceClip> clips) {
        Map<Integer, List<SourceClip>> byYear = new TreeMap<>();
        for (SourceClip clip : clips) {
            LocalDateTime dt = clip.getOriginalBookmarkedAt() != null ? clip.getOriginalBookmarkedAt() : clip.getCreatedAt();
            int year = dt.getYear();
            byYear.computeIfAbsent(year, k -> new ArrayList<>()).add(clip);
        }
        return byYear;
    }

    /**
     * 用新生成的内容覆盖用户当前该类型的有效结果：先删除旧 Note 并 flush，再插入新的。
     * 必须先 flush 删除——Hibernate 默认 flush 顺序是先 INSERT 后 DELETE，如果不先 flush，
     * 新 Note 的 INSERT 会在旧 Note 的 DELETE 之前执行，触发 (owner_id, generated_type) 唯一索引冲突。
     * 必须通过 self 代理调用（而不是 this. 直接调用）——@Transactional 依赖 Spring 的代理式 AOP，
     * 只拦截"经过代理"的外部调用，同一个 bean 内部的 this. 自调用会绕过代理，导致注解静默失效。
     */
    @Transactional
    public Note replaceGeneratedNote(User owner, Note.GeneratedType type, String title, String markdownContent,
                                      List<SourceClip> refClips) {
        noteRepository.findByOwnerAndGeneratedType(owner, type).ifPresent(old -> {
            noteRepository.delete(old);
            noteRepository.flush();
        });

        Note note = noteRepository.save(Note.builder()
                .title(title)
                .content(markdownContent)
                .contentType("markdown")
                .owner(owner)
                .generatedType(type)
                .build());

        int order = 0;
        for (SourceClip clip : refClips) {
            noteClipRefRepository.save(NoteClipRef.builder()
                    .note(note)
                    .clip(clip)
                    .sortOrder(order++)
                    .build());
        }
        return note;
    }

    /** 把收藏列表拼成 markdown 链接列表，每条一行，供 runCluster/runTimeline 拼进生成结果 */
    static String buildLinkListMarkdown(List<SourceClip> clips) {
        StringBuilder sb = new StringBuilder();
        for (SourceClip clip : clips) {
            sb.append("- [").append(sanitizeLinkText(clip.getTitle()))
                    .append("](").append(clip.getSourceUrl()).append(")\n");
        }
        return sb.toString();
    }

    /**
     * 去掉标题里会破坏 [标题](链接) 语法的字符——渲染端 renderMarkdown 是简单正则实现
     * （\[([^\]]+)\]\(([^)]+)\)），标题原样带 ] 会把链接边界截断。
     */
    private static String sanitizeLinkText(String text) {
        if (text == null) return "";
        return text.replace("[", "(").replace("]", ")").replace("\n", " ").replace("\r", " ").trim();
    }

    Note runCluster(Long jobId, User owner, List<SourceClip> clips) throws Exception {
        AiTranslationService ai = resolveService();
        List<List<SourceClip>> batches = partition(clips, 50);
        jobRepository.updateTotalSteps(jobId, batches.size());

        List<List<String>> topicsPerClip = new ArrayList<>();
        for (List<SourceClip> batch : batches) {
            List<String> titles = batch.stream().map(SourceClip::getTitle).toList();
            topicsPerClip.addAll(ai.classifyTopics(titles));
            jobRepository.incrementCompletedSteps(jobId);
        }

        LinkedHashMap<String, List<SourceClip>> groups = groupByTopic(clips, topicsPerClip);
        jobRepository.updateTotalSteps(jobId, batches.size() + groups.size());

        StringBuilder markdown = new StringBuilder();
        List<SourceClip> orderedRefs = new ArrayList<>();
        for (Map.Entry<String, List<SourceClip>> entry : groups.entrySet()) {
            List<String> titles = entry.getValue().stream().map(SourceClip::getTitle).toList();
            String summary = ai.summarizeCluster(entry.getKey(), titles);
            markdown.append("## ").append(entry.getKey()).append("\n\n")
                    .append(summary).append("\n\n")
                    .append(buildLinkListMarkdown(entry.getValue())).append("\n");
            orderedRefs.addAll(entry.getValue());
            jobRepository.incrementCompletedSteps(jobId);
        }

        return self.replaceGeneratedNote(owner, Note.GeneratedType.CLUSTER, "知识地图", markdown.toString(), orderedRefs);
    }

    Note runTimeline(Long jobId, User owner, List<SourceClip> clips) throws Exception {
        AiTranslationService ai = resolveService();
        Map<Integer, List<SourceClip>> byYear = groupByYear(clips);
        jobRepository.updateTotalSteps(jobId, byYear.size());

        StringBuilder markdown = new StringBuilder();
        List<SourceClip> orderedRefs = new ArrayList<>();
        for (Map.Entry<Integer, List<SourceClip>> entry : byYear.entrySet()) {
            List<String> titles = entry.getValue().stream().map(SourceClip::getTitle).toList();
            String bucketLabel = entry.getKey() + " 年";
            String narrative = ai.summarizeTimelineBucket(bucketLabel, titles);
            markdown.append("## ").append(bucketLabel).append("\n\n")
                    .append(narrative).append("\n\n")
                    .append(buildLinkListMarkdown(entry.getValue())).append("\n");
            orderedRefs.addAll(entry.getValue());
            jobRepository.incrementCompletedSteps(jobId);
        }

        return self.replaceGeneratedNote(owner, Note.GeneratedType.TIMELINE, "时间线回顾", markdown.toString(), orderedRefs);
    }

    /** 若该用户该类型已有 RUNNING 的任务直接复用，否则建新任务并提交异步生成 */
    public BookmarkAgentJob generate(BookmarkAgentJob.Type type, User user) {
        Optional<BookmarkAgentJob> latest = jobRepository.findTopByOwnerAndTypeOrderByCreatedAtDesc(user, type);
        if (latest.isPresent() && latest.get().getStatus() == BookmarkAgentJob.Status.RUNNING) {
            return latest.get();
        }

        BookmarkAgentJob job = jobRepository.save(BookmarkAgentJob.builder()
                .owner(user)
                .type(type)
                .status(BookmarkAgentJob.Status.RUNNING)
                .build());

        Long jobId = job.getId();
        Long ownerId = user.getId();
        executor.submit(() -> runGenerate(jobId, type, ownerId));
        return job;
    }

    void runGenerate(Long jobId, BookmarkAgentJob.Type type, Long ownerId) {
        try {
            User owner = userRepository.findById(ownerId).orElseThrow();
            List<SourceClip> clips = clipRepository
                    .findByOwnerAndSourceType(owner, SourceClip.SourceType.WEBPAGE, Pageable.unpaged())
                    .getContent();
            Note note = type == BookmarkAgentJob.Type.CLUSTER
                    ? runCluster(jobId, owner, clips)
                    : runTimeline(jobId, owner, clips);
            self.completeJob(jobId, note.getId());
        } catch (Exception e) {
            log.error("BookmarkAgentJob {} failed", jobId, e);
            self.failJob(jobId, e.getMessage());
        }
    }

    // NOTE: 这两个方法必须是 public（Spring 代理式 AOP 只拦截 public 方法），
    // 且调用方必须通过 self 代理调用而不是 this. 直接调用（同一 bean 内部自调用会绕过代理），
    // 两个条件同时满足 @Transactional 才会真正生效——参见 replaceGeneratedNote 上的注释。
    @Transactional
    public void failJob(Long jobId, String message) {
        BookmarkAgentJob job = jobRepository.findById(jobId).orElseThrow();
        job.setStatus(BookmarkAgentJob.Status.FAILED);
        job.setErrorMessage(message != null && message.length() > 500 ? message.substring(0, 500) : message);
        job.setFinishedAt(LocalDateTime.now());
        jobRepository.save(job);
    }

    @Transactional
    public void completeJob(Long jobId, Long noteId) {
        BookmarkAgentJob job = jobRepository.findById(jobId).orElseThrow();
        job.setStatus(BookmarkAgentJob.Status.DONE);
        job.setResultNoteId(noteId);
        job.setFinishedAt(LocalDateTime.now());
        jobRepository.save(job);
    }

    private AiTranslationService resolveService() {
        return switch (aiProperties.getProvider().toLowerCase()) {
            case "openai" -> openAiService;
            case "deepseek" -> deepseekService;
            case "doubao" -> doubaoService;
            default -> anthropicService;
        };
    }
}
