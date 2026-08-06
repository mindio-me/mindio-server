/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.controller;

import com.entropybits.worknotes.spring_boot.dto.LocalDocDirectoryRequest;
import com.entropybits.worknotes.spring_boot.dto.LocalDocDirectoryResponse;
import com.entropybits.worknotes.spring_boot.dto.LocalDocumentResponse;
import com.entropybits.worknotes.spring_boot.entity.LocalDocument;
import com.entropybits.worknotes.spring_boot.service.LocalDocService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/local-docs")
@RequiredArgsConstructor
public class LocalDocController {

    private final LocalDocService localDocService;

    @GetMapping("/fs/list")
    public ResponseEntity<Map<String, Object>> listDirectory(
            @RequestParam(required = false) String path) {
        return ResponseEntity.ok(localDocService.listDirectory(path));
    }

    @GetMapping("/directories")
    public ResponseEntity<List<LocalDocDirectoryResponse>> getDirectories(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(localDocService.getDirectories(userDetails.getUsername()));
    }

    @PostMapping("/directories")
    public ResponseEntity<LocalDocDirectoryResponse> addDirectory(
            @Valid @RequestBody LocalDocDirectoryRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(localDocService.addDirectory(request, userDetails.getUsername()));
    }

    @DeleteMapping("/directories/{id}")
    public ResponseEntity<Void> deleteDirectory(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        localDocService.deleteDirectory(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/directories/{id}/scan")
    public ResponseEntity<LocalDocDirectoryResponse> rescan(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(localDocService.rescan(id, userDetails.getUsername()));
    }

    @GetMapping("/files/{id}")
    public ResponseEntity<Resource> serveFile(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        LocalDocument doc = localDocService.getVerifiedDocument(id, userDetails.getUsername());
        String absPath = doc.getAbsolutePath() != null ? doc.getAbsolutePath() : "";
        String fileType = doc.getFileType() != null ? doc.getFileType() : "";
        Path path = Paths.get(absPath);
        if (!Files.exists(path) || !Files.isReadable(path)) {
            return ResponseEntity.notFound().build();
        }
        String encodedName = URLEncoder.encode(doc.getFileName() != null ? doc.getFileName() : "", StandardCharsets.UTF_8).replace("+", "%20");
        String disposition = "pdf".equals(fileType) ? "inline" : "attachment";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentTypeFor(fileType)))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename*=UTF-8''" + encodedName)
                .body(new FileSystemResource(path));
    }

    @NonNull
    private static String contentTypeFor(String fileType) {
        return switch (fileType) {
            case "pdf"  -> "application/pdf";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "doc"  -> "application/msword";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "xls"  -> "application/vnd.ms-excel";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "ppt"  -> "application/vnd.ms-powerpoint";
            default     -> "application/octet-stream";
        };
    }

    @GetMapping("/directories/{id}/documents")
    public ResponseEntity<Page<LocalDocumentResponse>> getDocuments(
            @PathVariable Long id,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String fileType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "fileName") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction,
            @AuthenticationPrincipal UserDetails userDetails) {
        Sort sort = Sort.by("DESC".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
        Page<LocalDocumentResponse> result = localDocService.getDocuments(
                id, userDetails.getUsername(), keyword, fileType,
                PageRequest.of(page, size, sort));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/documents")
    public ResponseEntity<Page<LocalDocumentResponse>> getAllDocuments(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String fileType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "fileName") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction,
            @AuthenticationPrincipal UserDetails userDetails) {
        Sort sort = Sort.by("DESC".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
        Page<LocalDocumentResponse> result = localDocService.getAllDocuments(
                userDetails.getUsername(), keyword, fileType,
                PageRequest.of(page, size, sort));
        return ResponseEntity.ok(result);
    }
}
