/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.wechat.dto;

import com.entropybits.worknotes.spring_boot.integration.wechat.entity.WechatBinding;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class WechatBindingResponse {
    private Long id;
    private String status;
    private LocalDateTime boundAt;
    private LocalDateTime createdAt;

    public static WechatBindingResponse fromEntity(WechatBinding binding) {
        return WechatBindingResponse.builder()
                .id(binding.getId())
                .status(binding.getStatus().name())
                .boundAt(binding.getBoundAt())
                .createdAt(binding.getCreatedAt())
                .build();
    }
}
