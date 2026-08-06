/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.dto;

import com.entropybits.worknotes.spring_boot.entity.ImportItem;
import com.entropybits.worknotes.spring_boot.entity.ImportJob;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ImportJobResponse {
    private Long id;
    private ImportJob.JobStatus status;
    private String fileName;
    private int totalCount;
    private int checkedCount;
    /** 按分类分组的条目列表，只在 status=READY/DONE 时前端才需要展示明细 */
    private Map<ImportItem.Category, List<ImportItemResponse>> groups;
}
