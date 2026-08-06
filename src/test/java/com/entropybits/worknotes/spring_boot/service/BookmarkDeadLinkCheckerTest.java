/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.service;

import com.entropybits.worknotes.spring_boot.entity.ImportItem;
import com.entropybits.worknotes.spring_boot.entity.ImportJob;
import com.entropybits.worknotes.spring_boot.repository.ImportItemRepository;
import com.entropybits.worknotes.spring_boot.repository.ImportJobRepository;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookmarkDeadLinkCheckerTest {

    @Mock ImportItemRepository itemRepository;
    @Mock ImportJobRepository jobRepository;

    private BookmarkDeadLinkChecker checker;
    private HttpServer httpServer;

    private void setUp() throws Exception {
        checker = new BookmarkDeadLinkChecker(itemRepository, jobRepository);
        ReflectionTestUtils.setField(checker, "timeoutMs", 500);

        httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext("/ok", ex -> { ex.sendResponseHeaders(200, -1); ex.close(); });
        httpServer.createContext("/missing", ex -> { ex.sendResponseHeaders(404, -1); ex.close(); });
        httpServer.start();
    }

    @AfterEach
    void tearDown() {
        if (httpServer != null) httpServer.stop(0);
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + httpServer.getAddress().getPort();
    }

    @Test
    void checkUrl_returnsAliveFor200() throws Exception {
        setUp();
        BookmarkDeadLinkChecker.CheckResult result = checker.checkUrl(baseUrl() + "/ok");
        assertThat(result.alive()).isTrue();
        assertThat(result.httpStatus()).isEqualTo("200");
    }

    @Test
    void checkUrl_returnsDeadFor404() throws Exception {
        setUp();
        BookmarkDeadLinkChecker.CheckResult result = checker.checkUrl(baseUrl() + "/missing");
        assertThat(result.alive()).isFalse();
        assertThat(result.httpStatus()).isEqualTo("404");
    }

    @Test
    void checkUrl_returnsDeadWithTimeoutMarkerForUnreachableHost() {
        setUp0();
        BookmarkDeadLinkChecker.CheckResult result = checker.checkUrl("http://127.0.0.1:1");
        assertThat(result.alive()).isFalse();
        assertThat(result.httpStatus()).isEqualTo("TIMEOUT");
    }

    private void setUp0() {
        checker = new BookmarkDeadLinkChecker(itemRepository, jobRepository);
        ReflectionTestUtils.setField(checker, "timeoutMs", 300);
    }

    @Test
    void triggerCheck_updatesAllPendingItemsAndMarksJobReady() throws Exception {
        setUp();
        ImportJob job = ImportJob.builder().id(1L).status(ImportJob.JobStatus.CHECKING).build();
        ImportItem pending = ImportItem.builder().id(1L).job(job)
                .rawUrl(baseUrl() + "/ok").category(ImportItem.Category.PENDING_CHECK).build();
        when(itemRepository.findByJobAndCategory(any(), eq(ImportItem.Category.PENDING_CHECK)))
                .thenReturn(List.of(pending));
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));

        checker.triggerCheck(1L);
        // triggerCheck 内部异步提交任务后立即返回；给后台线程一点时间跑完
        Thread.sleep(1000);

        verify(itemRepository).save(argThat(item ->
                item.getCategory() == ImportItem.Category.IMPORTABLE && "200".equals(item.getHttpStatus())));
        verify(jobRepository).incrementCheckedCount(1L);
        verify(jobRepository).save(argThat(j -> j.getStatus() == ImportJob.JobStatus.READY));
    }
}
