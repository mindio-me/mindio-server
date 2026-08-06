/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.controller;

import com.entropybits.worknotes.spring_boot.dto.ResourceRequest;
import com.entropybits.worknotes.spring_boot.dto.ResourceResponse;
import com.entropybits.worknotes.spring_boot.service.ResourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Resource API Controller
 */
@RestController
@RequestMapping("/v1/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    /**
     * 获取所有公开资源
     * GET /api/v1/resources
     */
    @GetMapping
    public ResponseEntity<List<ResourceResponse>> getAllPublicResources() {
        List<ResourceResponse> resources = resourceService.getAllPublicResources();
        return ResponseEntity.ok(resources);
    }

    /**
     * 按分类获取资源
     * GET /api/v1/resources/category/{category}
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<ResourceResponse>> getResourcesByCategory(@PathVariable String category) {
        List<ResourceResponse> resources = resourceService.getResourcesByCategory(category);
        return ResponseEntity.ok(resources);
    }

    /**
     * 根据ID获取资源
     * GET /api/v1/resources/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResourceResponse> getResourceById(@PathVariable Long id) {
        ResourceResponse resource = resourceService.getResourceById(id);
        return ResponseEntity.ok(resource);
    }

    /**
     * 创建资源
     * POST /api/v1/resources
     */
    @PostMapping
    public ResponseEntity<ResourceResponse> createResource(
            @Valid @RequestBody ResourceRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        ResourceResponse resource = resourceService.createResource(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(resource);
    }

    /**
     * 更新资源
     * PUT /api/v1/resources/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ResourceResponse> updateResource(
            @PathVariable Long id,
            @Valid @RequestBody ResourceRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        ResourceResponse resource = resourceService.updateResource(id, request, username);
        return ResponseEntity.ok(resource);
    }

    /**
     * 删除资源
     * DELETE /api/v1/resources/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResource(
            @PathVariable Long id,
            Authentication authentication) {
        String username = authentication.getName();
        resourceService.deleteResource(id, username);
        return ResponseEntity.noContent().build();
    }
}
