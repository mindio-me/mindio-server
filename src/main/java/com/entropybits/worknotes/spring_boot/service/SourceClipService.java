/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.service;

import com.entropybits.worknotes.spring_boot.dto.SourceClipRequest;
import com.entropybits.worknotes.spring_boot.dto.SourceClipResponse;
import com.entropybits.worknotes.spring_boot.entity.SourceClip;
import com.entropybits.worknotes.spring_boot.entity.Tag;
import com.entropybits.worknotes.spring_boot.entity.User;
import com.entropybits.worknotes.spring_boot.exception.ResourceNotFoundException;
import com.entropybits.worknotes.spring_boot.repository.SourceClipRepository;
import com.entropybits.worknotes.spring_boot.repository.TagRepository;
import com.entropybits.worknotes.spring_boot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SourceClipService {

    private static final int EXCERPT_MAX_LEN = 300;

    private final SourceClipRepository clipRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;

    @Transactional
    public SourceClipResponse createClip(SourceClipRequest request, String username) {
        User user = getUser(username);
        Set<Tag> tags = resolveTags(request.getTagIds(), user);

        SourceClip clip = SourceClip.builder()
                .sourceType(request.getSourceType())
                .sourceUrl(request.getSourceUrl())
                .sourceTitle(request.getSourceTitle())
                .sourceAuthor(request.getSourceAuthor())
                .extractionMode(request.getExtractionMode() != null
                        ? request.getExtractionMode() : SourceClip.ExtractionMode.FULL)
                .extractionStatus(request.getExtractionStatus())
                .title(request.getTitle())
                .content(request.getContent())
                .contentFormat(request.getContentFormat())
                .excerpt(buildExcerpt(request.getContent()))
                .tags(tags)
                .owner(user)
                .build();

        return SourceClipResponse.fromEntity(clipRepository.save(clip));
    }

    @Transactional(readOnly = true)
    public Page<SourceClipResponse> listClips(String username, String keyword,
                                              SourceClip.SourceType sourceType, Pageable pageable) {
        User user = getUser(username);
        Page<SourceClip> page;
        if (keyword != null && !keyword.isBlank() || sourceType != null) {
            String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
            page = clipRepository.searchByOwnerAndType(user, kw, sourceType, pageable);
        } else {
            page = clipRepository.findByOwner(user, pageable);
        }
        return page.map(SourceClipResponse::fromEntitySummary);
    }

    @Transactional(readOnly = true)
    public SourceClipResponse getClip(Long id, String username) {
        SourceClip clip = findClip(id);
        return SourceClipResponse.fromEntity(clip);
    }

    @Transactional
    public SourceClipResponse updateClip(Long id, SourceClipRequest request, String username) {
        User user = getUser(username);
        SourceClip clip = findClip(id);
        Set<Tag> tags = resolveTags(request.getTagIds(), user);

        clip.setSourceType(request.getSourceType());
        clip.setSourceUrl(request.getSourceUrl());
        clip.setSourceTitle(request.getSourceTitle());
        clip.setSourceAuthor(request.getSourceAuthor());
        clip.setExtractionMode(request.getExtractionMode() != null
                ? request.getExtractionMode() : SourceClip.ExtractionMode.FULL);
        clip.setExtractionStatus(request.getExtractionStatus());
        clip.setTitle(request.getTitle());
        clip.setContent(request.getContent());
        clip.setContentFormat(request.getContentFormat());
        clip.setExcerpt(buildExcerpt(request.getContent()));
        clip.setTags(tags);

        return SourceClipResponse.fromEntity(clipRepository.save(clip));
    }

    @Transactional
    public SourceClipResponse updateTitle(Long id, String title) {
        SourceClip clip = findClip(id);
        clip.setTitle(title);
        return SourceClipResponse.fromEntitySummary(clipRepository.save(clip));
    }

    @Transactional
    public void deleteClip(Long id, String username) {
        SourceClip clip = findClip(id);
        // NoteClipRef 有 ON DELETE CASCADE，直接删除即可
        clipRepository.delete(clip);
    }

    // ---- helpers ----

    private SourceClip findClip(Long id) {
        return clipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("素材不存在"));
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
    }

    private Set<Tag> resolveTags(List<Long> tagIds, User user) {
        if (tagIds == null || tagIds.isEmpty()) return new HashSet<>();
        return new HashSet<>(tagRepository.findAllById(tagIds));
    }

    /** 从 HTML/文本中提取纯文本 excerpt */
    static String buildExcerpt(String content) {
        if (content == null || content.isBlank()) return "";
        // 去除 HTML 标签
        String plain = content.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
        if (plain.length() <= EXCERPT_MAX_LEN) return plain;
        return plain.substring(0, EXCERPT_MAX_LEN - 1) + "…";
    }
}
