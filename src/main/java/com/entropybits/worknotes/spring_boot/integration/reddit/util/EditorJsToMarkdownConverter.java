/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.reddit.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Converts EditorJS JSON content to Reddit-flavored Markdown.
 */
@Component
@RequiredArgsConstructor
public class EditorJsToMarkdownConverter {

    private final ObjectMapper objectMapper;

    /**
     * Converts EditorJS JSON to Markdown string.
     * @param editorJsJson EditorJS content JSON
     * @param maxLength    truncate result to this many characters (0 = no limit)
     */
    public String convert(String editorJsJson, int maxLength) {
        if (editorJsJson == null || editorJsJson.isBlank()) return "";
        try {
            JsonNode blocks = objectMapper.readTree(editorJsJson).path("blocks");
            if (!blocks.isArray()) return "";

            StringBuilder sb = new StringBuilder();
            for (JsonNode block : blocks) {
                String type = block.path("type").asText();
                JsonNode data = block.path("data");
                String md = convertBlock(type, data);
                if (!md.isEmpty()) {
                    sb.append(md).append("\n\n");
                }
                if (maxLength > 0 && sb.length() >= maxLength) break;
            }

            String result = sb.toString().stripTrailing();
            if (maxLength > 0 && result.length() > maxLength) {
                result = result.substring(0, maxLength - 1) + "…";
            }
            return result;
        } catch (Exception e) {
            return "";
        }
    }

    private String convertBlock(String type, JsonNode data) {
        return switch (type) {
            case "header"    -> convertHeader(data);
            case "paragraph" -> convertParagraph(data);
            case "list"      -> convertList(data);
            case "code"      -> convertCode(data);
            case "quote"     -> convertQuote(data);
            case "delimiter" -> "---";
            case "image"     -> convertImage(data);
            default          -> "";
        };
    }

    private String convertHeader(JsonNode data) {
        int level = data.path("level").asInt(2);
        String text = stripHtml(data.path("text").asText(""));
        if (text.isBlank()) return "";
        return "#".repeat(Math.min(level, 6)) + " " + text;
    }

    private String convertParagraph(JsonNode data) {
        String text = inlineMarkdown(data.path("text").asText(""));
        return text.isBlank() ? "" : text;
    }

    private String convertList(JsonNode data) {
        boolean ordered = "ordered".equals(data.path("style").asText("unordered"));
        JsonNode items = data.path("items");
        if (!items.isArray() || items.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        int counter = 1;
        for (JsonNode item : items) {
            String text = item.isObject()
                    ? stripHtml(item.path("content").asText(""))
                    : stripHtml(item.asText(""));
            if (text.isBlank()) continue;
            if (ordered) {
                sb.append(counter++).append(". ").append(text).append("\n");
            } else {
                sb.append("- ").append(text).append("\n");
            }
        }
        return sb.toString().stripTrailing();
    }

    private String convertCode(JsonNode data) {
        String code = data.path("code").asText("");
        if (code.isBlank()) return "";
        return "```\n" + code + "\n```";
    }

    private String convertQuote(JsonNode data) {
        String text    = stripHtml(data.path("text").asText(""));
        String caption = stripHtml(data.path("caption").asText(""));
        if (text.isBlank()) return "";
        String result = "> " + text;
        if (!caption.isBlank()) result += "\n>\n> — " + caption;
        return result;
    }

    private String convertImage(JsonNode data) {
        String url     = data.path("file").path("url").asText("");
        String caption = stripHtml(data.path("caption").asText(""));
        if (url.isBlank()) return "";
        String label = caption.isBlank() ? "图片" : caption;
        return "[" + label + "](" + url + ")";
    }

    /** Strip HTML tags */
    private String stripHtml(String s) {
        if (s == null) return "";
        return s.replaceAll("<[^>]*>", "").trim();
    }

    /** Convert common inline HTML to Markdown equivalents */
    private String inlineMarkdown(String s) {
        if (s == null) return "";
        return s
                .replaceAll("<b>(.*?)</b>",      "**$1**")
                .replaceAll("<strong>(.*?)</strong>", "**$1**")
                .replaceAll("<i>(.*?)</i>",      "*$1*")
                .replaceAll("<em>(.*?)</em>",    "*$1*")
                .replaceAll("<code>(.*?)</code>", "`$1`")
                .replaceAll("<a[^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a>", "[$2]($1)")
                .replaceAll("<br\\s*/?>",        "\n")
                .replaceAll("<[^>]*>",           "")
                .trim();
    }
}
