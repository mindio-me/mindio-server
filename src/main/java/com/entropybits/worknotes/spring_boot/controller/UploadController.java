/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.controller;

import com.entropybits.worknotes.spring_boot.dto.BaseUploadRequest;
import com.entropybits.worknotes.spring_boot.dto.FileResultVo;
import com.entropybits.worknotes.spring_boot.dto.RemoteUploadRequest;
import com.entropybits.worknotes.spring_boot.service.UploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传控制器
 */
@RestController
@RequestMapping("/v1/upload")
@Tag(name = "文件上传", description = "文件上传相关接口")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping("/image")
    @Operation(summary = "图片上传", description = "上传图片文件")
    public ResponseEntity<FileResultVo> uploadImage(
            @Parameter(description = "图片文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "模块名称（如：note, user等）") @RequestParam(value = "model", defaultValue = "note") String model,
            @Parameter(description = "分类ID（0-编辑器, 1-笔记图片, 2-用户头像, 3-其他）") @RequestParam(value = "pid", defaultValue = "0") Integer pid,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        // 获取当前用户ID
        Long userId = getUserId(userDetails);
        FileResultVo result = uploadService.imageUpload(file, model, pid, userId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/file")
    @Operation(summary = "文件上传", description = "上传普通文件")
    public ResponseEntity<FileResultVo> uploadFile(
            @Parameter(description = "文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "模块名称") @RequestParam(value = "model", defaultValue = "note") String model,
            @Parameter(description = "分类ID") @RequestParam(value = "pid", defaultValue = "0") Integer pid,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = getUserId(userDetails);
        FileResultVo result = uploadService.fileUpload(file, model, pid, userId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/base64")
    @Operation(summary = "Base64图片上传", description = "上传Base64编码的图片")
    public ResponseEntity<FileResultVo> uploadBase64(
            @Valid @RequestBody BaseUploadRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        FileResultVo result = uploadService.base64Upload(request.getBase64Url(), request.getModel(), request.getPid());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/remote")
    @Operation(summary = "网络链接上传", description = "从网络URL拉取文件并保存为附件")
    public ResponseEntity<FileResultVo> uploadRemote(
            @Valid @RequestBody RemoteUploadRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = getUserId(userDetails);
        FileResultVo result = uploadService.remoteUpload(request.getUrl(), request.getModel(), request.getPid(), userId);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取用户ID（从UserDetails中提取）
     * 这里需要根据实际的UserDetails实现来调整
     */
    private Long getUserId(UserDetails userDetails) {
        // TODO: 根据实际的UserDetails实现来获取用户ID
        // 如果UserDetails中包含用户ID，可以这样获取：
        // if (userDetails instanceof CustomUserDetails) {
        //     return ((CustomUserDetails) userDetails).getUserId();
        // }
        return null; // 暂时返回null，表示平台上传
    }
}

