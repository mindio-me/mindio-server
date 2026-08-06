/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.service;

import com.entropybits.worknotes.spring_boot.entity.ImportItem;
import com.entropybits.worknotes.spring_boot.entity.ImportJob;
import com.entropybits.worknotes.spring_boot.repository.ImportItemRepository;
import com.entropybits.worknotes.spring_boot.repository.ImportJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookmarkDeadLinkChecker implements DeadLinkTrigger {

    private static final String USER_AGENT =
            "Mozilla/5.0 (compatible; MindioBot/1.0; +https://mindio.me)";
    private static final int MAX_CONCURRENCY = 16;
    private static final int PER_HOST_CONCURRENCY = 2;

    private int timeoutMs = 5000;

    private final ImportItemRepository itemRepository;
    private final ImportJobRepository jobRepository;

    private final ExecutorService executor = Executors.newFixedThreadPool(MAX_CONCURRENCY);
    private final Map<String, Semaphore> hostSemaphores = new ConcurrentHashMap<>();

    public record CheckResult(boolean alive, String httpStatus) {
    }

    @Override
    public void triggerCheck(Long jobId) {
        ImportJob job = jobRepository.findById(jobId).orElseThrow();
        List<ImportItem> pending = itemRepository.findByJobAndCategory(job, ImportItem.Category.PENDING_CHECK);

        List<CompletableFuture<Void>> futures = pending.stream()
                .map(item -> CompletableFuture.runAsync(() -> checkOne(item, jobId), executor))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> finishJob(jobId));
    }

    private void checkOne(ImportItem item, Long jobId) {
        Semaphore semaphore = hostSemaphores.computeIfAbsent(hostOf(item.getRawUrl()),
                h -> new Semaphore(PER_HOST_CONCURRENCY));
        try {
            semaphore.acquire();
            try {
                CheckResult result = checkUrl(item.getRawUrl());
                item.setCategory(result.alive() ? ImportItem.Category.IMPORTABLE : ImportItem.Category.DEAD_LINK);
                item.setHttpStatus(result.httpStatus());
                itemRepository.save(item);
                jobRepository.incrementCheckedCount(jobId);
            } finally {
                semaphore.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** package-private 方便测试直接调用，不需要跑并发管线 */
    CheckResult checkUrl(String url) {
        try {
            Connection.Response resp = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(timeoutMs)
                    .method(Connection.Method.HEAD)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .execute();
            int status = resp.statusCode();
            if (status == 405) { // 部分站点不支持 HEAD，退化成 GET
                resp = Jsoup.connect(url)
                        .userAgent(USER_AGENT)
                        .timeout(timeoutMs)
                        .method(Connection.Method.GET)
                        .ignoreHttpErrors(true)
                        .ignoreContentType(true)
                        .execute();
                status = resp.statusCode();
            }
            boolean alive = status < 400;
            return new CheckResult(alive, String.valueOf(status));
        } catch (Exception e) {
            log.debug("Dead-link check failed for {}: {}", url, e.toString());
            return new CheckResult(false, "TIMEOUT");
        }
    }

    private void finishJob(Long jobId) {
        ImportJob job = jobRepository.findById(jobId).orElseThrow();
        job.setStatus(ImportJob.JobStatus.READY);
        jobRepository.save(job);
    }

    private String hostOf(String url) {
        try {
            String host = URI.create(url).getHost();
            return host != null ? host : url;
        } catch (Exception e) {
            return url;
        }
    }
}
