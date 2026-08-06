/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.service.impl;

import com.entropybits.worknotes.spring_boot.entity.Attachment;
import com.entropybits.worknotes.spring_boot.exception.ResourceNotFoundException;
import com.entropybits.worknotes.spring_boot.repository.AttachmentRepository;
import com.entropybits.worknotes.spring_boot.service.AttachmentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 附件管理服务实现类
 */
@Service
public class AttachmentServiceImpl implements AttachmentService {

    @Value("${worknotes.upload.url-prefix:}")
    private String uploadUrlPrefix;

    private final AttachmentRepository attachmentRepository;

    public AttachmentServiceImpl(AttachmentRepository attachmentRepository) {
        this.attachmentRepository = attachmentRepository;
    }

    @Override
    public Page<Attachment> getList(Integer pid, String attType, Pageable pageable) {
        if (pid != null && attType != null && !attType.isEmpty()) {
            return attachmentRepository.findByPidAndAttType(pid, attType, pageable);
        } else if (pid != null) {
            return attachmentRepository.findByPid(pid, pageable);
        } else {
            return attachmentRepository.findAll(pageable);
        }
    }

    @Override
    public Attachment getById(Long id) {
        return attachmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("附件不存在: " + id));
    }

    @Override
    @Transactional
    public void deleteByIds(List<Long> ids) {
        attachmentRepository.deleteByAttIdIn(ids);
    }

    @Override
    public String prefixImage(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        if (uploadUrlPrefix != null && !uploadUrlPrefix.isEmpty() && !path.startsWith("http")) {
            return uploadUrlPrefix + (path.startsWith("/") ? path : "/" + path);
        }
        return path;
    }

    @Override
    public String prefixFile(String path) {
        return prefixImage(path);
    }

    @Override
    public String clearPrefix(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        if (uploadUrlPrefix != null && !uploadUrlPrefix.isEmpty() && path.startsWith(uploadUrlPrefix)) {
            return path.substring(uploadUrlPrefix.length());
        }
        return path;
    }
}

