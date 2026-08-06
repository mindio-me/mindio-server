/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.service.impl;

import com.entropybits.worknotes.spring_boot.dto.FileResultVo;
import com.entropybits.worknotes.spring_boot.entity.Attachment;
import com.entropybits.worknotes.spring_boot.exception.BadRequestException;
import com.entropybits.worknotes.spring_boot.repository.AttachmentRepository;
import com.entropybits.worknotes.spring_boot.service.UploadService;
import com.entropybits.worknotes.spring_boot.utils.UploadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * 文件上传服务实现类
 * 目前支持本地存储，后续可扩展云存储功能
 */
@Service
public class UploadServiceImpl implements UploadService {

    private static final Logger logger = LoggerFactory.getLogger(UploadServiceImpl.class);

    // 允许的图片扩展名
    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "heic"
    );

    // 允许的文件扩展名
    private static final List<String> ALLOWED_FILE_EXTENSIONS = Arrays.asList(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "zip", "rar", "7z",
            "mp4", "mov", "avi", "mkv", "webm", "flv",
            "mp3", "wav", "ogg", "m4a", "flac", "aac", "opus", "wma"
    );

    // 默认最大文件大小（MB）
    private static final int DEFAULT_MAX_FILE_SIZE_MB = 50;
    private static final int DEFAULT_MAX_IMAGE_SIZE_MB = 10;

    @Autowired
    private com.entropybits.worknotes.spring_boot.config.UploadPathConfig uploadPathConfig;

    @Value("${worknotes.upload.url-prefix:}")
    private String uploadUrlPrefix;

    private final AttachmentRepository attachmentRepository;

    public UploadServiceImpl(AttachmentRepository attachmentRepository) {
        this.attachmentRepository = attachmentRepository;
    }

    @jakarta.annotation.PostConstruct
    public void init() {
        logger.info("=== Upload Service Configuration ===");
        String uploadRootPath = uploadPathConfig.getUploadPath();
        logger.info("Upload Root Path: {}", uploadRootPath);
        logger.info("Upload URL Prefix: {}", uploadUrlPrefix);

        // 将相对路径转换为绝对路径（用于调试）
        java.io.File uploadDir = new java.io.File(uploadRootPath);
        logger.info("Upload Absolute Path: {}", uploadDir.getAbsolutePath());
        logger.info("Upload Directory Exists: {}", uploadDir.exists());
    }

    @Override
    public FileResultVo imageUpload(MultipartFile multipartFile, String model, Integer pid) {
        return imageUpload(multipartFile, model, pid, null);
    }

    @Override
    public FileResultVo imageUpload(MultipartFile multipartFile, String model, Integer pid, Long ownerId) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new BadRequestException("上传的文件不能为空");
        }

        try {
            return commonUpload(multipartFile, model, pid, ownerId, true);
        } catch (IOException e) {
            logger.error("图片上传IO异常", e);
            throw new BadRequestException("图片上传失败: " + e.getMessage());
        }
    }

    @Override
    public FileResultVo fileUpload(MultipartFile multipartFile, String model, Integer pid) {
        return fileUpload(multipartFile, model, pid, null);
    }

    @Override
    public FileResultVo fileUpload(MultipartFile multipartFile, String model, Integer pid, Long ownerId) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new BadRequestException("上传的文件不能为空");
        }

        try {
            return commonUpload(multipartFile, model, pid, ownerId, false);
        } catch (IOException e) {
            logger.error("文件上传IO异常", e);
            throw new BadRequestException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public FileResultVo base64Upload(String base64, String model, Integer pid) {
        return base64Upload(base64, model, pid, null);
    }

    @Override
    public FileResultVo remoteUpload(String url, String model, Integer pid, Long ownerId) {
        if (url == null || url.isBlank()) {
            throw new BadRequestException("url不能为空");
        }
        try {
            URI uri = URI.create(url);
            String path = uri.getPath() == null ? "" : uri.getPath();
            String filename = path.contains("/") ? path.substring(path.lastIndexOf("/") + 1) : path;
            if (filename.isBlank()) {
                filename = "remote";
            }

            String extName = UploadUtil.getFileExtension(filename);
            boolean isImage = ALLOWED_IMAGE_EXTENSIONS.contains(extName.toLowerCase());
            if (!isImage && !ALLOWED_FILE_EXTENSIONS.contains(extName.toLowerCase())) {
                // 兜底：不给扩展名也允许，默认按文件处理并用 bin
                extName = extName.isBlank() ? "bin" : extName;
            }

            // 下载文件（限制大小：按图片/文件的默认限制）
            long maxSizeBytes = (isImage ? DEFAULT_MAX_IMAGE_SIZE_MB : DEFAULT_MAX_FILE_SIZE_MB) * 1024L * 1024L;

            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(20000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "worknotes-uploader/1.0");

            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new BadRequestException("远程文件拉取失败，HTTP " + code);
            }

            long contentLength = conn.getContentLengthLong();
            if (contentLength > 0 && contentLength > maxSizeBytes) {
                throw new BadRequestException("远程文件过大，最大允许: " + (maxSizeBytes / 1024 / 1024) + "MB");
            }

            // 如果没有扩展名，尝试从 Content-Type 推断
            if (extName.isBlank() || "bin".equals(extName)) {
                String contentType = conn.getContentType();
                if (contentType != null && contentType.contains("/")) {
                    // 从 Content-Type 推断扩展名：image/png -> png, image/jpeg -> jpg
                    String guessedExt = guessExtensionFromContentType(contentType);
                    if (guessedExt != null && !guessedExt.isBlank()) {
                        extName = guessedExt;
                        isImage = ALLOWED_IMAGE_EXTENSIONS.contains(extName.toLowerCase());
                    }
                }
            }

            // 生成新文件名并保存到本地
            String newFileName = UploadUtil.fileName(extName.isBlank() ? "bin" : extName);
            String modelPath = "public/" + model + "/";
            String webPath = UploadUtil.getWebPath(UploadUtil.UPLOAD_FILE_KEYWORD + "/" + modelPath);
            String uploadRootPath = uploadPathConfig.getUploadPath();
            String serverPath = UploadUtil.getServerPath(uploadRootPath, webPath);
            String destPath = serverPath + newFileName;
            
            // 调试日志：显示路径计算过程
            logger.debug("=== Remote Upload Path Debug ===");
            logger.debug("Upload Root Path (from config): {}", uploadRootPath);
            logger.debug("Model Path: {}", modelPath);
            logger.debug("Web Path: {}", webPath);
            logger.debug("Server Path (calculated): {}", serverPath);
            logger.debug("Destination Path (full): {}", destPath);

            File file = UploadUtil.createFile(destPath);

            long written;
            try (InputStream in = conn.getInputStream()) {
                written = Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            if (written > maxSizeBytes) {
                // 超限则删除
                //noinspection ResultOfMethodCallIgnored
                file.delete();
                throw new BadRequestException("远程文件过大，最大允许: " + (maxSizeBytes / 1024 / 1024) + "MB");
            }

            FileResultVo result = new FileResultVo();
            result.setFileName(filename);
            result.setExtName(extName);
            result.setFileSize(written);
            result.setUrl("/uploads/" + webPath + newFileName);
            result.setType(conn.getContentType());

            Attachment attachment = new Attachment();
            attachment.setName(filename);
            attachment.setAttDir(result.getUrl());
            attachment.setAttSize(result.getFileSize());
            attachment.setAttType(extName);
            attachment.setImageType(1);
            attachment.setPid(pid != null ? pid : 0);
            attachment.setOwner(ownerId);
            attachmentRepository.save(attachment);

            if (uploadUrlPrefix != null && !uploadUrlPrefix.isEmpty()) {
                result.setUrl(uploadUrlPrefix + result.getUrl());
            }
            return result;
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("url格式不正确");
        } catch (IOException e) {
            logger.error("远程文件拉取IO异常", e);
            throw new BadRequestException("远程文件拉取失败: " + e.getMessage());
        }
    }

    /**
     * Base64图片上传（内部方法，支持指定所有者）
     */
    private FileResultVo base64Upload(String base64, String model, Integer pid, Long ownerId) {
        if (base64 == null || base64.isEmpty()) {
            throw new BadRequestException("Base64数据不能为空");
        }

        try {
            // 去掉base64前缀 data:image/jpeg;base64,
            String base64Data = base64;
            if (base64.contains(",")) {
                base64Data = base64.substring(base64.indexOf(",") + 1);
            }

            // 解码Base64
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);

            // 生成文件名
            String extName = "jpg"; // 默认jpg
            String newFileName = UploadUtil.fileName(extName);

            // 构建路径
            String modelPath = "public/" + model + "/";
            String webPath = UploadUtil.getWebPath(UploadUtil.UPLOAD_FILE_KEYWORD + "/" + modelPath);
            String uploadRootPath = uploadPathConfig.getUploadPath();
            String serverPath = UploadUtil.getServerPath(uploadRootPath, webPath);
            String destPath = serverPath + newFileName;
            
            // 调试日志：显示路径计算过程
            logger.debug("=== Base64 Upload Path Debug ===");
            logger.debug("Upload Root Path (from config): {}", uploadRootPath);
            logger.debug("Model Path: {}", modelPath);
            logger.debug("Web Path: {}", webPath);
            logger.debug("Server Path (calculated): {}", serverPath);
            logger.debug("Destination Path (full): {}", destPath);

            // 创建文件
            File file = UploadUtil.createFile(destPath);

            // 写入文件
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(imageBytes);
                fos.flush();
            }

            // 构建返回结果
            FileResultVo result = new FileResultVo();
            result.setFileName(newFileName);
            result.setExtName(extName);
            result.setFileSize((long) imageBytes.length);
            result.setUrl("/uploads/" + webPath + newFileName);
            result.setType("image/jpeg");

            // 保存附件记录
            Attachment attachment = new Attachment();
            attachment.setName(newFileName);
            attachment.setAttDir(result.getUrl());
            attachment.setAttSize(result.getFileSize());
            attachment.setAttType(extName);
            attachment.setImageType(1); // 本地存储
            attachment.setPid(pid != null ? pid : 0);
            attachment.setOwner(ownerId);

            attachmentRepository.save(attachment);

            // 添加URL前缀
            if (uploadUrlPrefix != null && !uploadUrlPrefix.isEmpty()) {
                result.setUrl(uploadUrlPrefix + result.getUrl());
            }

            return result;
        } catch (Exception e) {
            logger.error("Base64图片上传异常", e);
            throw new BadRequestException("Base64图片上传失败: " + e.getMessage());
        }
    }

    /**
     * 通用上传方法
     *
     * @param multipartFile 文件
     * @param model 模块
     * @param pid 分类ID
     * @param ownerId 所有者ID
     * @param isImage 是否为图片
     * @return FileResultVo
     * @throws IOException IO异常
     */
    private FileResultVo commonUpload(MultipartFile multipartFile, String model, Integer pid, Long ownerId, boolean isImage) throws IOException {
        // 验证文件
        String originalFilename = multipartFile.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new BadRequestException("文件名不能为空");
        }

        // 获取文件扩展名
        String extName = UploadUtil.getFileExtension(originalFilename);
        if (extName.isEmpty()) {
            // 尝试从ContentType获取
            String contentType = multipartFile.getContentType();
            if (contentType != null && contentType.contains("/")) {
                extName = contentType.split("/")[1];
            } else {
                throw new BadRequestException("无法识别文件类型");
            }
        }

        // 验证文件类型
        List<String> allowedExtensions = isImage ? ALLOWED_IMAGE_EXTENSIONS : ALLOWED_FILE_EXTENSIONS;
        if (!allowedExtensions.contains(extName.toLowerCase())) {
            throw new BadRequestException(
                    String.format("不支持的文件类型: %s，允许的类型: %s", extName, String.join(", ", allowedExtensions))
            );
        }

        // 验证文件大小
        long fileSizeBytes = multipartFile.getSize();
        long maxSizeBytes = (isImage ? DEFAULT_MAX_IMAGE_SIZE_MB : DEFAULT_MAX_FILE_SIZE_MB) * 1024L * 1024L;
        if (fileSizeBytes > maxSizeBytes) {
            throw new BadRequestException(
                    String.format("文件大小超过限制，最大允许: %d MB，当前文件: %.2f MB",
                            isImage ? DEFAULT_MAX_IMAGE_SIZE_MB : DEFAULT_MAX_FILE_SIZE_MB,
                            fileSizeBytes / 1024.0 / 1024.0)
            );
        }

        // 生成新文件名
        String newFileName = UploadUtil.fileName(extName);

        // 构建路径
        String modelPath = "public/" + model + "/";
        String webPath = UploadUtil.getWebPath(UploadUtil.UPLOAD_FILE_KEYWORD + "/" + modelPath);
        String uploadRootPath = uploadPathConfig.getUploadPath();
        String serverPath = UploadUtil.getServerPath(uploadRootPath, webPath);
        String destPath = serverPath + newFileName;
        
        // 调试日志：显示路径计算过程
        logger.debug("=== File Upload Path Debug ===");
        logger.debug("Upload Root Path (from config): {}", uploadRootPath);
        logger.debug("Model Path: {}", modelPath);
        logger.debug("Web Path: {}", webPath);
        logger.debug("Server Path (calculated): {}", serverPath);
        logger.debug("Destination Path (full): {}", destPath);

        // 创建文件
        File file = UploadUtil.createFile(destPath);

        // 保存文件
        multipartFile.transferTo(file);

        // 构建返回结果
        FileResultVo result = new FileResultVo();
        result.setFileName(originalFilename);
        result.setExtName(extName);
        result.setFileSize(fileSizeBytes);
        result.setUrl("/uploads/" + webPath + newFileName);
        result.setType(multipartFile.getContentType());

        // 保存附件记录
        Attachment attachment = new Attachment();
        attachment.setName(originalFilename);
        attachment.setAttDir(result.getUrl());
        attachment.setAttSize(result.getFileSize());
        attachment.setAttType(extName);
        attachment.setImageType(1); // 本地存储
        attachment.setPid(pid != null ? pid : 0);
        attachment.setOwner(ownerId);

        attachmentRepository.save(attachment);

        // 添加URL前缀
        if (uploadUrlPrefix != null && !uploadUrlPrefix.isEmpty()) {
            result.setUrl(uploadUrlPrefix + result.getUrl());
        }

        return result;
    }

    /**
     * 从 Content-Type 推断文件扩展名
     *
     * @param contentType HTTP Content-Type 头
     * @return 文件扩展名（不含点），如果无法推断则返回 null
     */
    private String guessExtensionFromContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return null;
        }

        // 移除参数部分（如 "image/png; charset=utf-8" -> "image/png"）
        String mimeType = contentType.split(";")[0].trim().toLowerCase();

        // 常见 MIME 类型映射
        return switch (mimeType) {
            // 图片类型
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            case "image/bmp" -> "bmp";
            case "image/svg+xml" -> "svg";
            case "image/tiff" -> "tiff";
            case "image/x-icon", "image/vnd.microsoft.icon" -> "ico";

            // 文档类型
            case "application/pdf" -> "pdf";
            case "application/msword" -> "doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx";
            case "application/vnd.ms-excel" -> "xls";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "xlsx";
            case "application/vnd.ms-powerpoint" -> "ppt";
            case "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> "pptx";

            // 文本类型
            case "text/plain" -> "txt";
            case "text/markdown" -> "md";
            case "text/html" -> "html";
            case "text/css" -> "css";
            case "text/javascript", "application/javascript" -> "js";
            case "application/json" -> "json";
            case "application/xml", "text/xml" -> "xml";

            // 压缩文件
            case "application/zip" -> "zip";
            case "application/x-rar-compressed" -> "rar";
            case "application/x-7z-compressed" -> "7z";
            case "application/x-tar" -> "tar";
            case "application/gzip" -> "gz";

            // 视频
            case "video/mp4" -> "mp4";
            case "video/mpeg" -> "mpeg";
            case "video/quicktime" -> "mov";

            // 音频
            case "audio/mpeg", "audio/mp3" -> "mp3";
            case "audio/wav", "audio/x-wav" -> "wav";
            case "audio/ogg" -> "ogg";
            case "audio/mp4", "audio/m4a", "audio/x-m4a" -> "m4a";
            case "audio/flac", "audio/x-flac" -> "flac";
            case "audio/aac" -> "aac";
            case "audio/opus" -> "opus";
            case "audio/x-ms-wma" -> "wma";

            default -> null;
        };
    }
}

