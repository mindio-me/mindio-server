/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.service;

import com.entropybits.worknotes.spring_boot.entity.*;
import com.entropybits.worknotes.spring_boot.exception.ResourceNotFoundException;
import com.entropybits.worknotes.spring_boot.repository.ImportItemRepository;
import com.entropybits.worknotes.spring_boot.repository.ImportJobRepository;
import com.entropybits.worknotes.spring_boot.repository.SourceClipRepository;
import com.entropybits.worknotes.spring_boot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BookmarkImportService {

    private final BookmarkHtmlParser parser;
    private final BookmarkUrlNormalizer normalizer;
    private final ImportJobRepository jobRepository;
    private final ImportItemRepository itemRepository;
    private final SourceClipRepository clipRepository;
    private final UserRepository userRepository;
    private final DeadLinkTrigger deadLinkTrigger;

    @Transactional
    public ImportJob parseAndTriage(String html, String fileName, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        List<ParsedBookmark> parsed = parser.parse(html);

        Map<String, Long> existingByNormalizedUrl = new HashMap<>();
        for (SourceClip clip : clipRepository.findByOwner(user, Pageable.unpaged())) {
            if (clip.getSourceUrl() != null) {
                existingByNormalizedUrl.put(normalizer.normalize(clip.getSourceUrl()), clip.getId());
            }
        }

        ImportJob job = jobRepository.save(ImportJob.builder()
                .owner(user)
                .status(ImportJob.JobStatus.PARSING)
                .fileName(fileName)
                .totalCount(parsed.size())
                .build());

        java.util.Set<String> seenInBatch = new java.util.HashSet<>();
        List<ImportItem> items = new java.util.ArrayList<>();
        for (ParsedBookmark bookmark : parsed) {
            String normalizedUrl = normalizer.normalize(bookmark.rawUrl());
            ImportItem.NoiseReason noiseReason = parser.isNoise(bookmark.rawUrl());

            ImportItem.Category category;
            Long duplicateOfClipId = null;
            if (noiseReason != null) {
                category = ImportItem.Category.NOISE;
            } else if (existingByNormalizedUrl.containsKey(normalizedUrl)) {
                category = ImportItem.Category.DUPLICATE;
                duplicateOfClipId = existingByNormalizedUrl.get(normalizedUrl);
            } else if (!seenInBatch.add(normalizedUrl)) {
                category = ImportItem.Category.DUPLICATE;
            } else {
                category = ImportItem.Category.PENDING_CHECK;
            }

            items.add(ImportItem.builder()
                    .job(job)
                    .rawTitle(bookmark.title())
                    .rawUrl(bookmark.rawUrl())
                    .normalizedUrl(normalizedUrl)
                    .folderPath(bookmark.folderPath())
                    .bookmarkAddedAt(bookmark.addedAt())
                    .category(category)
                    .noiseReason(noiseReason)
                    .duplicateOfClipId(duplicateOfClipId)
                    .userDecision(ImportItem.UserDecision.PENDING)
                    .build());
        }
        itemRepository.saveAll(items);

        job.setStatus(ImportJob.JobStatus.CHECKING);
        jobRepository.save(job);

        deadLinkTrigger.triggerCheck(job.getId());
        return job;
    }
}
