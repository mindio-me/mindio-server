/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.service;

import com.entropybits.worknotes.spring_boot.entity.ImportItem;
import com.entropybits.worknotes.spring_boot.entity.ImportJob;
import com.entropybits.worknotes.spring_boot.entity.SourceClip;
import com.entropybits.worknotes.spring_boot.entity.User;
import com.entropybits.worknotes.spring_boot.repository.ImportItemRepository;
import com.entropybits.worknotes.spring_boot.repository.ImportJobRepository;
import com.entropybits.worknotes.spring_boot.repository.SourceClipRepository;
import com.entropybits.worknotes.spring_boot.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookmarkImportServiceTest {

    @Mock ImportJobRepository jobRepository;
    @Mock ImportItemRepository itemRepository;
    @Mock SourceClipRepository clipRepository;
    @Mock UserRepository userRepository;
    @Mock DeadLinkTrigger deadLinkTrigger;

    private final BookmarkHtmlParser parser = new BookmarkHtmlParser();
    private final BookmarkUrlNormalizer normalizer = new BookmarkUrlNormalizer();

    private BookmarkImportService service;
    private User user;

    private static final String HTML = """
            <DL><p>
                <DT><A HREF="chrome://bookmarks/" ADD_DATE="1503238940">Bookmarks</A>
                <DT><A HREF="https://example.com/a" ADD_DATE="1503358931">A</A>
                <DT><A HREF="https://example.com/a?utm_source=x" ADD_DATE="1503358932">A duplicate in batch</A>
            </DL><p>
            """;

    private void setUp() {
        service = new BookmarkImportService(
                parser, normalizer, jobRepository, itemRepository, clipRepository, userRepository, deadLinkTrigger);
        user = User.builder().id(1L).username("alice").build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(clipRepository.findByOwner(eq(user), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(jobRepository.save(any())).thenAnswer(inv -> {
            ImportJob job = inv.getArgument(0);
            if (job.getId() == null) job.setId(100L);
            return job;
        });
        when(itemRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void parseAndTriage_classifiesNoiseDuplicateAndPendingCheck() {
        setUp();

        service.parseAndTriage(HTML, "bookmarks.html", "alice");

        ArgumentCaptor<List<ImportItem>> captor = ArgumentCaptor.forClass(List.class);
        verify(itemRepository).saveAll(captor.capture());
        List<ImportItem> items = captor.getValue();

        assertThat(items).hasSize(3);
        assertThat(items).filteredOn(i -> i.getRawUrl().equals("chrome://bookmarks/"))
                .extracting(ImportItem::getCategory).containsExactly(ImportItem.Category.NOISE);
        assertThat(items).filteredOn(i -> i.getRawUrl().equals("https://example.com/a"))
                .extracting(ImportItem::getCategory).containsExactly(ImportItem.Category.PENDING_CHECK);
        assertThat(items).filteredOn(i -> i.getRawUrl().equals("https://example.com/a?utm_source=x"))
                .extracting(ImportItem::getCategory).containsExactly(ImportItem.Category.DUPLICATE);
    }

    @Test
    void parseAndTriage_flagsDuplicateAgainstExistingClip() {
        setUp();
        SourceClip existing = SourceClip.builder().id(9L).sourceUrl("https://example.com/a").build();
        when(clipRepository.findByOwner(eq(user), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(existing)));

        service.parseAndTriage(HTML, "bookmarks.html", "alice");

        ArgumentCaptor<List<ImportItem>> captor = ArgumentCaptor.forClass(List.class);
        verify(itemRepository).saveAll(captor.capture());
        List<ImportItem> items = captor.getValue();

        ImportItem a = items.stream().filter(i -> i.getRawUrl().equals("https://example.com/a")).findFirst().orElseThrow();
        assertThat(a.getCategory()).isEqualTo(ImportItem.Category.DUPLICATE);
        assertThat(a.getDuplicateOfClipId()).isEqualTo(9L);
    }

    @Test
    void parseAndTriage_setsJobStatusCheckingAndTriggersAsyncCheck() {
        setUp();

        ImportJob job = service.parseAndTriage(HTML, "bookmarks.html", "alice");

        assertThat(job.getStatus()).isEqualTo(ImportJob.JobStatus.CHECKING);
        assertThat(job.getTotalCount()).isEqualTo(3);
        verify(deadLinkTrigger).triggerCheck(job.getId());
    }
}
