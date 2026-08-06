/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.wechat;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

public final class WechatXmlMessageParser {

    private WechatXmlMessageParser() {}

    public static WechatInboundMessage parse(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // 防 XXE：禁止 DOCTYPE 声明（微信消息体本就不含 DOCTYPE，命中即视为异常输入）
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));
            doc.getDocumentElement().normalize();
            Element root = doc.getDocumentElement();

            return new WechatInboundMessage(
                tag(root, "MsgType"),
                tag(root, "FromUserName"),
                tag(root, "ToUserName"),
                tag(root, "MsgId"),
                tag(root, "Content"),
                tag(root, "Url"),
                tag(root, "Title"),
                tag(root, "PicUrl")
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("解析微信消息 XML 失败: " + e.getMessage(), e);
        }
    }

    private static String tag(Element root, String tagName) {
        NodeList nodes = root.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) return null;
        String text = nodes.item(0).getTextContent();
        return text != null ? text.trim() : null;
    }
}
