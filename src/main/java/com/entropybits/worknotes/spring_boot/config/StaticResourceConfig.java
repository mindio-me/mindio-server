/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 静态资源映射配置
 * 将本地上传目录映射到 /uploads/** 以供访问
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(StaticResourceConfig.class);

    @Autowired
    private UploadPathConfig uploadPathConfig;

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        String uploadRootPath = uploadPathConfig.getUploadPath();
        Path uploadPath = Paths.get(uploadRootPath).toAbsolutePath().normalize();
        String location = uploadPath.toUri().toString(); // file:/...

        // 确保 location 以 / 结尾
        if (!location.endsWith("/")) {
            location = location + "/";
        }

        // 使用 System.out 确保日志一定输出
        System.out.println("========== 静态资源映射配置 ==========");
        System.out.println("uploadRootPath: " + uploadRootPath);
        System.out.println("uploadPath: " + uploadPath);
        System.out.println("location URI: " + location);
        System.out.println("目录是否存在: " + new File(uploadRootPath).exists());
        System.out.println("映射: /uploads/** -> " + location);
        System.out.println("======================================");

        logger.info("=== 静态资源映射配置 ===");
        logger.info("uploadRootPath: {}", uploadRootPath);
        logger.info("uploadPath: {}", uploadPath);
        logger.info("location URI: {}", location);
        logger.info("目录是否存在: {}", new File(uploadRootPath).exists());
        logger.info("映射: /uploads/** -> {}", location);

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location)
                .setCachePeriod(3600);
    }
}


