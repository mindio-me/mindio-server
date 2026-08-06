/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.dto;

import lombok.Data;

/**
 * 文件上传结果对象
 */
@Data
public class FileResultVo {

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 扩展名
     */
    private String extName;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 可供访问的URL（包含域名）
     */
    private String url;

    /**
     * 文件类型
     */
    private String type;
}

