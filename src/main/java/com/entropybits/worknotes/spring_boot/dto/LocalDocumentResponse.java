/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.dto;

import com.entropybits.worknotes.spring_boot.entity.LocalDocument;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LocalDocumentResponse {

    private Long id;
    private Long directoryId;
    private String directoryName;
    private String directoryPath;
    private String fileName;
    private String absolutePath;
    private String relativePath;
    private String fileType;
    private Long fileSize;
    private LocalDateTime fileLastModified;
    private LocalDateTime snapshotCreatedAt;

    public static LocalDocumentResponse fromEntity(LocalDocument doc) {
        LocalDocumentResponse resp = new LocalDocumentResponse();
        resp.setId(doc.getId());
        resp.setDirectoryId(doc.getDirectory().getId());
        resp.setDirectoryName(doc.getDirectory().getDisplayName() != null
                ? doc.getDirectory().getDisplayName()
                : directoryName(doc.getDirectory().getDirPath()));
        resp.setDirectoryPath(doc.getDirectory().getDirPath());
        resp.setFileName(doc.getFileName());
        resp.setAbsolutePath(doc.getAbsolutePath());
        resp.setRelativePath(doc.getRelativePath());
        resp.setFileType(doc.getFileType());
        resp.setFileSize(doc.getFileSize());
        resp.setFileLastModified(doc.getFileLastModified());
        resp.setSnapshotCreatedAt(doc.getSnapshotCreatedAt());
        return resp;
    }

    private static String directoryName(String path) {
        if (path == null || path.isBlank()) return "";
        String normalized = path.replace('\\', '/');
        int end = normalized.endsWith("/") ? normalized.length() - 1 : normalized.length();
        int slash = normalized.lastIndexOf('/', end - 1);
        return normalized.substring(slash + 1, end);
    }
}
