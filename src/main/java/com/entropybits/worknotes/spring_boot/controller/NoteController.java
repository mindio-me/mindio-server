/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.controller;

import com.entropybits.worknotes.spring_boot.dto.NoteRequest;
import com.entropybits.worknotes.spring_boot.dto.NoteResponse;
import com.entropybits.worknotes.spring_boot.dto.SearchRequest;
import com.entropybits.worknotes.spring_boot.service.NoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 笔记控制器 - 处理笔记的 CRUD 操作
 */
@RestController
@RequestMapping("/v1/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    /**
     * 创建新笔记
     */
    @PostMapping
    public ResponseEntity<NoteResponse> createNote(
            @Valid @RequestBody NoteRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        NoteResponse response = noteService.createNote(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 更新笔记
     */
    @PutMapping("/{id}")
    public ResponseEntity<NoteResponse> updateNote(
            @PathVariable Long id,
            @Valid @RequestBody NoteRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        NoteResponse response = noteService.updateNote(id, request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * 删除笔记
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNote(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        noteService.deleteNote(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    /**
     * 获取笔记详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<NoteResponse> getNoteById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        NoteResponse response = noteService.getNoteById(id, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * 获取当前用户的笔记列表（分页）
     * 支持按项目ID列表和标签ID列表筛选
     */
    @GetMapping
    public ResponseEntity<Page<NoteResponse>> getUserNotes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "modifiedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Set<Long> tagIds,
            @RequestParam(required = false) String projectIds, // 逗号分隔的项目ID列表
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<NoteResponse> notes;

        // 解析 projectIds 参数（逗号分隔的字符串）
        List<Long> projectIdList = null;
        if (projectIds != null && !projectIds.isEmpty()) {
            try {
                projectIdList = java.util.Arrays.stream(projectIds.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Long::parseLong)
                        .collect(java.util.stream.Collectors.toList());
            } catch (NumberFormatException e) {
                // 如果解析失败，忽略 projectIds 参数
            }
        }

        // 根据项目ID列表筛选
        if (projectIdList != null && !projectIdList.isEmpty()) {
            notes = noteService.getNotesByProjects(userDetails.getUsername(), projectIdList, pageable);
        }
        // 根据标签筛选
        else if (tagIds != null && !tagIds.isEmpty()) {
            notes = noteService.getNotesByTags(userDetails.getUsername(), tagIds, pageable);
        }
        // 根据关键词搜索
        else if (keyword != null && !keyword.isEmpty()) {
            notes = noteService.searchUserNotes(userDetails.getUsername(), keyword, pageable);
        }
        // 获取全部笔记
        else {
            notes = noteService.getUserNotes(userDetails.getUsername(), pageable);
        }

        return ResponseEntity.ok(notes);
    }

    /**
     * 获取所有公开笔记（无需认证）
     */
    @GetMapping("/public")
    public ResponseEntity<Page<NoteResponse>> getPublicNotes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "modifiedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<NoteResponse> notes = noteService.getPublicNotes(pageable);
        return ResponseEntity.ok(notes);
    }

    /**
     * 切换笔记公开状态（isPublic true/false 互相切换）
     */
    @PatchMapping("/{id}/toggle-public")
    public ResponseEntity<NoteResponse> togglePublic(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        NoteResponse response = noteService.togglePublic(id, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * 获取公开笔记详情（无需认证）
     */
    @GetMapping("/public/{id}")
    public ResponseEntity<NoteResponse> getPublicNoteById(@PathVariable Long id) {
        NoteResponse response = noteService.getPublicNoteById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 全文搜索：在标题、内容和摘要中搜索关键词
     */
    @GetMapping("/search")
    public ResponseEntity<Page<NoteResponse>> fullTextSearch(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "modifiedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<NoteResponse> notes = noteService.fullTextSearch(userDetails.getUsername(), keyword, pageable);
        return ResponseEntity.ok(notes);
    }

    /**
     * 多条件组合搜索
     */
    @PostMapping("/search/advanced")
    public ResponseEntity<Page<NoteResponse>> advancedSearch(
            @RequestBody SearchRequest searchRequest,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "modifiedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<NoteResponse> notes = noteService.advancedSearch(
                userDetails.getUsername(),
                searchRequest.getKeyword(),
                searchRequest.getIsPublic(),
                searchRequest.getTagIds(),
                searchRequest.getStartDate(),
                searchRequest.getEndDate(),
                pageable
        );
        return ResponseEntity.ok(notes);
    }
}
