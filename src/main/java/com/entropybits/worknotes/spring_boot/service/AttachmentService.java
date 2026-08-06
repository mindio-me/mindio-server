/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.service;

import com.entropybits.worknotes.spring_boot.entity.Attachment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 附件管理服务接口
 */
public interface AttachmentService {

    /**
     * 获取附件列表（分页）
     *
     * @param pid 分类ID
     * @param attType 附件类型
     * @param pageable 分页参数
     * @return 附件列表
     */
    Page<Attachment> getList(Integer pid, String attType, Pageable pageable);

    /**
     * 根据ID获取附件
     *
     * @param id 附件ID
     * @return 附件
     */
    Attachment getById(Long id);

    /**
     * 删除附件
     *
     * @param ids 附件ID列表
     */
    void deleteByIds(List<Long> ids);

    /**
     * 给图片URL添加前缀
     *
     * @param path 路径
     * @return 带前缀的URL
     */
    String prefixImage(String path);

    /**
     * 给文件URL添加前缀
     *
     * @param path 路径
     * @return 带前缀的URL
     */
    String prefixFile(String path);

    /**
     * 清除URL前缀
     *
     * @param path 路径
     * @return 清除前缀后的路径
     */
    String clearPrefix(String path);
}

