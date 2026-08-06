/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.service;

import com.entropybits.worknotes.spring_boot.dto.LocalDocDirectoryRequest;
import com.entropybits.worknotes.spring_boot.dto.LocalDocDirectoryResponse;
import com.entropybits.worknotes.spring_boot.dto.LocalDocumentResponse;
import com.entropybits.worknotes.spring_boot.entity.LocalDocDirectory;
import com.entropybits.worknotes.spring_boot.entity.LocalDocument;
import com.entropybits.worknotes.spring_boot.entity.User;
import com.entropybits.worknotes.spring_boot.repository.LocalDocDirectoryRepository;
import com.entropybits.worknotes.spring_boot.repository.LocalDocumentRepository;
import com.entropybits.worknotes.spring_boot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LocalDocService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "doc", "docx", "xls", "xlsx", "ppt", "pptx", "pdf"
    );
    private static final int MAX_FILES = 10_000;

    private final LocalDocDirectoryRepository dirRepository;
    private final LocalDocumentRepository docRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<LocalDocDirectoryResponse> getDirectories(String username) {
        User user = getUser(username);
        return dirRepository.findByOwnerOrderByCreatedAtDesc(user).stream()
                .map(LocalDocDirectoryResponse::fromEntity)
                .toList();
    }

    @Transactional
    public LocalDocDirectoryResponse addDirectory(LocalDocDirectoryRequest request, String username) {
        User user = getUser(username);
        String normalizedPath = normalizePath(request.getDirPath());

        if (dirRepository.findByOwnerAndDirPath(user, normalizedPath).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该目录已添加：" + normalizedPath);
        }

        LocalDocDirectory dir = LocalDocDirectory.builder()
                .owner(user)
                .dirPath(normalizedPath)
                .displayName(request.getDisplayName())
                .build();
        dir = dirRepository.save(dir);

        return LocalDocDirectoryResponse.fromEntity(scan(dir, user));
    }

    @Transactional
    public void deleteDirectory(Long dirId, String username) {
        LocalDocDirectory dir = getOwnedDirectory(dirId, username);
        dirRepository.delete(dir);
    }

    @Transactional
    public LocalDocDirectoryResponse rescan(Long dirId, String username) {
        LocalDocDirectory dir = getOwnedDirectory(dirId, username);
        User user = getUser(username);
        return LocalDocDirectoryResponse.fromEntity(scan(dir, user));
    }

    @Transactional(readOnly = true)
    public Page<LocalDocumentResponse> getDocuments(Long dirId, String username,
                                                     String keyword, String fileType,
                                                     Pageable pageable) {
        LocalDocDirectory dir = getOwnedDirectory(dirId, username);
        return docRepository.searchInDirectory(dir, keyword, fileType, pageable)
                .map(LocalDocumentResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<LocalDocumentResponse> getAllDocuments(String username,
                                                       String keyword, String fileType,
                                                       Pageable pageable) {
        User user = getUser(username);
        return docRepository.searchByOwner(user, keyword, fileType, pageable)
                .map(LocalDocumentResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public LocalDocument getVerifiedDocument(Long docId, String username) {
        return getOwnedDocument(docId, username);
    }

    // ---------- file system browser ----------

    public Map<String, Object> listDirectory(String path) {
        List<Map<String, String>> entries = new ArrayList<>();
        String currentPath;
        String parentPath = null;

        if (path == null || path.isBlank()) {
            // return drive roots (C:\, D:\ on Windows; / on Unix)
            for (java.io.File root : java.io.File.listRoots()) {
                entries.add(Map.of("name", root.getPath(), "path", root.getPath()));
            }
            currentPath = "";
        } else {
            Path dir;
            try {
                dir = Paths.get(path.trim());
            } catch (InvalidPathException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "路径格式无效：" + path);
            }
            if (!Files.isDirectory(dir) || !Files.isReadable(dir)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "目录不存在或无法读取：" + path);
            }
            currentPath = dir.toString();
            Path parent = dir.getParent();
            parentPath = parent != null ? parent.toString() : null;

            try (java.util.stream.Stream<Path> stream = Files.list(dir)) {
                stream.filter(p -> Files.isDirectory(p) && !isHidden(p))
                      .sorted()
                      .forEach(p -> entries.add(Map.of("name", p.getFileName().toString(), "path", p.toString())));
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "读取目录失败：" + e.getMessage());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("currentPath", currentPath);
        result.put("parentPath", parentPath);
        result.put("entries", entries);
        return result;
    }

    // ---------- internal ----------

    private LocalDocDirectory scan(LocalDocDirectory dir, User user) {
        if ("SCANNING".equals(dir.getScanStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该目录正在扫描中，请稍后再试");
        }

        Path root = Paths.get(dir.getDirPath());
        if (!root.isAbsolute() || !Files.isDirectory(root) || !Files.isReadable(root)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "目录不存在或无读取权限：" + dir.getDirPath());
        }

        dir.setScanStatus("SCANNING");
        dir.setLastScanError(null);
        dirRepository.save(dir);

        List<LocalDocument> collected = new ArrayList<>();
        boolean truncated = false;

        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path d, BasicFileAttributes attrs) {
                    if (d != root && isHidden(d)) return FileVisitResult.SKIP_SUBTREE;
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (collected.size() >= MAX_FILES) return FileVisitResult.TERMINATE;
                    if (isHidden(file)) return FileVisitResult.CONTINUE;

                    String name = file.getFileName().toString();
                    String ext = extension(name);
                    if (!ALLOWED_EXTENSIONS.contains(ext)) return FileVisitResult.CONTINUE;

                    String relPath = root.relativize(file).toString();
                    LocalDateTime lastModified = attrs.lastModifiedTime()
                            .toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

                    collected.add(LocalDocument.builder()
                            .directory(dir)
                            .owner(user)
                            .fileName(name)
                            .absolutePath(file.toString())
                            .relativePath(relPath)
                            .fileType(ext)
                            .fileSize(attrs.size())
                            .fileLastModified(lastModified)
                            .build());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });

            if (collected.size() >= MAX_FILES) {
                truncated = true;
            }

            docRepository.deleteByDirectory(dir);

            // batch save in chunks of 500
            for (int i = 0; i < collected.size(); i += 500) {
                docRepository.saveAll(collected.subList(i, Math.min(i + 500, collected.size())));
            }

            dir.setScanStatus("IDLE");
            dir.setLastScanAt(LocalDateTime.now());
            dir.setDocumentCount(collected.size());
            if (truncated) {
                dir.setLastScanError("文件数量超过上限 " + MAX_FILES + "，已截断显示");
            }

        } catch (IOException e) {
            dir.setScanStatus("ERROR");
            dir.setLastScanError(e.getMessage());
        }

        return dirRepository.save(dir);
    }

    private String normalizePath(String raw) {
        try {
            Path p = Paths.get(raw.trim());
            if (!p.isAbsolute()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入绝对路径：" + raw);
            }
            return p.toString();
        } catch (InvalidPathException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "路径格式无效：" + raw);
        }
    }

    private boolean isHidden(Path path) {
        try {
            return Files.isHidden(path);
        } catch (IOException e) {
            return false;
        }
    }

    private String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) return "";
        return fileName.substring(dot + 1).toLowerCase();
    }

    private LocalDocument getOwnedDocument(Long docId, String username) {
        LocalDocument doc = docRepository.findById(docId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "文档不存在"));
        if (!doc.getOwner().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权访问该文档");
        }
        return doc;
    }

    private LocalDocDirectory getOwnedDirectory(Long dirId, String username) {
        LocalDocDirectory dir = dirRepository.findById(dirId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "目录不存在"));
        if (!dir.getOwner().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权访问该目录");
        }
        return dir;
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在"));
    }
}
