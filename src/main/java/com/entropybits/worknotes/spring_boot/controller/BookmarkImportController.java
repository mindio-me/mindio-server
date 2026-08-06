/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.controller;

import com.entropybits.worknotes.spring_boot.dto.GroupConfirmRequest;
import com.entropybits.worknotes.spring_boot.dto.ImportItemResponse;
import com.entropybits.worknotes.spring_boot.dto.ImportJobResponse;
import com.entropybits.worknotes.spring_boot.dto.ItemsConfirmRequest;
import com.entropybits.worknotes.spring_boot.entity.ImportItem;
import com.entropybits.worknotes.spring_boot.entity.ImportJob;
import com.entropybits.worknotes.spring_boot.exception.BadRequestException;
import com.entropybits.worknotes.spring_boot.exception.ResourceNotFoundException;
import com.entropybits.worknotes.spring_boot.repository.ImportItemRepository;
import com.entropybits.worknotes.spring_boot.repository.ImportJobRepository;
import com.entropybits.worknotes.spring_boot.service.BookmarkImportService;
import com.entropybits.worknotes.spring_boot.service.BookmarkMergeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/bookmark-import")
@RequiredArgsConstructor
public class BookmarkImportController {

    private final BookmarkImportService importService;
    private final BookmarkMergeService mergeService;
    private final ImportJobRepository jobRepository;
    private final ImportItemRepository itemRepository;

    @PostMapping("/parse")
    public ResponseEntity<ImportJobResponse> parse(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails user) throws IOException {
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().matches(".*\\.html?$")) {
            throw new BadRequestException("请上传浏览器导出的书签 HTML 文件（.html/.htm）");
        }
        String html = new String(file.getBytes(), StandardCharsets.UTF_8);
        ImportJob job = importService.parseAndTriage(html, fileName, user.getUsername());
        return ResponseEntity.ok(toResponse(job, false));
    }

    @GetMapping("/jobs/{id}")
    public ResponseEntity<ImportJobResponse> getJob(@PathVariable Long id) {
        ImportJob job = findJob(id);
        return ResponseEntity.ok(toResponse(job, true));
    }

    @PostMapping("/jobs/{id}/confirm-group")
    public ResponseEntity<Void> confirmGroup(@PathVariable Long id, @Valid @RequestBody GroupConfirmRequest req) {
        ImportJob job = findJob(id);
        mergeService.markDecision(job, req.getCategory(), req.getDecision());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/jobs/{id}/confirm-items")
    public ResponseEntity<Void> confirmItems(@PathVariable Long id, @Valid @RequestBody ItemsConfirmRequest req) {
        findJob(id); // 校验 job 存在
        List<ImportItem> items = itemRepository.findAllById(req.getItemIds());
        mergeService.markItemsDecision(items, req.getDecision());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/jobs/{id}/merge")
    public ResponseEntity<Void> merge(@PathVariable Long id) {
        ImportJob job = findJob(id);
        List<ImportItem> confirmed = Arrays.stream(new ImportItem.Category[]{
                        ImportItem.Category.IMPORTABLE, ImportItem.Category.DEAD_LINK})
                .flatMap(cat -> itemRepository
                        .findByJobAndCategoryAndUserDecision(job, cat, ImportItem.UserDecision.CONFIRMED).stream())
                .collect(Collectors.toList());
        mergeService.merge(job, confirmed);
        job.setStatus(ImportJob.JobStatus.DONE);
        jobRepository.save(job);
        return ResponseEntity.noContent().build();
    }

    private ImportJob findJob(Long id) {
        return jobRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("导入任务不存在"));
    }

    private ImportJobResponse toResponse(ImportJob job, boolean includeGroups) {
        ImportJobResponse.ImportJobResponseBuilder builder = ImportJobResponse.builder()
                .id(job.getId())
                .status(job.getStatus())
                .fileName(job.getFileName())
                .totalCount(job.getTotalCount())
                .checkedCount(job.getCheckedCount());
        if (includeGroups) {
            List<ImportItem> items = itemRepository.findByJob(job);
            builder.groups(items.stream()
                    .filter(i -> i.getCategory() != ImportItem.Category.PENDING_CHECK)
                    .map(ImportItemResponse::fromEntity)
                    .collect(Collectors.groupingBy(ImportItemResponse::getCategory)));
        }
        return builder.build();
    }
}
