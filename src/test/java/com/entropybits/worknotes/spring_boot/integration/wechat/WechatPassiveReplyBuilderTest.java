/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.wechat;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WechatPassiveReplyBuilderTest {

    @Test
    void buildsTextReplyXmlWithSwappedToFrom() {
        String xml = WechatPassiveReplyBuilder.text("openid-abc", "gh_123", "已保存《测试》");

        assertThat(xml).contains("<ToUserName><![CDATA[openid-abc]]></ToUserName>");
        assertThat(xml).contains("<FromUserName><![CDATA[gh_123]]></FromUserName>");
        assertThat(xml).contains("<MsgType><![CDATA[text]]></MsgType>");
        assertThat(xml).contains("<Content><![CDATA[已保存《测试》]]></Content>");
        assertThat(xml).contains("<CreateTime>");
    }
}
