/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.dto;

import com.entropybits.worknotes.spring_boot.entity.ImportItem;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GroupConfirmRequest {
    @NotNull
    private ImportItem.Category category;
    @NotNull
    private ImportItem.UserDecision decision;
}
