/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

/**
 * 网络文件上传请求：服务端从 URL 拉取并保存
 */
@Data
public class RemoteUploadRequest {

    @NotBlank(message = "url不能为空")
    @URL(message = "url格式不正确")
    private String url;

    @NotBlank(message = "model不能为空")
    private String model;

    private Integer pid = 0;
}


