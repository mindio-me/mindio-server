/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.dto;

import com.entropybits.worknotes.spring_boot.entity.LocalDocDirectory;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LocalDocDirectoryResponse {

    private Long id;
    private String dirPath;
    private String displayName;
    private String scanStatus;
    private LocalDateTime lastScanAt;
    private String lastScanError;
    private Integer documentCount;
    private LocalDateTime createdAt;

    public static LocalDocDirectoryResponse fromEntity(LocalDocDirectory dir) {
        LocalDocDirectoryResponse resp = new LocalDocDirectoryResponse();
        resp.setId(dir.getId());
        resp.setDirPath(dir.getDirPath());
        resp.setDisplayName(dir.getDisplayName());
        resp.setScanStatus(dir.getScanStatus());
        resp.setLastScanAt(dir.getLastScanAt());
        resp.setLastScanError(dir.getLastScanError());
        resp.setDocumentCount(dir.getDocumentCount());
        resp.setCreatedAt(dir.getCreatedAt());
        return resp;
    }
}
