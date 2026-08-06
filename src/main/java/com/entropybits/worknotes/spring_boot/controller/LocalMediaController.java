/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.controller;

import com.entropybits.worknotes.spring_boot.dto.LocalMediaDirectoryRequest;
import com.entropybits.worknotes.spring_boot.dto.LocalMediaDirectoryResponse;
import com.entropybits.worknotes.spring_boot.dto.LocalMediaFileResponse;
import com.entropybits.worknotes.spring_boot.entity.LocalMediaFile;
import com.entropybits.worknotes.spring_boot.service.LocalMediaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/v1/local-media")
@RequiredArgsConstructor
public class LocalMediaController {

    private final LocalMediaService localMediaService;

    @GetMapping("/directories")
    public ResponseEntity<List<LocalMediaDirectoryResponse>> getDirectories(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(localMediaService.getDirectories(userDetails.getUsername()));
    }

    @PostMapping("/directories")
    public ResponseEntity<LocalMediaDirectoryResponse> addDirectory(
            @Valid @RequestBody LocalMediaDirectoryRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(localMediaService.addDirectory(request, userDetails.getUsername()));
    }

    @DeleteMapping("/directories/{id}")
    public ResponseEntity<Void> deleteDirectory(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        localMediaService.deleteDirectory(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/directories/{id}/scan")
    public ResponseEntity<LocalMediaDirectoryResponse> rescan(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(localMediaService.rescan(id, userDetails.getUsername()));
    }

    @GetMapping("/directories/{id}/files")
    public ResponseEntity<Page<LocalMediaFileResponse>> getFiles(
            @PathVariable Long id,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String mediaType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "40") int size,
            @RequestParam(defaultValue = "fileName") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction,
            @AuthenticationPrincipal UserDetails userDetails) {
        Sort sort = Sort.by("DESC".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
        return ResponseEntity.ok(localMediaService.getFiles(
                id, userDetails.getUsername(), keyword, mediaType,
                PageRequest.of(page, size, sort)));
    }

    @GetMapping("/files")
    public ResponseEntity<Page<LocalMediaFileResponse>> getAllFiles(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String mediaType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "40") int size,
            @RequestParam(defaultValue = "fileName") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction,
            @AuthenticationPrincipal UserDetails userDetails) {
        Sort sort = Sort.by("DESC".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);
        return ResponseEntity.ok(localMediaService.getAllFiles(
                userDetails.getUsername(), keyword, mediaType,
                PageRequest.of(page, size, sort)));
    }

    @GetMapping("/thumbnails/{id}")
    public ResponseEntity<Resource> getThumbnail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            File thumb = localMediaService.getThumbnail(id, userDetails.getUsername());
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
                    .body(new FileSystemResource(thumb));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/files/{id}")
    public ResponseEntity<?> serveFile(
            @PathVariable Long id,
            @RequestHeader HttpHeaders requestHeaders,
            @AuthenticationPrincipal UserDetails userDetails) {
        LocalMediaFile file = localMediaService.getVerifiedFile(id, userDetails.getUsername());
        Path path = Paths.get(file.getAbsolutePath());
        if (!Files.exists(path) || !Files.isReadable(path)) {
            return ResponseEntity.notFound().build();
        }

        String encodedName = URLEncoder.encode(file.getFileName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        String contentType = contentTypeFor(file.getFileExtension());
        Resource resource = new FileSystemResource(path);

        List<HttpRange> ranges = requestHeaders.getRange();
        if (!ranges.isEmpty()) {
            try {
                long fileLength = resource.contentLength();
                HttpRange range = ranges.get(0);
                long start = range.getRangeStart(fileLength);
                long end = range.getRangeEnd(fileLength);
                int contentLength = (int) (end - start + 1);

                byte[] data = new byte[contentLength];
                try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(path.toFile(), "r")) {
                    raf.seek(start);
                    raf.readFully(data);
                }

                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileLength)
                        .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedName)
                        .body(data);
            } catch (IOException e) {
                return ResponseEntity.internalServerError().build();
            }
        }

        try {
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedName)
                    .contentLength(resource.contentLength())
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private static String contentTypeFor(String ext) {
        if (ext == null) return "application/octet-stream";
        return switch (ext.toLowerCase()) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png"         -> "image/png";
            case "gif"         -> "image/gif";
            case "webp"        -> "image/webp";
            case "bmp"         -> "image/bmp";
            case "svg"         -> "image/svg+xml";
            case "mp4"         -> "video/mp4";
            case "webm"        -> "video/webm";
            case "avi"         -> "video/x-msvideo";
            case "mov"         -> "video/quicktime";
            case "mkv"         -> "video/x-matroska";
            case "wmv"         -> "video/x-ms-wmv";
            case "mp3"         -> "audio/mpeg";
            case "flac"        -> "audio/flac";
            case "wav"         -> "audio/wav";
            case "aac"         -> "audio/aac";
            case "ogg"         -> "audio/ogg";
            case "m4a"         -> "audio/mp4";
            default            -> "application/octet-stream";
        };
    }
}
