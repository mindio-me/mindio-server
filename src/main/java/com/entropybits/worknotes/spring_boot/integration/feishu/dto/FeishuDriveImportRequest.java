/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.feishu.dto;

import lombok.Data;

/**
 * 云空间文档导入请求
 */
@Data
public class FeishuDriveImportRequest {
    /**
     * 文件 token
     */
    private String fileToken;

    /**
     * 文件名称
     */
    private String fileName;

    /**
     * 文件类型：docx/doc
     */
    private String fileType;

    /**
     * 文件 URL
     */
    private String fileUrl;

    /**
     * 创建时间（Unix 秒级时间戳字符串，来自飞书 API）
     */
    private String createdTime;

    /**
     * 修改时间（Unix 秒级时间戳字符串，来自飞书 API）
     */
    private String modifiedTime;
}
