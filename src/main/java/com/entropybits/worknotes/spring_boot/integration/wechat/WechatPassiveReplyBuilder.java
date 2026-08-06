/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.wechat;

import java.time.Instant;

public final class WechatPassiveReplyBuilder {

    private WechatPassiveReplyBuilder() {}

    /**
     * 构建文本被动回复 XML。
     * toUser/fromUser 相对原始消息是对调的：回复的接收方是原消息发送者，回复的发送方是公众号自己。
     */
    public static String text(String toUser, String fromUser, String content) {
        return "<xml>"
            + "<ToUserName><![CDATA[" + toUser + "]]></ToUserName>"
            + "<FromUserName><![CDATA[" + fromUser + "]]></FromUserName>"
            + "<CreateTime>" + Instant.now().getEpochSecond() + "</CreateTime>"
            + "<MsgType><![CDATA[text]]></MsgType>"
            + "<Content><![CDATA[" + content + "]]></Content>"
            + "</xml>";
    }
}
