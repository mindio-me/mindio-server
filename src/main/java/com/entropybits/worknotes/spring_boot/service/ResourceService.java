/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.service;

import com.entropybits.worknotes.spring_boot.dto.ResourceRequest;
import com.entropybits.worknotes.spring_boot.dto.ResourceResponse;
import com.entropybits.worknotes.spring_boot.entity.Resource;
import com.entropybits.worknotes.spring_boot.entity.User;
import com.entropybits.worknotes.spring_boot.exception.ResourceNotFoundException;
import com.entropybits.worknotes.spring_boot.repository.ResourceRepository;
import com.entropybits.worknotes.spring_boot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Resource 业务逻辑层
 */
@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ResourceResponse> getAllPublicResources() {
        return resourceRepository.findByIsPublicTrueOrderByDisplayOrderAsc()
                .stream()
                .map(ResourceResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ResourceResponse> getResourcesByCategory(String category) {
        return resourceRepository.findByCategoryAndIsPublicTrueOrderByDisplayOrderAsc(category)
                .stream()
                .map(ResourceResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ResourceResponse getResourceById(Long id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("资源不存在，ID: " + id));
        return ResourceResponse.fromEntity(resource);
    }

    @Transactional
    public ResourceResponse createResource(ResourceRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        Resource resource = Resource.builder()
                .name(request.getName())
                .description(request.getDescription())
                .url(request.getUrl())
                .icon(request.getIcon())
                .category(request.getCategory())
                .tags(request.getTags())
                .isPublic(request.getIsPublic())
                .isFeatured(request.getIsFeatured())
                .displayOrder(request.getDisplayOrder())
                .owner(user)
                .build();

        Resource savedResource = resourceRepository.save(resource);
        return ResourceResponse.fromEntity(savedResource);
    }

    @Transactional
    public ResourceResponse updateResource(Long id, ResourceRequest request, String username) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("资源不存在，ID: " + id));

        if (!resource.getOwner().getUsername().equals(username)) {
            throw new RuntimeException("没有权限修改此资源");
        }

        resource.setName(request.getName());
        resource.setDescription(request.getDescription());
        resource.setUrl(request.getUrl());
        resource.setIcon(request.getIcon());
        resource.setCategory(request.getCategory());
        resource.setTags(request.getTags());
        resource.setIsPublic(request.getIsPublic());
        resource.setIsFeatured(request.getIsFeatured());
        resource.setDisplayOrder(request.getDisplayOrder());

        Resource updatedResource = resourceRepository.save(resource);
        return ResourceResponse.fromEntity(updatedResource);
    }

    @Transactional
    public void deleteResource(Long id, String username) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("资源不存在，ID: " + id));

        if (!resource.getOwner().getUsername().equals(username)) {
            throw new RuntimeException("没有权限删除此资源");
        }

        resourceRepository.delete(resource);
    }
}
