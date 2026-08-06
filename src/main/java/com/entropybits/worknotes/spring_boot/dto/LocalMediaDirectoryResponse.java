/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.dto;

import com.entropybits.worknotes.spring_boot.entity.LocalMediaDirectory;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LocalMediaDirectoryResponse {

    private Long id;
    private String dirPath;
    private String displayName;
    private String scanStatus;
    private LocalDateTime lastScanAt;
    private String lastScanError;
    private Integer fileCount;
    private LocalDateTime createdAt;

    public static LocalMediaDirectoryResponse fromEntity(LocalMediaDirectory dir) {
        LocalMediaDirectoryResponse resp = new LocalMediaDirectoryResponse();
        resp.setId(dir.getId());
        resp.setDirPath(dir.getDirPath());
        resp.setDisplayName(dir.getDisplayName());
        resp.setScanStatus(dir.getScanStatus());
        resp.setLastScanAt(dir.getLastScanAt());
        resp.setLastScanError(dir.getLastScanError());
        resp.setFileCount(dir.getFileCount());
        resp.setCreatedAt(dir.getCreatedAt());
        return resp;
    }
}
