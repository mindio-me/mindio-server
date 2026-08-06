/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.service;

import com.entropybits.worknotes.spring_boot.entity.*;
import com.entropybits.worknotes.spring_boot.repository.ImportItemRepository;
import com.entropybits.worknotes.spring_boot.repository.SourceClipRepository;
import com.entropybits.worknotes.spring_boot.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BookmarkMergeService {

    private final ImportItemRepository itemRepository;
    private final SourceClipRepository clipRepository;
    private final TagRepository tagRepository;

    @Transactional
    public void markDecision(ImportJob job, ImportItem.Category category, ImportItem.UserDecision decision) {
        List<ImportItem> items = itemRepository.findByJobAndCategory(job, category);
        items.forEach(i -> i.setUserDecision(decision));
        itemRepository.saveAll(items);
    }

    @Transactional
    public void markItemDecision(ImportItem item, ImportItem.UserDecision decision) {
        item.setUserDecision(decision);
        itemRepository.save(item);
    }

    @Transactional
    public void markItemsDecision(List<ImportItem> items, ImportItem.UserDecision decision) {
        items.forEach(i -> i.setUserDecision(decision));
        itemRepository.saveAll(items);
    }

    @Transactional
    public List<SourceClip> merge(ImportJob job, List<ImportItem> confirmedItems) {
        List<SourceClip> created = new ArrayList<>();
        for (ImportItem item : confirmedItems) {
            Set<Tag> tags = new HashSet<>();
            String leafFolder = leafFolderName(item.getFolderPath());
            if (leafFolder != null) {
                tags.add(resolveTag(leafFolder, job.getOwner()));
            }

            boolean fromDeadLink = item.getCategory() == ImportItem.Category.DEAD_LINK;

            SourceClip clip = SourceClip.builder()
                    .sourceType(SourceClip.SourceType.WEBPAGE)
                    .sourceUrl(item.getRawUrl())
                    .title(item.getRawTitle() != null && !item.getRawTitle().isBlank()
                            ? item.getRawTitle() : item.getRawUrl())
                    .extractionMode(SourceClip.ExtractionMode.LINK_ONLY)
                    .extractionStatus(SourceClip.ExtractionStatus.SUCCESS)
                    .originalBookmarkedAt(item.getBookmarkAddedAt())
                    .wasDetectedDeadLink(fromDeadLink)
                    .manuallyConfirmedAlive(fromDeadLink)
                    .tags(tags)
                    .owner(job.getOwner())
                    .build();
            clip = clipRepository.save(clip);
            created.add(clip);

            item.setResultClipId(clip.getId());
            itemRepository.save(item);
        }
        return created;
    }

    private String leafFolderName(String folderPath) {
        if (folderPath == null || folderPath.isBlank()) return null;
        String[] parts = folderPath.split("/");
        return parts[parts.length - 1];
    }

    private Tag resolveTag(String name, User owner) {
        return tagRepository.findByNameAndOwner(name, owner)
                .orElseGet(() -> tagRepository.save(Tag.builder().name(name).owner(owner).build()));
    }
}
