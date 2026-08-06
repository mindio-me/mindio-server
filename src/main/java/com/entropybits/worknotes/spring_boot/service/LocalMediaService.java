/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.service;

import com.entropybits.worknotes.spring_boot.dto.LocalMediaDirectoryRequest;
import com.entropybits.worknotes.spring_boot.dto.LocalMediaDirectoryResponse;
import com.entropybits.worknotes.spring_boot.dto.LocalMediaFileResponse;
import com.entropybits.worknotes.spring_boot.entity.LocalMediaDirectory;
import com.entropybits.worknotes.spring_boot.entity.LocalMediaFile;
import com.entropybits.worknotes.spring_boot.entity.User;
import com.entropybits.worknotes.spring_boot.repository.LocalMediaDirectoryRepository;
import com.entropybits.worknotes.spring_boot.repository.LocalMediaFileRepository;
import com.entropybits.worknotes.spring_boot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LocalMediaService {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg");
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "avi", "mov", "mkv", "webm", "wmv");
    private static final Set<String> AUDIO_EXTENSIONS = Set.of("mp3", "flac", "wav", "aac", "ogg", "m4a");
    private static final Set<String> ALL_EXTENSIONS;

    static {
        ALL_EXTENSIONS = new HashSet<>();
        ALL_EXTENSIONS.addAll(IMAGE_EXTENSIONS);
        ALL_EXTENSIONS.addAll(VIDEO_EXTENSIONS);
        ALL_EXTENSIONS.addAll(AUDIO_EXTENSIONS);
    }

    private static final int MAX_FILES = 10_000;

    private final LocalMediaDirectoryRepository dirRepository;
    private final LocalMediaFileRepository fileRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<LocalMediaDirectoryResponse> getDirectories(String username) {
        User user = getUser(username);
        return dirRepository.findByOwnerOrderByCreatedAtDesc(user).stream()
                .map(LocalMediaDirectoryResponse::fromEntity)
                .toList();
    }

    @Transactional
    public LocalMediaDirectoryResponse addDirectory(LocalMediaDirectoryRequest request, String username) {
        User user = getUser(username);
        String normalizedPath = normalizePath(request.getDirPath());

        if (dirRepository.findByOwnerAndDirPath(user, normalizedPath).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该目录已添加：" + normalizedPath);
        }

        LocalMediaDirectory dir = LocalMediaDirectory.builder()
                .owner(user)
                .dirPath(normalizedPath)
                .displayName(request.getDisplayName())
                .build();
        dir = dirRepository.save(dir);

        return LocalMediaDirectoryResponse.fromEntity(scan(dir, user));
    }

    @Transactional
    public void deleteDirectory(Long dirId, String username) {
        LocalMediaDirectory dir = getOwnedDirectory(dirId, username);
        dirRepository.delete(dir);
    }

    @Transactional
    public LocalMediaDirectoryResponse rescan(Long dirId, String username) {
        LocalMediaDirectory dir = getOwnedDirectory(dirId, username);
        User user = getUser(username);
        return LocalMediaDirectoryResponse.fromEntity(scan(dir, user));
    }

    @Transactional(readOnly = true)
    public Page<LocalMediaFileResponse> getFiles(Long dirId, String username,
                                                  String keyword, String mediaType,
                                                  Pageable pageable) {
        LocalMediaDirectory dir = getOwnedDirectory(dirId, username);
        return fileRepository.searchInDirectory(dir, keyword, mediaType, pageable)
                .map(LocalMediaFileResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<LocalMediaFileResponse> getAllFiles(String username,
                                                    String keyword, String mediaType,
                                                    Pageable pageable) {
        User user = getUser(username);
        return fileRepository.searchByOwner(user, keyword, mediaType, pageable)
                .map(LocalMediaFileResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public LocalMediaFile getVerifiedFile(Long fileId, String username) {
        LocalMediaFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "文件不存在"));
        if (!file.getOwner().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权访问该文件");
        }
        return file;
    }

    @Transactional(readOnly = true)
    public File getThumbnail(Long fileId, String username) throws IOException {
        LocalMediaFile file = getVerifiedFile(fileId, username);

        if (!"IMAGE".equals(file.getMediaType())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "非图片文件无缩略图");
        }

        Path cacheDir = Paths.get(System.getProperty("user.home"), ".worknotes", "thumbnails");
        Files.createDirectories(cacheDir);

        Path cachePath = cacheDir.resolve(fileId + ".jpg");
        if (Files.exists(cachePath)) {
            return cachePath.toFile();
        }

        Path source = Paths.get(file.getAbsolutePath());
        if (!Files.exists(source) || !Files.isReadable(source)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "源文件不存在或无法读取");
        }

        Thumbnails.of(source.toFile())
                .size(300, 300)
                .keepAspectRatio(true)
                .outputFormat("jpg")
                .toFile(cachePath.toFile());

        return cachePath.toFile();
    }

    // ---------- internal ----------

    private LocalMediaDirectory scan(LocalMediaDirectory dir, User user) {
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

        List<LocalMediaFile> collected = new ArrayList<>();

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
                    if (!ALL_EXTENSIONS.contains(ext)) return FileVisitResult.CONTINUE;

                    String mediaType = resolveMediaType(ext);
                    String relPath = root.relativize(file).toString();
                    LocalDateTime lastModified = attrs.lastModifiedTime()
                            .toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

                    Integer width = null;
                    Integer height = null;
                    if ("IMAGE".equals(mediaType) && !"svg".equals(ext)) {
                        try {
                            BufferedImage img = ImageIO.read(file.toFile());
                            if (img != null) {
                                width = img.getWidth();
                                height = img.getHeight();
                            }
                        } catch (Exception ignored) {
                            // dimensions remain null — non-fatal
                        }
                    }

                    collected.add(LocalMediaFile.builder()
                            .directory(dir)
                            .owner(user)
                            .fileName(name)
                            .absolutePath(file.toString())
                            .relativePath(relPath)
                            .mediaType(mediaType)
                            .fileExtension(ext)
                            .fileSize(attrs.size())
                            .fileLastModified(lastModified)
                            .imageWidth(width)
                            .imageHeight(height)
                            .build());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });

            boolean truncated = collected.size() >= MAX_FILES;

            fileRepository.deleteByDirectory(dir);
            for (int i = 0; i < collected.size(); i += 500) {
                fileRepository.saveAll(collected.subList(i, Math.min(i + 500, collected.size())));
            }

            dir.setScanStatus("IDLE");
            dir.setLastScanAt(LocalDateTime.now());
            dir.setFileCount(collected.size());
            if (truncated) {
                dir.setLastScanError("文件数量超过上限 " + MAX_FILES + "，已截断显示");
            }

        } catch (IOException e) {
            dir.setScanStatus("ERROR");
            dir.setLastScanError(e.getMessage());
        }

        return dirRepository.save(dir);
    }

    private String resolveMediaType(String ext) {
        if (IMAGE_EXTENSIONS.contains(ext)) return "IMAGE";
        if (VIDEO_EXTENSIONS.contains(ext)) return "VIDEO";
        return "AUDIO";
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

    private LocalMediaDirectory getOwnedDirectory(Long dirId, String username) {
        LocalMediaDirectory dir = dirRepository.findById(dirId)
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
