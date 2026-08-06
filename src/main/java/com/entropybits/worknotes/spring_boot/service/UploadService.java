/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.service;

import com.entropybits.worknotes.spring_boot.dto.FileResultVo;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传服务接口
 */
public interface UploadService {

    /**
     * 图片上传
     *
     * @param multipartFile 文件
     * @param model 模块（如：note, user等）
     * @param pid 分类ID（0-编辑器, 1-笔记图片, 2-用户头像, 3-其他）
     * @return FileResultVo
     */
    FileResultVo imageUpload(MultipartFile multipartFile, String model, Integer pid);

    /**
     * 图片上传（指定所有者）
     *
     * @param multipartFile 文件
     * @param model 模块
     * @param pid 分类ID
     * @param ownerId 所有者ID（用户ID）
     * @return FileResultVo
     */
    FileResultVo imageUpload(MultipartFile multipartFile, String model, Integer pid, Long ownerId);

    /**
     * 文件上传
     *
     * @param multipartFile 文件
     * @param model 模块
     * @param pid 分类ID
     * @return FileResultVo
     */
    FileResultVo fileUpload(MultipartFile multipartFile, String model, Integer pid);

    /**
     * 文件上传（指定所有者）
     *
     * @param multipartFile 文件
     * @param model 模块
     * @param pid 分类ID
     * @param ownerId 所有者ID
     * @return FileResultVo
     */
    FileResultVo fileUpload(MultipartFile multipartFile, String model, Integer pid, Long ownerId);

    /**
     * Base64图片上传
     *
     * @param base64 Base64编码的图片数据
     * @param model 模块
     * @param pid 分类ID
     * @return FileResultVo
     */
    FileResultVo base64Upload(String base64, String model, Integer pid);

    /**
     * 网络链接上传：服务端从 URL 拉取并保存为附件
     *
     * @param url 远程文件URL
     * @param model 模块
     * @param pid 分类ID
     * @param ownerId 所有者ID
     * @return FileResultVo
     */
    FileResultVo remoteUpload(String url, String model, Integer pid, Long ownerId);
}

