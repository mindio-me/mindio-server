/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.utils;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 文件上传工具类
 */
public class UploadUtil {

    /**
     * 文件上传关键字
     */
    public static final String UPLOAD_FILE_KEYWORD = "worknotesimage";

    /**
     * 根据文件的绝对路径创建一个文件对象
     *
     * @param filePath 文件路径
     * @return 返回创建的这个文件对象
     * @throws IOException 如果目录创建失败
     */
    public static File createFile(String filePath) throws IOException {
        // 直接使用原始路径，File API 会自动处理路径分隔符
        File file = new File(filePath);
        File parentFile = file.getParentFile();
        
        // 创建父目录（如果不存在）
        if (parentFile != null && !parentFile.exists()) {
            try {
                // 使用 File.mkdirs() - 更可靠的方式
                boolean created = parentFile.mkdirs();
                if (!created && !parentFile.exists()) {
                    throw new IOException("无法创建目录: " + parentFile.getAbsolutePath());
                }
            } catch (SecurityException e) {
                throw new IOException("无权限创建目录: " + parentFile.getAbsolutePath(), e);
            }
        }
        
        return file;
    }

    /**
     * 生成文件名
     *
     * @param extName 扩展名（不含点号）
     * @return 文件名
     */
    public static String fileName(String extName) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String randomStr = generateRandomString(10);
        return uuid + randomStr + "." + extName;
    }

    /**
     * 生成随机字符串
     *
     * @param length 长度
     * @return 随机字符串
     */
    private static String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
    }

    /**
     * 获取Web访问路径（相对路径）
     *
     * @param modelPath 模块路径
     * @return Web路径
     */
    public static String getWebPath(String modelPath) {
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        // 确保 modelPath 以 / 结尾，避免双斜杠
        String normalizedPath = modelPath.endsWith("/") ? modelPath : modelPath + "/";
        return normalizedPath + datePath + "/";
    }

    /**
     * 获取服务器存储路径（绝对路径）
     * 确保路径规范化，避免出现 \..\ 或 \..\ 这样的路径
     *
     * @param rootPath 根路径
     * @param webPath Web路径
     * @return 服务器路径（使用系统合适的路径分隔符）
     */
    public static String getServerPath(String rootPath, String webPath) {
        // 首先规范化两个路径，移除不必要的分隔符
        String normalRoot = rootPath.trim();
        if (normalRoot.isEmpty()) {
            normalRoot = "./uploads";
        }
        
        String normalWeb = webPath.trim();
        
        // 确保 rootPath 以分隔符结尾
        if (!normalRoot.endsWith("/") && !normalRoot.endsWith("\\")) {
            normalRoot += File.separator;
        }
        
        // 拼接路径：使用系统的文件分隔符
        String result = normalRoot + normalWeb;
        
        // 规范化路径：将所有正斜杠转换为系统适配的分隔符
        result = result.replace("/", File.separator).replace("\\", File.separator);
        
        // 移除重复的分隔符
        String sep = File.separator;
        while (result.contains(sep + sep)) {
            result = result.replace(sep + sep, sep);
        }
        
        return result;
    }

    /**
     * 获取文件扩展名（不含点号）
     *
     * @param filename 文件名
     * @return 扩展名
     */
    public static String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1).toLowerCase();
    }
}

