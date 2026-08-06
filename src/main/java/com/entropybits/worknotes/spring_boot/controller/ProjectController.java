/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.controller;

import com.entropybits.worknotes.spring_boot.dto.ProjectRequest;
import com.entropybits.worknotes.spring_boot.dto.ProjectResponse;
import com.entropybits.worknotes.spring_boot.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Project API Controller
 */
@RestController
@RequestMapping("/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    /**
     * 获取所有公开项目
     * GET /api/v1/projects/public
     */
    @GetMapping("/public")
    public ResponseEntity<List<ProjectResponse>> getAllPublicProjects() {
        List<ProjectResponse> projects = projectService.getAllPublicProjects();
        return ResponseEntity.ok(projects);
    }

    /**
     * 获取所有精选项目
     * GET /api/v1/projects/featured
     */
    @GetMapping("/featured")
    public ResponseEntity<List<ProjectResponse>> getFeaturedProjects() {
        List<ProjectResponse> projects = projectService.getFeaturedProjects();
        return ResponseEntity.ok(projects);
    }

    /**
     * 按分类获取公开项目
     * GET /api/v1/projects/category/{category}
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<ProjectResponse>> getProjectsByCategory(@PathVariable String category) {
        List<ProjectResponse> projects = projectService.getProjectsByCategory(category);
        return ResponseEntity.ok(projects);
    }

    /**
     * 根据ID获取项目
     * GET /api/v1/projects/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable Long id) {
        ProjectResponse project = projectService.getProjectById(id);
        return ResponseEntity.ok(project);
    }

    /**
     * 获取当前用户的所有项目
     * GET /api/v1/projects/my
     */
    @GetMapping("/my")
    public ResponseEntity<List<ProjectResponse>> getMyProjects(Authentication authentication) {
        String username = authentication.getName();
        List<ProjectResponse> projects = projectService.getCurrentUserProjects(username);
        return ResponseEntity.ok(projects);
    }

    /**
     * 创建项目
     * POST /api/v1/projects
     */
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody ProjectRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        ProjectResponse project = projectService.createProject(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(project);
    }

    /**
     * 更新项目
     * PUT /api/v1/projects/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        ProjectResponse project = projectService.updateProject(id, request, username);
        return ResponseEntity.ok(project);
    }

    /**
     * 删除项目
     * DELETE /api/v1/projects/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable Long id,
            Authentication authentication) {
        String username = authentication.getName();
        projectService.deleteProject(id, username);
        return ResponseEntity.noContent().build();
    }
}
