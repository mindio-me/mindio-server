/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.service;

import com.entropybits.worknotes.spring_boot.entity.*;
import com.entropybits.worknotes.spring_boot.repository.ImportItemRepository;
import com.entropybits.worknotes.spring_boot.repository.SourceClipRepository;
import com.entropybits.worknotes.spring_boot.repository.TagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookmarkMergeServiceTest {

    @Mock ImportItemRepository itemRepository;
    @Mock SourceClipRepository clipRepository;
    @Mock TagRepository tagRepository;

    private BookmarkMergeService service;

    private void setUp() {
        service = new BookmarkMergeService(itemRepository, clipRepository, tagRepository);
        when(clipRepository.save(any())).thenAnswer(inv -> {
            SourceClip c = inv.getArgument(0);
            if (c.getId() == null) c.setId(999L);
            return c;
        });
        when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void merge_convertsConfirmedItemToSourceClipWithOriginalBookmarkedAt() {
        setUp();
        User owner = User.builder().id(1L).build();
        ImportJob job = ImportJob.builder().id(1L).owner(owner).build();
        LocalDateTime addedAt = LocalDateTime.of(2019, 3, 1, 0, 0);
        ImportItem item = ImportItem.builder().id(1L).job(job)
                .rawTitle("Example").rawUrl("https://example.com/a")
                .folderPath(null)
                .bookmarkAddedAt(addedAt)
                .category(ImportItem.Category.IMPORTABLE)
                .userDecision(ImportItem.UserDecision.CONFIRMED)
                .build();

        List<SourceClip> result = service.merge(job, List.of(item));

        assertThat(result).hasSize(1);
        SourceClip clip = result.get(0);
        assertThat(clip.getSourceType()).isEqualTo(SourceClip.SourceType.WEBPAGE);
        assertThat(clip.getSourceUrl()).isEqualTo("https://example.com/a");
        assertThat(clip.getTitle()).isEqualTo("Example");
        assertThat(clip.getExtractionMode()).isEqualTo(SourceClip.ExtractionMode.LINK_ONLY);
        assertThat(clip.getOriginalBookmarkedAt()).isEqualTo(addedAt);
        assertThat(clip.getOwner()).isEqualTo(owner);
    }

    @Test
    void merge_marksDeadLinkOriginFieldsWhenItemCategoryIsDeadLink() {
        setUp();
        User owner = User.builder().id(1L).build();
        ImportJob job = ImportJob.builder().id(1L).owner(owner).build();
        ImportItem item = ImportItem.builder().id(1L).job(job)
                .rawTitle("Example").rawUrl("https://example.com/a")
                .category(ImportItem.Category.DEAD_LINK)
                .userDecision(ImportItem.UserDecision.CONFIRMED)
                .build();

        List<SourceClip> result = service.merge(job, List.of(item));

        assertThat(result.get(0).getWasDetectedDeadLink()).isTrue();
        assertThat(result.get(0).getManuallyConfirmedAlive()).isTrue();
    }

    @Test
    void merge_leavesDeadLinkFieldsFalseWhenItemCategoryIsImportable() {
        setUp();
        User owner = User.builder().id(1L).build();
        ImportJob job = ImportJob.builder().id(1L).owner(owner).build();
        ImportItem item = ImportItem.builder().id(1L).job(job)
                .rawTitle("Example").rawUrl("https://example.com/a")
                .category(ImportItem.Category.IMPORTABLE)
                .userDecision(ImportItem.UserDecision.CONFIRMED)
                .build();

        List<SourceClip> result = service.merge(job, List.of(item));

        assertThat(result.get(0).getWasDetectedDeadLink()).isFalse();
        assertThat(result.get(0).getManuallyConfirmedAlive()).isFalse();
    }

    @Test
    void merge_writesBackResultClipIdOnItem() {
        setUp();
        User owner = User.builder().id(1L).build();
        ImportJob job = ImportJob.builder().id(1L).owner(owner).build();
        ImportItem item = ImportItem.builder().id(1L).job(job)
                .rawTitle("Example").rawUrl("https://example.com/a")
                .category(ImportItem.Category.IMPORTABLE)
                .userDecision(ImportItem.UserDecision.CONFIRMED)
                .build();

        service.merge(job, List.of(item));

        verify(itemRepository).save(argThat(saved -> saved.getResultClipId() != null));
    }

    @Test
    void merge_createsAndReusesTagFromFolderPathLeafSegment() {
        setUp();
        User owner = User.builder().id(1L).build();
        ImportJob job = ImportJob.builder().id(1L).owner(owner).build();
        ImportItem item = ImportItem.builder().id(1L).job(job)
                .rawTitle("Example").rawUrl("https://example.com/a")
                .folderPath("书签栏/工具")
                .category(ImportItem.Category.IMPORTABLE)
                .userDecision(ImportItem.UserDecision.CONFIRMED)
                .build();
        when(tagRepository.findByNameAndOwner("工具", owner)).thenReturn(Optional.empty());
        when(tagRepository.save(any())).thenAnswer(inv -> {
            Tag t = inv.getArgument(0);
            if (t.getId() == null) t.setId(5L);
            return t;
        });

        List<SourceClip> result = service.merge(job, List.of(item));

        assertThat(result.get(0).getTags()).extracting(Tag::getName).containsExactly("工具");
        verify(tagRepository).save(argThat(t -> "工具".equals(t.getName()) && t.getOwner().equals(owner)));
    }

    @Test
    void markDecision_bulkUpdatesAllItemsInCategory() {
        setUp();
        ImportJob job = ImportJob.builder().id(1L).build();
        ImportItem a = ImportItem.builder().id(1L).job(job).category(ImportItem.Category.NOISE)
                .userDecision(ImportItem.UserDecision.PENDING).build();
        ImportItem b = ImportItem.builder().id(2L).job(job).category(ImportItem.Category.NOISE)
                .userDecision(ImportItem.UserDecision.PENDING).build();
        when(itemRepository.findByJobAndCategory(job, ImportItem.Category.NOISE)).thenReturn(List.of(a, b));

        service.markDecision(job, ImportItem.Category.NOISE, ImportItem.UserDecision.SKIPPED);

        assertThat(a.getUserDecision()).isEqualTo(ImportItem.UserDecision.SKIPPED);
        assertThat(b.getUserDecision()).isEqualTo(ImportItem.UserDecision.SKIPPED);
        verify(itemRepository).saveAll(List.of(a, b));
    }

    @Test
    void markItemsDecision_bulkUpdatesGivenItems() {
        setUp();
        ImportJob job = ImportJob.builder().id(1L).build();
        ImportItem a = ImportItem.builder().id(1L).job(job).category(ImportItem.Category.DEAD_LINK)
                .userDecision(ImportItem.UserDecision.PENDING).build();
        ImportItem b = ImportItem.builder().id(2L).job(job).category(ImportItem.Category.DEAD_LINK)
                .userDecision(ImportItem.UserDecision.PENDING).build();

        service.markItemsDecision(List.of(a, b), ImportItem.UserDecision.CONFIRMED);

        assertThat(a.getUserDecision()).isEqualTo(ImportItem.UserDecision.CONFIRMED);
        assertThat(b.getUserDecision()).isEqualTo(ImportItem.UserDecision.CONFIRMED);
        verify(itemRepository).saveAll(List.of(a, b));
    }
}
