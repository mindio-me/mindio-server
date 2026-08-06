/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.service;

import com.entropybits.worknotes.spring_boot.config.UploadPathConfig;
import com.entropybits.worknotes.spring_boot.dto.ClipImportUrlRequest;
import com.entropybits.worknotes.spring_boot.dto.SourceClipDraft;
import com.entropybits.worknotes.spring_boot.entity.SourceClip;
import com.entropybits.worknotes.spring_boot.utils.UploadUtil;
import lombok.extern.slf4j.Slf4j;
import net.dankito.readability4j.Article;
import net.dankito.readability4j.Readability4J;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@Slf4j
@Service
public class ClipImportService {

    @Autowired
    private UploadPathConfig uploadPathConfig;

    private static final int FETCH_TIMEOUT_MS = 10_000;
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
    private static final int MIN_CONTENT_TEXT_LENGTH = 100;

    // ------------------------------------------------------------------ //
    //  URL 导入
    // ------------------------------------------------------------------ //

    public SourceClipDraft fetchFromUrl(ClipImportUrlRequest request) {
        String url = request.getUrl().trim();
        boolean isWechat = url.contains("mp.weixin.qq.com");
        SourceClip.SourceType sourceType = isWechat
                ? SourceClip.SourceType.WECHAT_ARTICLE
                : SourceClip.SourceType.WEBPAGE;

        SourceClip.ExtractionMode mode = request.getExtractionMode() != null
                ? request.getExtractionMode() : SourceClip.ExtractionMode.FULL;

        if (mode == SourceClip.ExtractionMode.LINK_ONLY) {
            return fetchMetadataOnly(url, sourceType);
        }

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(FETCH_TIMEOUT_MS)
                    .get();

            doc.setBaseUri(url);
            String title    = extractTitle(doc, isWechat);
            String author   = extractAuthor(doc, isWechat);
            String content  = extractContent(doc, isWechat, url);
            if (content != null) content = rewriteImages(content, url, isWechat);

            if (content == null || content.isBlank()) {
                return SourceClipDraft.builder()
                        .sourceType(sourceType)
                        .extractionMode(SourceClip.ExtractionMode.FULL)
                        .extractionStatus(SourceClip.ExtractionStatus.FAILED)
                        .sourceUrl(url)
                        .sourceTitle(title)
                        .sourceAuthor(author)
                        .suggestedTitle(title)
                        .fetchSuccess(false)
                        .fetchFailReason("无法自动提取正文内容，将仅保存链接")
                        .build();
            }

            return SourceClipDraft.builder()
                    .sourceType(sourceType)
                    .extractionMode(SourceClip.ExtractionMode.FULL)
                    .extractionStatus(SourceClip.ExtractionStatus.SUCCESS)
                    .sourceUrl(url)
                    .sourceTitle(title)
                    .sourceAuthor(author)
                    .suggestedTitle(title)
                    .content(content)
                    .contentFormat("html")
                    .fetchSuccess(true)
                    .build();

        } catch (IOException e) {
            log.warn("Failed to fetch URL {}: {}", url, e.getMessage());
            return SourceClipDraft.builder()
                    .sourceType(sourceType)
                    .extractionMode(SourceClip.ExtractionMode.FULL)
                    .extractionStatus(SourceClip.ExtractionStatus.FAILED)
                    .sourceUrl(url)
                    .fetchSuccess(false)
                    .fetchFailReason("抓取失败：" + e.getMessage())
                    .build();
        }
    }

    /** "仅保存链接"模式：只取标题，不解析正文、不下载图片。 */
    public SourceClipDraft fetchMetadataOnly(String url, SourceClip.SourceType sourceType) {
        boolean isWechat = sourceType == SourceClip.SourceType.WECHAT_ARTICLE;
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(FETCH_TIMEOUT_MS)
                    .get();
            doc.setBaseUri(url);
            String title = extractTitle(doc, isWechat);
            return SourceClipDraft.builder()
                    .sourceType(sourceType)
                    .extractionMode(SourceClip.ExtractionMode.LINK_ONLY)
                    .sourceUrl(url)
                    .sourceTitle(title)
                    .suggestedTitle(title)
                    .fetchSuccess(true)
                    .build();
        } catch (IOException e) {
            log.warn("Failed to fetch metadata for URL {}: {}", url, e.getMessage());
            return SourceClipDraft.builder()
                    .sourceType(sourceType)
                    .extractionMode(SourceClip.ExtractionMode.LINK_ONLY)
                    .sourceUrl(url)
                    .suggestedTitle(url)
                    .fetchSuccess(true)
                    .build();
        }
    }

    // ------------------------------------------------------------------ //
    //  Private/package-private helpers — extraction
    // ------------------------------------------------------------------ //

    String extractTitle(Document doc, boolean isWechat) {
        if (isWechat) {
            Element el = doc.getElementById("activity-name");
            if (el != null && !el.text().isBlank()) return el.text().trim();
        }
        Element og = doc.selectFirst("meta[property=og:title]");
        if (og != null && !og.attr("content").isBlank()) return og.attr("content").trim();
        return doc.title();
    }

    String extractAuthor(Document doc, boolean isWechat) {
        if (isWechat) {
            Element el = doc.getElementById("js_name");
            if (el != null && !el.text().isBlank()) return el.text().trim();
        }
        Element og = doc.selectFirst("meta[name=author]");
        if (og != null && !og.attr("content").isBlank()) return og.attr("content").trim();
        return null;
    }

    String extractContent(Document doc, boolean isWechat, String baseUrl) {
        if (isWechat) {
            Element el = doc.getElementById("js_content");
            return el != null ? el.html() : null;
        }

        Article article = new Readability4J(baseUrl, doc).parse();
        String readable = article.getContent();
        if (readable != null && Jsoup.parseBodyFragment(readable).text().length() > MIN_CONTENT_TEXT_LENGTH) {
            return readable;
        }

        // Readability4J 判断内容不足时的二级兜底：常见正文选择器
        for (String selector : List.of("article", "[itemprop=articleBody]",
                ".post-content", ".entry-content", ".article-content", ".content")) {
            Element el = doc.selectFirst(selector);
            if (el != null && el.text().length() > MIN_CONTENT_TEXT_LENGTH) return el.html();
        }
        return null;
    }

    String rewriteImages(String html, String baseUrl, boolean isWechat) {
        Document frag = Jsoup.parseBodyFragment(html, baseUrl);
        for (Element img : frag.select("img")) {
            // 微信懒加载：data-src 才是真实图片地址
            if (isWechat) {
                String dataSrc = img.attr("data-src");
                if (!dataSrc.isBlank()) {
                    img.attr("src", dataSrc);
                }
            }
            // 相对路径转绝对路径
            String abs = img.absUrl("src");
            if (!abs.isBlank()) {
                img.attr("src", abs);
            }
            // 本地化图片，防止原站图片将来失效（不再仅限公众号）
            String imgSrc = img.attr("src");
            if (!imgSrc.isBlank()) {
                String localPath = downloadAndSaveImage(imgSrc, baseUrl);
                if (localPath != null) {
                    img.attr("src", localPath);
                }
            }
        }
        return frag.body().html();
    }

    public String downloadAndSaveImage(String imgUrl, String referrer) {
        try {
            Connection.Response resp = Jsoup.connect(imgUrl)
                    .userAgent(USER_AGENT)
                    .referrer(referrer)
                    .timeout(FETCH_TIMEOUT_MS)
                    .ignoreContentType(true)
                    .execute();

            byte[] bytes = resp.bodyAsBytes();
            if (bytes.length == 0) return null;

            String extName = resolveImageExt(imgUrl, resp.contentType());
            String webPath = UploadUtil.getWebPath(UploadUtil.UPLOAD_FILE_KEYWORD + "/public/clip_image/");
            String newFileName = UploadUtil.fileName(extName);
            String serverPath = UploadUtil.getServerPath(uploadPathConfig.getUploadPath(), webPath);

            File file = UploadUtil.createFile(serverPath + newFileName);
            Files.write(file.toPath(), bytes);

            return "/uploads/" + webPath + newFileName;
        } catch (Exception e) {
            log.warn("下载图片失败 {}: {}", imgUrl, e.getMessage());
            return null;
        }
    }

    private String resolveImageExt(String imgUrl, String contentType) {
        if (contentType != null) {
            if (contentType.contains("png"))  return "png";
            if (contentType.contains("gif"))  return "gif";
            if (contentType.contains("webp")) return "webp";
            if (contentType.contains("jpeg") || contentType.contains("jpg")) return "jpg";
        }
        String path = imgUrl.contains("?") ? imgUrl.substring(0, imgUrl.indexOf('?')) : imgUrl;
        String ext = UploadUtil.getFileExtension(path);
        if (!ext.isBlank() && ext.length() <= 4) return ext;
        if (imgUrl.contains("wx_fmt=png"))  return "png";
        if (imgUrl.contains("wx_fmt=gif"))  return "gif";
        return "jpg";
    }
}
