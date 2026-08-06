/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.dto;

import com.entropybits.worknotes.spring_boot.entity.LocalMediaFile;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LocalMediaFileResponse {

    private Long id;
    private Long directoryId;
    private String fileName;
    private String absolutePath;
    private String relativePath;
    private String mediaType;
    private String fileExtension;
    private Long fileSize;
    private LocalDateTime fileLastModified;
    private Integer imageWidth;
    private Integer imageHeight;
    private LocalDateTime snapshotCreatedAt;

    public static LocalMediaFileResponse fromEntity(LocalMediaFile f) {
        LocalMediaFileResponse resp = new LocalMediaFileResponse();
        resp.setId(f.getId());
        resp.setDirectoryId(f.getDirectory().getId());
        resp.setFileName(f.getFileName());
        resp.setAbsolutePath(f.getAbsolutePath());
        resp.setRelativePath(f.getRelativePath());
        resp.setMediaType(f.getMediaType());
        resp.setFileExtension(f.getFileExtension());
        resp.setFileSize(f.getFileSize());
        resp.setFileLastModified(f.getFileLastModified());
        resp.setImageWidth(f.getImageWidth());
        resp.setImageHeight(f.getImageHeight());
        resp.setSnapshotCreatedAt(f.getSnapshotCreatedAt());
        return resp;
    }
}
