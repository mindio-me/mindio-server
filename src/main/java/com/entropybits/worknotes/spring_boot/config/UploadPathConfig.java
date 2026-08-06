/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 上传路径配置类
 * 使用工作目录 + 相对路径配置上传路径
 */
@Configuration
public class UploadPathConfig {

    private static final Logger logger = LoggerFactory.getLogger(UploadPathConfig.class);

    @Value("${worknotes.upload.path:}")
    private String configuredUploadPath;

    @Value("${worknotes.upload.relative-path:uploads}")
    private String relativeUploadPath;

    private String resolvedUploadPath;

    /**
     * 获取解析后的上传路径
     * 如果配置了绝对路径，则使用配置的路径
     * 否则使用 web 根目录 + relativeUploadPath
     */
    public String getUploadPath() {
        return resolvedUploadPath;
    }

    @PostConstruct
    public void init() {
        // 如果配置了绝对路径，直接使用
        if (configuredUploadPath != null && !configuredUploadPath.trim().isEmpty()) {
            resolvedUploadPath = Paths.get(configuredUploadPath).toAbsolutePath().normalize().toString();
            logger.info("使用配置的上传路径: {}", resolvedUploadPath);
            ensureDirectoryExists();
            return;
        }

        // 优先使用工作目录（项目目录），因为嵌入式 Tomcat 的 ServletContext.getRealPath
        // 会返回临时目录，导致上传文件存放位置与预期不符
        String webRootPath = System.getProperty("user.dir");
        logger.info("使用工作目录作为 web 根目录: {}", webRootPath);

        // 构建上传路径：工作目录 + relativeUploadPath
        Path uploadPath = Paths.get(webRootPath, relativeUploadPath).toAbsolutePath().normalize();
        resolvedUploadPath = uploadPath.toString();

        logger.info("=== 上传路径配置 ===");
        logger.info("Web 根目录: {}", webRootPath);
        logger.info("相对上传路径: {}", relativeUploadPath);
        logger.info("最终上传路径: {}", resolvedUploadPath);

        ensureDirectoryExists();
    }

    private void ensureDirectoryExists() {
        File uploadDir = new File(resolvedUploadPath);
        if (!uploadDir.exists()) {
            boolean created = uploadDir.mkdirs();
            if (created) {
                logger.info("创建上传目录: {}", resolvedUploadPath);
            } else {
                logger.warn("无法创建上传目录: {}", resolvedUploadPath);
            }
        } else {
            logger.info("上传目录已存在: {}", resolvedUploadPath);
        }
    }
}
