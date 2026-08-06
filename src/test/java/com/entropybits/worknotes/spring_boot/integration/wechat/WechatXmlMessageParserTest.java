/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.wechat;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WechatXmlMessageParserTest {

    @Test
    void parsesTextMessage() {
        String xml = "<xml>"
            + "<ToUserName><![CDATA[gh_123]]></ToUserName>"
            + "<FromUserName><![CDATA[openid-abc]]></FromUserName>"
            + "<CreateTime>1700000000</CreateTime>"
            + "<MsgType><![CDATA[text]]></MsgType>"
            + "<Content><![CDATA[今天读到一篇好文章]]></Content>"
            + "<MsgId>1000000001</MsgId>"
            + "</xml>";

        WechatInboundMessage msg = WechatXmlMessageParser.parse(xml);

        assertThat(msg.msgType()).isEqualTo("text");
        assertThat(msg.fromUserName()).isEqualTo("openid-abc");
        assertThat(msg.toUserName()).isEqualTo("gh_123");
        assertThat(msg.content()).isEqualTo("今天读到一篇好文章");
        assertThat(msg.msgId()).isEqualTo("1000000001");
    }

    @Test
    void parsesLinkMessage() {
        String xml = "<xml>"
            + "<ToUserName><![CDATA[gh_123]]></ToUserName>"
            + "<FromUserName><![CDATA[openid-abc]]></FromUserName>"
            + "<CreateTime>1700000000</CreateTime>"
            + "<MsgType><![CDATA[link]]></MsgType>"
            + "<Title><![CDATA[一篇好文章]]></Title>"
            + "<Url><![CDATA[https://mp.weixin.qq.com/s/xyz]]></Url>"
            + "<MsgId>1000000002</MsgId>"
            + "</xml>";

        WechatInboundMessage msg = WechatXmlMessageParser.parse(xml);

        assertThat(msg.msgType()).isEqualTo("link");
        assertThat(msg.title()).isEqualTo("一篇好文章");
        assertThat(msg.url()).isEqualTo("https://mp.weixin.qq.com/s/xyz");
    }

    @Test
    void parsesImageMessage() {
        String xml = "<xml>"
            + "<ToUserName><![CDATA[gh_123]]></ToUserName>"
            + "<FromUserName><![CDATA[openid-abc]]></FromUserName>"
            + "<CreateTime>1700000000</CreateTime>"
            + "<MsgType><![CDATA[image]]></MsgType>"
            + "<PicUrl><![CDATA[http://mmbiz.qpic.cn/pic.jpg]]></PicUrl>"
            + "<MsgId>1000000003</MsgId>"
            + "</xml>";

        WechatInboundMessage msg = WechatXmlMessageParser.parse(xml);

        assertThat(msg.msgType()).isEqualTo("image");
        assertThat(msg.picUrl()).isEqualTo("http://mmbiz.qpic.cn/pic.jpg");
    }

    @Test
    void rejectsXmlWithDoctype() {
        String malicious = "<?xml version=\"1.0\"?><!DOCTYPE xml [<!ENTITY x SYSTEM \"file:///etc/passwd\">]>"
            + "<xml><Content>&x;</Content></xml>";

        assertThatThrownBy(() -> WechatXmlMessageParser.parse(malicious))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
