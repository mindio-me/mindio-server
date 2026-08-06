/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.controller;

import com.entropybits.worknotes.spring_boot.dto.GroupConfirmRequest;
import com.entropybits.worknotes.spring_boot.dto.ItemsConfirmRequest;
import com.entropybits.worknotes.spring_boot.entity.ImportItem;
import com.entropybits.worknotes.spring_boot.entity.ImportJob;
import com.entropybits.worknotes.spring_boot.exception.BadRequestException;
import com.entropybits.worknotes.spring_boot.exception.ResourceNotFoundException;
import com.entropybits.worknotes.spring_boot.repository.ImportItemRepository;
import com.entropybits.worknotes.spring_boot.repository.ImportJobRepository;
import com.entropybits.worknotes.spring_boot.service.BookmarkImportService;
import com.entropybits.worknotes.spring_boot.service.BookmarkMergeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookmarkImportControllerTest {

    @Mock BookmarkImportService importService;
    @Mock BookmarkMergeService mergeService;
    @Mock ImportJobRepository jobRepository;
    @Mock ImportItemRepository itemRepository;

    private BookmarkImportController controller;
    private final UserDetails principal =
            org.springframework.security.core.userdetails.User.withUsername("alice").password("x").authorities("USER").build();

    private void setUp() {
        controller = new BookmarkImportController(importService, mergeService, jobRepository, itemRepository);
    }

    @Test
    void parse_delegatesToServiceWithFileContentAndUsername() throws Exception {
        setUp();
        MockMultipartFile file = new MockMultipartFile(
                "file", "bookmarks.html", "text/html", "<DL><p></DL>".getBytes());
        ImportJob job = ImportJob.builder().id(1L).status(ImportJob.JobStatus.CHECKING).totalCount(0).checkedCount(0).build();
        when(importService.parseAndTriage(any(), eq("bookmarks.html"), eq("alice"))).thenReturn(job);

        ResponseEntity<?> response = controller.parse(file, principal);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(importService).parseAndTriage("<DL><p></DL>", "bookmarks.html", "alice");
    }

    @Test
    void parse_rejectsNonHtmlFileWithoutCreatingJob() {
        setUp();
        MockMultipartFile file = new MockMultipartFile(
                "file", "bookmarks.json", "application/json", "{}".getBytes());

        assertThatThrownBy(() -> controller.parse(file, principal))
                .isInstanceOf(BadRequestException.class);
        verify(importService, never()).parseAndTriage(any(), any(), any());
    }

    @Test
    void confirmGroup_delegatesToMergeService() {
        setUp();
        ImportJob job = ImportJob.builder().id(1L).build();
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        GroupConfirmRequest req = new GroupConfirmRequest();
        req.setCategory(ImportItem.Category.NOISE);
        req.setDecision(ImportItem.UserDecision.SKIPPED);

        controller.confirmGroup(1L, req);

        verify(mergeService).markDecision(job, ImportItem.Category.NOISE, ImportItem.UserDecision.SKIPPED);
    }

    @Test
    void confirmItems_looksUpAllItemsAndDelegatesToMergeService() {
        setUp();
        ImportJob job = ImportJob.builder().id(1L).build();
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        ImportItem item1 = ImportItem.builder().id(10L).job(job)
                .category(ImportItem.Category.DEAD_LINK).build();
        ImportItem item2 = ImportItem.builder().id(11L).job(job)
                .category(ImportItem.Category.DEAD_LINK).build();
        when(itemRepository.findAllById(List.of(10L, 11L))).thenReturn(List.of(item1, item2));

        ItemsConfirmRequest req = new ItemsConfirmRequest();
        req.setItemIds(List.of(10L, 11L));
        req.setDecision(ImportItem.UserDecision.CONFIRMED);

        controller.confirmItems(1L, req);

        verify(mergeService).markItemsDecision(List.of(item1, item2), ImportItem.UserDecision.CONFIRMED);
    }

    @Test
    void getJob_throwsWhenJobNotFound() {
        setUp();
        when(jobRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.getJob(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void merge_delegatesConfirmedImportableAndDeadLinkItemsToMergeService() {
        setUp();
        ImportJob job = ImportJob.builder().id(1L).build();
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        ImportItem importable = ImportItem.builder().id(1L).job(job)
                .category(ImportItem.Category.IMPORTABLE).userDecision(ImportItem.UserDecision.CONFIRMED).build();
        ImportItem deadLinkException = ImportItem.builder().id(2L).job(job)
                .category(ImportItem.Category.DEAD_LINK).userDecision(ImportItem.UserDecision.CONFIRMED).build();
        when(itemRepository.findByJobAndCategoryAndUserDecision(job, ImportItem.Category.IMPORTABLE, ImportItem.UserDecision.CONFIRMED))
                .thenReturn(List.of(importable));
        when(itemRepository.findByJobAndCategoryAndUserDecision(job, ImportItem.Category.DEAD_LINK, ImportItem.UserDecision.CONFIRMED))
                .thenReturn(List.of(deadLinkException));

        controller.merge(1L);

        verify(mergeService).merge(job, List.of(importable, deadLinkException));
    }
}
