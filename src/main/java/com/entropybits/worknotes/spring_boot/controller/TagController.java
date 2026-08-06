/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.controller;

import com.entropybits.worknotes.spring_boot.dto.TagRequest;
import com.entropybits.worknotes.spring_boot.dto.TagResponse;
import com.entropybits.worknotes.spring_boot.service.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 标签控制器 - 处理标签的 CRUD 操作
 */
@RestController
@RequestMapping("/v1/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    /**
     * 创建新标签
     */
    @PostMapping
    public ResponseEntity<TagResponse> createTag(
            @Valid @RequestBody TagRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        TagResponse response = tagService.createTag(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 更新标签
     */
    @PutMapping("/{id}")
    public ResponseEntity<TagResponse> updateTag(
            @PathVariable Long id,
            @Valid @RequestBody TagRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        TagResponse response = tagService.updateTag(id, request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * 删除标签
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTag(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        tagService.deleteTag(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    /**
     * 获取当前用户的所有标签
     */
    @GetMapping
    public ResponseEntity<List<TagResponse>> getUserTags(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<TagResponse> tags = tagService.getUserTags(userDetails.getUsername());
        return ResponseEntity.ok(tags);
    }

    /**
     * 根据 ID 获取标签
     */
    @GetMapping("/{id}")
    public ResponseEntity<TagResponse> getTagById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        TagResponse response = tagService.getTagById(id, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }
}
