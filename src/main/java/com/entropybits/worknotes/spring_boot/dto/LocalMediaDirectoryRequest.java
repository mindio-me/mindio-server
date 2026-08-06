/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LocalMediaDirectoryRequest {

    @NotBlank(message = "目录路径不能为空")
    @Size(max = 1000, message = "目录路径长度不能超过1000个字符")
    private String dirPath;

    @Size(max = 200, message = "显示名称长度不能超过200个字符")
    private String displayName;
}
