/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.service;

import com.entropybits.worknotes.spring_boot.dto.TagRequest;
import com.entropybits.worknotes.spring_boot.dto.TagResponse;
import com.entropybits.worknotes.spring_boot.entity.Note;
import com.entropybits.worknotes.spring_boot.entity.Tag;
import com.entropybits.worknotes.spring_boot.entity.User;
import com.entropybits.worknotes.spring_boot.exception.BadRequestException;
import com.entropybits.worknotes.spring_boot.exception.ResourceNotFoundException;
import com.entropybits.worknotes.spring_boot.exception.UnauthorizedException;
import com.entropybits.worknotes.spring_boot.repository.NoteRepository;
import com.entropybits.worknotes.spring_boot.repository.TagRepository;
import com.entropybits.worknotes.spring_boot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 标签服务
 */
@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final NoteRepository noteRepository;

    /**
     * 创建标签
     */
    @Transactional
    public TagResponse createTag(TagRequest request, String username) {
        User user = getUserByUsername(username);

        // 检查标签名称是否已存在（同一用户下）
        if (tagRepository.existsByNameAndOwner(request.getName(), user)) {
            throw new BadRequestException("标签名称已存在");
        }

        Tag tag = Tag.builder()
                .name(request.getName())
                .owner(user)
                .build();

        Tag savedTag = tagRepository.save(tag);
        return TagResponse.fromEntity(savedTag);
    }

    /**
     * 更新标签
     */
    @Transactional
    public TagResponse updateTag(Long tagId, TagRequest request, String username) {
        Tag tag = getTagById(tagId);
        User user = getUserByUsername(username);

        // 检查权限
        if (!tag.getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedException("无权限修改此标签");
        }

        // 检查新名称是否已存在（排除当前标签）
        tagRepository.findByNameAndOwner(request.getName(), user)
                .ifPresent(existingTag -> {
                    if (!existingTag.getId().equals(tagId)) {
                        throw new BadRequestException("标签名称已存在");
                    }
                });

        tag.setName(request.getName());
        Tag updatedTag = tagRepository.save(tag);
        return TagResponse.fromEntity(updatedTag);
    }

    /**
     * 删除标签
     */
    @Transactional
    public void deleteTag(Long tagId, String username) {
        Tag tag = getTagById(tagId);
        User user = getUserByUsername(username);

        // 检查权限
        if (!tag.getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedException("无权限删除此标签");
        }

        // 先清除所有笔记与该标签的关联（解决外键约束问题）
        for (Note note : tag.getNotes()) {
            note.getTags().remove(tag);
            noteRepository.save(note);
        }
        tag.getNotes().clear();

        tagRepository.delete(tag);
    }

    /**
     * 获取用户的所有标签
     */
    @Transactional(readOnly = true)
    public List<TagResponse> getUserTags(String username) {
        User user = getUserByUsername(username);
        List<Tag> tags = tagRepository.findByOwner(user);
        return tags.stream()
                .map(TagResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 根据 ID 获取标签
     */
    @Transactional(readOnly = true)
    public TagResponse getTagById(Long tagId, String username) {
        Tag tag = getTagById(tagId);
        User user = getUserByUsername(username);

        // 检查权限
        if (!tag.getOwner().getId().equals(user.getId())) {
            throw new UnauthorizedException("无权限查看此标签");
        }

        return TagResponse.fromEntity(tag);
    }

    // ========== 辅助方法 ==========

    private Tag getTagById(Long tagId) {
        return tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("标签", "id", tagId));
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户", "username", username));
    }
}
