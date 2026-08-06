/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.controller;

import com.entropybits.worknotes.spring_boot.entity.Attachment;
import com.entropybits.worknotes.spring_boot.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 附件管理控制器
 */
@RestController
@RequestMapping("/v1/attachments")
@Tag(name = "附件管理", description = "附件管理相关接口")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @GetMapping
    @Operation(summary = "获取附件列表", description = "分页查询附件列表")
    public ResponseEntity<Page<Attachment>> getAttachments(
            @Parameter(description = "分类ID") @RequestParam(required = false) Integer pid,
            @Parameter(description = "附件类型") @RequestParam(required = false) String attType,
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "createTime") String sortBy,
            @Parameter(description = "排序方向") @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<Attachment> attachments = attachmentService.getList(pid, attType, pageable);
        return ResponseEntity.ok(attachments);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取附件详情", description = "根据ID获取附件信息")
    public ResponseEntity<Attachment> getAttachment(
            @Parameter(description = "附件ID") @PathVariable Long id
    ) {
        Attachment attachment = attachmentService.getById(id);
        return ResponseEntity.ok(attachment);
    }

    @DeleteMapping
    @Operation(summary = "删除附件", description = "批量删除附件")
    public ResponseEntity<Void> deleteAttachments(
            @Parameter(description = "附件ID列表") @RequestBody List<Long> ids
    ) {
        attachmentService.deleteByIds(ids);
        return ResponseEntity.noContent().build();
    }
}

