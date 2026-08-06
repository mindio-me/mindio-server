/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.service;

import com.entropybits.worknotes.spring_boot.dto.ProjectRequest;
import com.entropybits.worknotes.spring_boot.dto.ProjectResponse;
import com.entropybits.worknotes.spring_boot.entity.Project;
import com.entropybits.worknotes.spring_boot.entity.User;
import com.entropybits.worknotes.spring_boot.exception.ResourceNotFoundException;
import com.entropybits.worknotes.spring_boot.repository.NoteRepository;
import com.entropybits.worknotes.spring_boot.repository.ProjectRepository;
import com.entropybits.worknotes.spring_boot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Project 业务逻辑层
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final NoteRepository noteRepository;

    /**
     * 获取所有公开项目
     */
    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllPublicProjects() {
        return projectRepository.findByIsPublicTrueOrderByDisplayOrderAsc()
                .stream()
                .map(ProjectResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有精选项目
     */
    @Transactional(readOnly = true)
    public List<ProjectResponse> getFeaturedProjects() {
        return projectRepository.findByIsFeaturedTrueOrderByDisplayOrderAsc()
                .stream()
                .map(ProjectResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 按分类获取公开项目
     */
    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjectsByCategory(String category) {
        return projectRepository.findByCategoryAndIsPublicTrueOrderByDisplayOrderAsc(category)
                .stream()
                .map(ProjectResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取项目
     */
    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("项目不存在，ID: " + id));
        return ProjectResponse.fromEntity(project);
    }

    /**
     * 获取当前用户的所有项目
     */
    @Transactional(readOnly = true)
    public List<ProjectResponse> getCurrentUserProjects(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        return projectRepository.findByOwnerOrderByDisplayOrderAsc(user)
                .stream()
                .map(ProjectResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 创建项目
     */
    @Transactional
    public ProjectResponse createProject(ProjectRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        Project project = Project.builder()
                .name(request.getName())
                .shortName(request.getShortName())
                .subtitle(request.getSubtitle())
                .description(request.getDescription())
                .icon(request.getIcon())
                .imageUrl(request.getImageUrl())
                .projectUrl(request.getProjectUrl())
                .githubUrl(request.getGithubUrl())
                .category(request.getCategory())
                .technologies(request.getTechnologies())
                .content(request.getContent())
                .contentType(request.getContentType() != null ? request.getContentType() : "richtext")
                .isPublic(request.getIsPublic())
                .isFeatured(request.getIsFeatured())
                .displayOrder(request.getDisplayOrder())
                .owner(user)
                .build();

        Project savedProject = projectRepository.save(project);
        return ProjectResponse.fromEntity(savedProject);
    }

    /**
     * 更新项目
     */
    @Transactional
    public ProjectResponse updateProject(Long id, ProjectRequest request, String username) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("项目不存在，ID: " + id));

        // 验证权限
        if (!project.getOwner().getUsername().equals(username)) {
            throw new RuntimeException("没有权限修改此项目");
        }

        project.setName(request.getName());
        project.setShortName(request.getShortName());
        project.setSubtitle(request.getSubtitle());
        project.setDescription(request.getDescription());
        project.setIcon(request.getIcon());
        project.setImageUrl(request.getImageUrl());
        project.setProjectUrl(request.getProjectUrl());
        project.setGithubUrl(request.getGithubUrl());
        project.setCategory(request.getCategory());
        project.setTechnologies(request.getTechnologies());
        project.setContent(request.getContent());
        project.setContentType(request.getContentType() != null ? request.getContentType() : "richtext");
        project.setIsPublic(request.getIsPublic());
        project.setIsFeatured(request.getIsFeatured());
        project.setDisplayOrder(request.getDisplayOrder());

        Project updatedProject = projectRepository.save(project);
        return ProjectResponse.fromEntity(updatedProject);
    }

    /**
     * 删除项目
     * 删除前会将所有关联该项目的文档的 project_id 设置为 NULL（级联删除逻辑）
     */
    @Transactional
    public void deleteProject(Long id, String username) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("项目不存在，ID: " + id));

        // 验证权限
        if (!project.getOwner().getUsername().equals(username)) {
            throw new RuntimeException("没有权限删除此项目");
        }

        // 级联删除：将所有关联该项目的文档的 project_id 设置为 NULL
        noteRepository.findByProject(project).forEach(note -> {
            note.setProject(null);
            noteRepository.save(note);
        });

        projectRepository.delete(project);
    }
}
