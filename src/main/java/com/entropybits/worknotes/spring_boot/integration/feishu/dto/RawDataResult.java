/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.feishu.dto;

import com.entropybits.worknotes.spring_boot.integration.feishu.client.model.FeishuDocxBlocksResponse.BlockItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 原始数据获取结果
 * <p>
 * 封装从飞书API获取的原始数据：
 * - raw_content API返回的markdown
 * - blocks API返回的blocks列表
 * - 元数据（是否截断、转换策略等）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RawDataResult {

    /**
     * raw_content API 返回的 markdown
     * 可能为 null（如果API失败或跳过）
     * 可能被截断（如果文档过大）
     */
    private String rawMarkdown;

    /**
     * rawMarkdown 是否被截断
     */
    @Builder.Default
    private boolean truncated = false;

    /**
     * blocks API 返回的 blocks 列表
     * 应该始终完整（支持分页）
     */
    private List<BlockItem> blocks;

    /**
     * 分页数（blocks API）
     */
    @Builder.Default
    private int blocksPages = 0;

    /**
     * 转换策略
     * - raw_content: 使用 raw_content API 的结果
     * - blocks_api: 使用 blocks API 的结果
     * - hybrid: 两者结合
     * - failed: 都失败了
     */
    private String strategy;

    /**
     * 计算原始数据总大小（字节）
     */
    public int getTotalSizeBytes() {
        int size = 0;

        if (rawMarkdown != null) {
            size += rawMarkdown.getBytes(StandardCharsets.UTF_8).length;
        }

        // blocks 的大小通过JSON序列化后计算
        // 这里预估（实际在Service层序列化时计算）
        if (blocks != null && !blocks.isEmpty()) {
            // 预估每个block平均500字节
            size += blocks.size() * 500;
        }

        return size;
    }

    /**
     * 判断是否成功获取到数据
     */
    public boolean hasData() {
        return (rawMarkdown != null && !rawMarkdown.isEmpty())
                || (blocks != null && !blocks.isEmpty());
    }

    /**
     * 获取blocks总数
     */
    public int getBlocksCount() {
        return blocks != null ? blocks.size() : 0;
    }
}
