/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Base64图片上传请求对象
 */
@Data
public class BaseUploadRequest {

    /**
     * Base64编码的图片数据
     */
    @NotBlank(message = "Base64数据不能为空")
    private String base64Url;

    /**
     * 模块名称（如：note, user等）
     */
    @NotBlank(message = "模块名称不能为空")
    private String model;

    /**
     * 分类ID
     * 0-编辑器, 1-笔记图片, 2-用户头像, 3-其他
     */
    private Integer pid = 0;
}

