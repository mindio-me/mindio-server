/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 附件管理表实体类
 * 对应数据库表: attachments
 */
@Entity
@Table(name = "attachments")
@Data
@EqualsAndHashCode(callSuper = false)
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "att_id")
    private Long attId;

    /**
     * 附件名称
     */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * 附件路径
     */
    @Column(name = "att_dir", length = 500)
    private String attDir;

    /**
     * 压缩图片路径（如果有）
     */
    @Column(name = "satt_dir", length = 500)
    private String sattDir;

    /**
     * 附件大小（字节）
     */
    @Column(name = "att_size")
    private Long attSize;

    /**
     * 附件类型（如：png, jpg, pdf等）
     */
    @Column(name = "att_type", length = 50)
    private String attType;

    /**
     * 分类ID
     * 0-编辑器, 1-笔记图片, 2-用户头像, 3-其他
     */
    @Column(name = "pid")
    private Integer pid = 0;

    /**
     * 图片上传类型
     * 1-本地, 2-七牛云, 3-OSS, 4-COS, 5-京东云
     */
    @Column(name = "image_type")
    private Integer imageType = 1;

    /**
     * 资源归属方
     * -1-平台, 其他为用户ID
     */
    @Column(name = "owner")
    private Long owner;

    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    /**
     * 关联的用户（如果附件属于某个用户）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}

