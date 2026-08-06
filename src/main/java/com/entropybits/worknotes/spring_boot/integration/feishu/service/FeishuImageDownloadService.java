/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.feishu.service;

import com.entropybits.worknotes.spring_boot.dto.FileResultVo;
import com.entropybits.worknotes.spring_boot.entity.Attachment;
import com.entropybits.worknotes.spring_boot.entity.Note;
import com.entropybits.worknotes.spring_boot.entity.User;
import com.entropybits.worknotes.spring_boot.integration.feishu.client.FeishuClient;
import com.entropybits.worknotes.spring_boot.integration.feishu.client.model.FeishuBatchDownloadUrlResponse;
import com.entropybits.worknotes.spring_boot.integration.feishu.client.model.FeishuDocxBlocksResponse.BlockItem;
import com.entropybits.worknotes.spring_boot.integration.feishu.entity.FeishuDocumentSnapshot;
import com.entropybits.worknotes.spring_boot.integration.feishu.entity.FeishuImageMapping;
import com.entropybits.worknotes.spring_boot.integration.feishu.repository.FeishuImageMappingRepository;
import com.entropybits.worknotes.spring_boot.repository.AttachmentRepository;
import com.entropybits.worknotes.spring_boot.service.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 飞书图片下载服务
 * <p>
 * 负责下载飞书文档中的图片并保存到本地存储。
 * 支持批量下载、错误容错、URL替换。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeishuImageDownloadService {

    private final FeishuClient feishuClient;
    private final UploadService uploadService;
    private final FeishuImageMappingRepository imageMappingRepository;
    private final AttachmentRepository attachmentRepository;

    /**
     * 下载图片并替换markdown中的占位符
     *
     * @param accessToken 飞书访问令牌
     * @param documentId 文档ID
     * @param blocks 文档blocks列表
     * @param markdown 原始markdown内容
     * @param snapshot 文档快照
     * @param note 笔记
     * @param user 用户
     * @return 替换后的markdown内容
     */
    @Transactional
    public String downloadAndReplaceImages(
            String accessToken,
            String documentId,
            List<BlockItem> blocks,
            String markdown,
            FeishuDocumentSnapshot snapshot,
            Note note,
            User user) {

        log.info("Starting image download: documentId={}, userId={}", documentId, user.getId());

        if (blocks == null || blocks.isEmpty()) {
            log.debug("No blocks to process");
            return markdown;
        }

        // 1. 提取所有图片块
        List<ImageBlockInfo> imageInfos = extractImageBlocks(blocks);
        if (imageInfos.isEmpty()) {
            log.info("No images found in document: documentId={}", documentId);
            return markdown;
        }

        log.info("Found {} images in document: documentId={}", imageInfos.size(), documentId);

        // 2. 批量下载图片
        Map<String, String> tokenToLocalUrlMap = downloadImagesInBatches(
                accessToken, documentId, imageInfos, snapshot, note, user);

        // 3. 替换markdown中的占位符
        String updatedMarkdown = replaceImagePlaceholders(markdown, tokenToLocalUrlMap);

        log.info("Image download completed: documentId={}, total={}, success={}, failed={}",
                documentId, imageInfos.size(), tokenToLocalUrlMap.size(),
                imageInfos.size() - tokenToLocalUrlMap.size());

        return updatedMarkdown;
    }

    /**
     * 从blocks中提取图片信息
     */
    private List<ImageBlockInfo> extractImageBlocks(List<BlockItem> blocks) {
        List<ImageBlockInfo> imageInfos = new ArrayList<>();

        for (BlockItem block : blocks) {
            // block_type = 27 是图片块
            if (block.getBlockType() != null && block.getBlockType() == 27) {
                if (block.getImage() != null && block.getImage().getToken() != null) {
                    ImageBlockInfo info = new ImageBlockInfo();
                    info.blockId = block.getBlockId();
                    info.fileToken = block.getImage().getToken();
                    info.width = block.getImage().getWidth();
                    info.height = block.getImage().getHeight();
                    info.align = block.getImage().getAlign();

                    // 提取caption
                    if (block.getImage().getCaption() != null) {
                        info.caption = block.getImage().getCaption().getContent();
                    }

                    imageInfos.add(info);
                }
            }
        }

        return imageInfos;
    }

    /**
     * 批量下载图片（每批最多5个）
     *
     * @return fileToken -> localUrl 的映射
     */
    private Map<String, String> downloadImagesInBatches(
            String accessToken,
            String documentId,
            List<ImageBlockInfo> imageInfos,
            FeishuDocumentSnapshot snapshot,
            Note note,
            User user) {

        Map<String, String> tokenToLocalUrlMap = new HashMap<>();
        int batchSize = 5;  // 飞书API限制

        for (int i = 0; i < imageInfos.size(); i += batchSize) {
            int end = Math.min(i + batchSize, imageInfos.size());
            List<ImageBlockInfo> batch = imageInfos.subList(i, end);

            log.debug("Processing image batch {}/{}: tokens={}",
                    (i / batchSize) + 1, (imageInfos.size() + batchSize - 1) / batchSize,
                    batch.stream().map(info -> info.fileToken).toList());

            try {
                downloadBatch(accessToken, documentId, batch, snapshot, note, user, tokenToLocalUrlMap);
            } catch (Exception e) {
                log.error("Batch download failed (continuing with next batch): batchIndex={}, error={}",
                        i / batchSize, e.getMessage(), e);
                // 继续处理下一批
            }
        }

        return tokenToLocalUrlMap;
    }

    /**
     * 下载一批图片
     */
    private void downloadBatch(
            String accessToken,
            String documentId,
            List<ImageBlockInfo> batch,
            FeishuDocumentSnapshot snapshot,
            Note note,
            User user,
            Map<String, String> tokenToLocalUrlMap) {

        // 1. 获取临时下载URL
        List<String> fileTokens = batch.stream().map(info -> info.fileToken).toList();
        FeishuBatchDownloadUrlResponse response = feishuClient.getBatchTmpDownloadUrl(
                accessToken, fileTokens);

        if (response == null || response.getCode() == null || response.getCode() != 0) {
            String msg = response != null ? response.getMsg() : "null response";
            log.error("Failed to get batch download URLs: code={}, msg={}",
                    response != null ? response.getCode() : "null", msg);

            // 保存失败记录
            for (ImageBlockInfo info : batch) {
                saveFailedImageMapping(documentId, info, snapshot, note, user,
                        "Failed to get download URL: " + msg);
            }
            return;
        }

        if (response.getData() == null || response.getData().getTmpDownloadUrls() == null) {
            log.error("Batch download URL response has no data");
            for (ImageBlockInfo info : batch) {
                saveFailedImageMapping(documentId, info, snapshot, note, user,
                        "Response has no data");
            }
            return;
        }

        // 2. 创建 fileToken -> tmpUrl 映射
        Map<String, String> tokenToTmpUrlMap = new HashMap<>();
        for (var tmpUrl : response.getData().getTmpDownloadUrls()) {
            if (tmpUrl.getFileToken() != null && tmpUrl.getTmpDownloadUrl() != null) {
                tokenToTmpUrlMap.put(tmpUrl.getFileToken(), tmpUrl.getTmpDownloadUrl());
            }
        }

        // 3. 下载并上传每张图片
        for (ImageBlockInfo info : batch) {
            String tmpUrl = tokenToTmpUrlMap.get(info.fileToken);
            if (tmpUrl == null) {
                log.warn("No tmp URL for file_token: {}", info.fileToken);
                saveFailedImageMapping(documentId, info, snapshot, note, user,
                        "No tmp download URL returned");
                continue;
            }

            try {
                // 使用UploadService下载并上传
                FileResultVo result = uploadService.remoteUpload(
                        tmpUrl, "feishu_image", 1, user.getId());

                if (result != null && result.getUrl() != null) {
                    String localUrl = result.getUrl();

                    // 查找刚创建的附件（通过URL查找）
                    // 注意：URL可能包含前缀，需要提取相对路径
                    String relativeUrl = localUrl;
                    if (localUrl.startsWith("http://") || localUrl.startsWith("https://")) {
                        // 提取路径部分
                        try {
                            relativeUrl = new java.net.URL(localUrl).getPath();
                        } catch (Exception e) {
                            log.warn("Failed to parse URL: {}", localUrl);
                        }
                    }

                    Attachment attachment = attachmentRepository.findByAttDir(relativeUrl);
                    Long attachmentId = attachment != null ? attachment.getAttId() : null;

                    // 保存成功的映射
                    saveSuccessImageMapping(documentId, info, snapshot, note, user,
                            tmpUrl, localUrl, attachmentId);

                    tokenToLocalUrlMap.put(info.fileToken, localUrl);

                    log.debug("Image downloaded successfully: fileToken={}, localUrl={}, attachmentId={}",
                            info.fileToken, localUrl, attachmentId);
                } else {
                    log.warn("Upload returned null result: fileToken={}", info.fileToken);
                    saveFailedImageMapping(documentId, info, snapshot, note, user,
                            "Upload service returned null");
                }

            } catch (Exception e) {
                log.error("Failed to download/upload image: fileToken={}, error={}",
                        info.fileToken, e.getMessage(), e);
                saveFailedImageMapping(documentId, info, snapshot, note, user,
                        "Exception: " + e.getMessage());
            }
        }
    }

    /**
     * 保存成功的图片映射
     */
    private void saveSuccessImageMapping(
            String documentId,
            ImageBlockInfo info,
            FeishuDocumentSnapshot snapshot,
            Note note,
            User user,
            String feishuUrl,
            String localUrl,
            Long attachmentId) {

        Attachment attachment = attachmentId != null
                ? attachmentRepository.findById(attachmentId).orElse(null)
                : null;

        FeishuImageMapping mapping = FeishuImageMapping.builder()
                .fileToken(info.fileToken)
                .blockId(info.blockId)
                .documentId(documentId)
                .snapshot(snapshot)
                .note(note)
                .attachment(attachment)
                .user(user)
                .width(info.width)
                .height(info.height)
                .align(info.align)
                .caption(info.caption)
                .downloadStatus("success")
                .feishuUrl(feishuUrl)
                .localUrl(localUrl)
                .downloadedAt(LocalDateTime.now())
                .build();

        imageMappingRepository.save(mapping);
    }

    /**
     * 保存失败的图片映射
     */
    private void saveFailedImageMapping(
            String documentId,
            ImageBlockInfo info,
            FeishuDocumentSnapshot snapshot,
            Note note,
            User user,
            String errorMessage) {

        FeishuImageMapping mapping = FeishuImageMapping.builder()
                .fileToken(info.fileToken)
                .blockId(info.blockId)
                .documentId(documentId)
                .snapshot(snapshot)
                .note(note)
                .user(user)
                .width(info.width)
                .height(info.height)
                .align(info.align)
                .caption(info.caption)
                .downloadStatus("failed")
                .downloadErrorMessage(errorMessage)
                .build();

        imageMappingRepository.save(mapping);
    }

    /**
     * 替换markdown中的图片占位符
     */
    private String replaceImagePlaceholders(String markdown, Map<String, String> tokenToLocalUrlMap) {
        if (markdown == null || markdown.isEmpty() || tokenToLocalUrlMap.isEmpty()) {
            return markdown;
        }

        String result = markdown;

        for (Map.Entry<String, String> entry : tokenToLocalUrlMap.entrySet()) {
            String fileToken = entry.getKey();
            String localUrl = entry.getValue();

            // 替换 feishu-image:{token} 为本地 URL
            String placeholder = "feishu-image:" + fileToken;
            result = result.replace(placeholder, localUrl);

            log.debug("Replaced placeholder: {} -> {}", placeholder, localUrl);
        }

        return result;
    }

    /**
     * 图片块信息
     */
    private static class ImageBlockInfo {
        String blockId;
        String fileToken;
        Integer width;
        Integer height;
        Integer align;
        String caption;
    }
}
