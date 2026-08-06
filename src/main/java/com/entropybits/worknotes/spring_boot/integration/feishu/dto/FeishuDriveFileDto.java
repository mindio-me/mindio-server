/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.feishu.dto;

/**
 * 飞书云空间文件 DTO
 *
 * @param token        文件/文件夹 token
 * @param name         名称
 * @param type         类型：folder/docx/doc/sheet/bitable 等
 * @param parentToken  父文件夹 token
 * @param isFolder     是否为文件夹
 * @param canImport    是否可导入（docx/doc 类型为 true）
 * @param url          文件 URL
 * @param createdTime  创建时间（Unix 秒级时间戳字符串，来自飞书 API）
 * @param modifiedTime 修改时间（Unix 秒级时间戳字符串，来自飞书 API）
 */
public record FeishuDriveFileDto(
        String token,
        String name,
        String type,
        String parentToken,
        boolean isFolder,
        boolean canImport,
        String url,
        String createdTime,
        String modifiedTime
) {
}
