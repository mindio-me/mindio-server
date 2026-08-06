/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.wechat;

public record WechatInboundMessage(
    String msgType,
    String fromUserName,
    String toUserName,
    String msgId,
    String content,
    String url,
    String title,
    String picUrl
) {}
