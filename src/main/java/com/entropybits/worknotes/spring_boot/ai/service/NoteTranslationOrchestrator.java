/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.ai.service;

import com.entropybits.worknotes.spring_boot.ai.config.AiProperties;
import com.entropybits.worknotes.spring_boot.ai.dto.TranslationRequest;
import com.entropybits.worknotes.spring_boot.entity.Note;
import com.entropybits.worknotes.spring_boot.integration.wechat.EditorJsToHtmlConverter;
import com.entropybits.worknotes.spring_boot.repository.NoteRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class NoteTranslationOrchestrator {

    private final AiProperties aiProperties;
    private final AiTranslationService anthropicService;
    private final AiTranslationService openAiService;
    private final AiTranslationService deepseekService;
    private final AiTranslationService doubaoService;
    private final NoteRepository noteRepository;
    private final ObjectMapper objectMapper;
    private final EditorJsToHtmlConverter editorJsConverter;

    public NoteTranslationOrchestrator(
            AiProperties aiProperties,
            @Qualifier("anthropicTranslationService") AiTranslationService anthropicService,
            @Qualifier("openAiTranslationService") AiTranslationService openAiService,
            @Qualifier("deepseekTranslationService") AiTranslationService deepseekService,
            @Qualifier("doubaoTranslationService") AiTranslationService doubaoService,
            NoteRepository noteRepository,
            ObjectMapper objectMapper,
            EditorJsToHtmlConverter editorJsConverter) {
        this.aiProperties = aiProperties;
        this.anthropicService = anthropicService;
        this.openAiService = openAiService;
        this.deepseekService = deepseekService;
        this.doubaoService = doubaoService;
        this.noteRepository = noteRepository;
        this.objectMapper = objectMapper;
        this.editorJsConverter = editorJsConverter;
    }

    public List<String> generateLinkedInHashtags(Note note) throws Exception {
        AiTranslationService ai = resolveService();
        String bodyText = editorJsConverter.extractPlainText(note.getContent(), 3000);
        return ai.generateLinkedInHashtags(
            note.getTitle() != null ? note.getTitle() : "",
            bodyText
        );
    }

    @Transactional
    public Note translate(Note sourceNote, TranslationRequest request) throws Exception {
        AiTranslationService ai = resolveService();
        String targetLang = request.getTargetLanguage() != null ? request.getTargetLanguage() : "en";

        if ("LINKEDIN".equalsIgnoreCase(request.getMode())) {
            return linkedInRewrite(sourceNote, ai, targetLang);
        } else {
            return faithfulTranslate(sourceNote, ai, targetLang);
        }
    }

    // ── FAITHFUL ────────────────────────────────────────────────────────────────

    private Note faithfulTranslate(Note sourceNote, AiTranslationService ai, String targetLang) throws Exception {
        String content = sourceNote.getContent();
        if (content == null || content.isBlank()) {
            content = "{\"blocks\":[]}";
        }

        JsonNode root = objectMapper.readTree(content);
        JsonNode blocksNode = root.path("blocks");

        // Collect (blockIndex, fieldPath, text) tuples
        List<int[]> indices = new ArrayList<>();   // [blockIndex, subIndex]  subIndex=-1 for single text fields
        List<String> texts = new ArrayList<>();

        if (blocksNode.isArray()) {
            for (int i = 0; i < blocksNode.size(); i++) {
                JsonNode block = blocksNode.get(i);
                String type = block.path("type").asText();
                JsonNode data = block.path("data");
                log.debug("Block[{}] type={}", i, type);

                switch (type) {
                    case "paragraph":
                    case "header":
                    case "quote": {
                        String text = data.path("text").asText("");
                        if (!text.isBlank()) {
                            indices.add(new int[]{i, -1});
                            texts.add(stripHtml(text));
                        }
                        // quote caption
                        if ("quote".equals(type)) {
                            String caption = data.path("caption").asText("");
                            if (!caption.isBlank()) {
                                indices.add(new int[]{i, -2});
                                texts.add(stripHtml(caption));
                            }
                        }
                        break;
                    }
                    case "list": {
                        JsonNode items = data.path("items");
                        if (items.isArray()) {
                            for (int j = 0; j < items.size(); j++) {
                                JsonNode itemNode = items.get(j);
                                // 兼容新版 EditorJS list：item 为 {content: "...", items: [...]}
                                // 旧版 item 为纯字符串
                                String item = itemNode.isObject()
                                    ? itemNode.path("content").asText("")
                                    : itemNode.asText("");
                                if (!item.isBlank()) {
                                    indices.add(new int[]{i, j});
                                    texts.add(stripHtml(item));
                                }
                            }
                        }
                        break;
                    }
                    default:
                        log.debug("Block[{}] type={} skipped (not translatable)", i, type);
                        break;
                }
            }
        }
        log.info("Collected {} text segments from {} blocks for translation", texts.size(), blocksNode.size());

        // Translate title + all block texts in one shot (title is first element)
        List<String> allTexts = new ArrayList<>();
        allTexts.add(sourceNote.getTitle());
        allTexts.addAll(texts);

        log.info("Sending {} texts to AI (title + {} segments)", allTexts.size(), texts.size());
        List<String> allTranslated = ai.translateTexts(allTexts, targetLang);
        log.info("AI returned {} translated texts", allTranslated.size());
        for (int i = 0; i < allTranslated.size(); i++) {
            log.info("  [{}] {}", i, allTranslated.get(i));
        }

        String translatedTitle = allTranslated.get(0);
        List<String> translatedTexts = allTranslated.subList(1, allTranslated.size());

        // Re-build blocks JSON with translated text
        ObjectNode newRoot = objectMapper.createObjectNode();
        if (root.has("time")) newRoot.set("time", root.get("time"));
        if (root.has("version")) newRoot.set("version", root.get("version"));

        ArrayNode newBlocks = newRoot.putArray("blocks");
        if (blocksNode.isArray()) {
            for (int i = 0; i < blocksNode.size(); i++) {
                JsonNode block = blocksNode.get(i);
                newBlocks.add(translateBlock(block, i, indices, translatedTexts));
            }
        }

        String translatedContent = objectMapper.writeValueAsString(newRoot);

        Note translated = Note.builder()
            .title("[EN] " + translatedTitle)
            .content(translatedContent)
            .contentType(sourceNote.getContentType())
            .owner(sourceNote.getOwner())
            .isPublic(false)
            .language(targetLang)
            .sourceNote(sourceNote)
            .build();

        return noteRepository.save(translated);
    }

    private JsonNode translateBlock(JsonNode block, int blockIdx,
                                     List<int[]> indices, List<String> translated) {
        String type = block.path("type").asText();
        ObjectNode newBlock = block.deepCopy();
        ObjectNode data = (ObjectNode) newBlock.get("data");
        if (data == null) return newBlock;

        switch (type) {
            case "paragraph":
            case "header": {
                String tr = findTranslated(blockIdx, -1, indices, translated);
                if (tr != null) data.put("text", tr);
                break;
            }
            case "quote": {
                String tr = findTranslated(blockIdx, -1, indices, translated);
                if (tr != null) data.put("text", tr);
                String cap = findTranslated(blockIdx, -2, indices, translated);
                if (cap != null) data.put("caption", cap);
                break;
            }
            case "list": {
                JsonNode items = block.path("data").path("items");
                if (items.isArray()) {
                    ArrayNode newItems = data.putArray("items");
                    for (int j = 0; j < items.size(); j++) {
                        JsonNode origItem = items.get(j);
                        String tr = findTranslated(blockIdx, j, indices, translated);
                        if (origItem.isObject()) {
                            // 新版 EditorJS list：item 是 {content: "...", items: [...]}
                            ObjectNode newItem = origItem.deepCopy();
                            if (tr != null) newItem.put("content", tr);
                            newItems.add(newItem);
                        } else {
                            // 旧版：item 是纯字符串
                            newItems.add(tr != null ? tr : origItem.asText());
                        }
                    }
                }
                break;
            }
            default:
                break;
        }
        return newBlock;
    }

    private String findTranslated(int blockIdx, int subIdx, List<int[]> indices, List<String> translated) {
        for (int k = 0; k < indices.size(); k++) {
            int[] entry = indices.get(k);
            if (entry[0] == blockIdx && entry[1] == subIdx) {
                return k < translated.size() ? translated.get(k) : null;
            }
        }
        return null;
    }

    // ── LINKEDIN ────────────────────────────────────────────────────────────────

    private Note linkedInRewrite(Note sourceNote, AiTranslationService ai, String targetLang) throws Exception {
        String bodyText = editorJsConverter.extractPlainText(sourceNote.getContent(), 5000);
        log.info("LinkedIn rewrite: extracted body text ({} chars):\n{}", bodyText.length(), bodyText);

        // Translate title
        List<String> titleTranslated = ai.translateTexts(List.of(sourceNote.getTitle()), targetLang);
        String translatedTitle = titleTranslated.get(0);
        log.info("LinkedIn rewrite: translated title = {}", translatedTitle);

        String rewritten = ai.rewriteForLinkedIn(sourceNote.getTitle(), bodyText);
        log.info("LinkedIn rewrite: AI result ({} chars):\n{}", rewritten.length(), rewritten);

        // Extract image blocks from source to preserve in result
        List<JsonNode> imageBlocks = new ArrayList<>();
        String sourceContent = sourceNote.getContent();
        if (sourceContent != null && !sourceContent.isBlank()) {
            JsonNode sourceBlocks = objectMapper.readTree(sourceContent).path("blocks");
            if (sourceBlocks.isArray()) {
                for (JsonNode block : sourceBlocks) {
                    if ("image".equals(block.path("type").asText())) {
                        imageBlocks.add(block);
                    }
                }
            }
        }
        log.info("LinkedIn rewrite: appending {} image block(s) from source", imageBlocks.size());

        // Build EditorJS content: rewritten paragraphs + original image blocks
        String translatedContent = buildSingleParagraphContent(rewritten, imageBlocks);

        Note translated = Note.builder()
            .title("[EN] " + translatedTitle)
            .content(translatedContent)
            .contentType("editorjs")
            .owner(sourceNote.getOwner())
            .isPublic(false)
            .language(targetLang)
            .sourceNote(sourceNote)
            .build();

        return noteRepository.save(translated);
    }

    private String buildSingleParagraphContent(String text, List<JsonNode> extraBlocks) throws Exception {
        // Split on blank lines to create multiple paragraphs
        String[] paragraphs = text.split("\n\n+");
        ArrayNode blocks = objectMapper.createArrayNode();
        for (String para : paragraphs) {
            String trimmed = para.replace("\n", "<br>").trim();
            if (!trimmed.isEmpty()) {
                ObjectNode block = objectMapper.createObjectNode();
                block.put("type", "paragraph");
                ObjectNode data = block.putObject("data");
                data.put("text", trimmed);
                blocks.add(block);
            }
        }
        for (JsonNode imageBlock : extraBlocks) {
            blocks.add(imageBlock);
        }
        ObjectNode root = objectMapper.createObjectNode();
        root.set("blocks", blocks);
        return objectMapper.writeValueAsString(root);
    }

    // ── helpers ─────────────────────────────────────────────────────────────────

    private AiTranslationService resolveService() {
        return switch (aiProperties.getProvider().toLowerCase()) {
            case "openai"    -> openAiService;
            case "deepseek"  -> deepseekService;
            case "doubao"    -> doubaoService;
            default          -> anthropicService;
        };
    }

    /** Strip basic HTML tags for cleaner translation input */
    private String stripHtml(String s) {
        return s == null ? "" : s.replaceAll("<[^>]*>", "").trim();
    }
}
