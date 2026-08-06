/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.service;

import com.entropybits.worknotes.spring_boot.entity.ImportItem;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Component
public class BookmarkHtmlParser {

    public List<ParsedBookmark> parse(String html) {
        Document doc = Jsoup.parse(html);
        List<ParsedBookmark> result = new ArrayList<>();
        for (Element a : doc.select("a[href]")) {
            String href = a.attr("href").trim();
            if (href.isEmpty()) continue;
            String title = a.text().trim();
            LocalDateTime addedAt = parseAddDate(a.attr("add_date"));
            String folderPath = resolveFolderPath(a);
            result.add(new ParsedBookmark(title, href, addedAt, folderPath));
        }
        return result;
    }

    /** 非 http(s) scheme 视为噪音；javascript: 单独标记为书签脚本，其余（chrome:/edge:/about:/file: 等）标记为内部链接 */
    public ImportItem.NoiseReason isNoise(String url) {
        String lower = url.trim().toLowerCase();
        if (lower.startsWith("javascript:")) return ImportItem.NoiseReason.JS_BOOKMARKLET;
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return ImportItem.NoiseReason.INTERNAL_SCHEME;
        }
        return null;
    }

    private LocalDateTime parseAddDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return LocalDateTime.ofEpochSecond(Long.parseLong(raw.trim()), 0, ZoneOffset.UTC);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Netscape 书签格式里，文件夹标题 <H3> 和它对应的链接列表 <DL> 是同级的兄弟节点
     * （<DT><H3>Folder</H3> 紧接着 <DL><p>...</DL>）。逐层往外找祖先 <dl>，
     * 每层取它的前一个兄弟节点里的 <h3> 文本作为一段路径，从外到内拼接。
     */
    private String resolveFolderPath(Element anchor) {
        List<String> segments = new ArrayList<>();
        Element dl = anchor.closest("dl");
        while (dl != null) {
            Element sibling = dl.previousElementSibling();
            Element h3 = null;
            if (sibling != null) {
                h3 = "h3".equalsIgnoreCase(sibling.tagName()) ? sibling : sibling.selectFirst("h3");
            }
            if (h3 != null) segments.add(0, h3.text().trim());

            Element parent = dl.parent();
            Element nextDl = (parent != null) ? parent.closest("dl") : null;
            if (nextDl == dl) break; // 到达根节点，避免死循环
            dl = nextDl;
        }
        return segments.isEmpty() ? null : String.join("/", segments);
    }
}
