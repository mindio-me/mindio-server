/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.service;

import com.entropybits.worknotes.spring_boot.entity.LocalMediaDirectory;
import com.entropybits.worknotes.spring_boot.entity.LocalMediaFile;
import com.entropybits.worknotes.spring_boot.entity.User;
import com.entropybits.worknotes.spring_boot.repository.LocalMediaDirectoryRepository;
import com.entropybits.worknotes.spring_boot.repository.LocalMediaFileRepository;
import com.entropybits.worknotes.spring_boot.repository.UserRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LocalMediaServiceTest {

    @Mock LocalMediaDirectoryRepository dirRepository;
    @Mock LocalMediaFileRepository fileRepository;
    @Mock UserRepository userRepository;

    @TempDir Path tempDir;

    private LocalMediaService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new LocalMediaService(dirRepository, fileRepository, userRepository);

        user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    }

    @Test
    void scan_classifiesImageFilesCorrectly() throws IOException {
        Files.createFile(tempDir.resolve("photo.jpg"));
        Files.createFile(tempDir.resolve("screenshot.png"));
        Files.createFile(tempDir.resolve("animation.gif"));
        Files.createFile(tempDir.resolve("ignored.txt"));

        LocalMediaDirectory dir = LocalMediaDirectory.builder()
                .id(1L)
                .owner(user)
                .dirPath(tempDir.toString())
                .scanStatus("IDLE")
                .fileCount(0)
                .build();

        when(dirRepository.findById(1L)).thenReturn(Optional.of(dir));
        when(dirRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.rescan(1L, "testuser");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LocalMediaFile>> captor = ArgumentCaptor.forClass(List.class);
        verify(fileRepository, atLeastOnce()).saveAll(captor.capture());

        List<LocalMediaFile> saved = captor.getAllValues().stream()
                .flatMap(List::stream).toList();

        assertThat(saved).hasSize(3);
        assertThat(saved).allMatch(f -> "IMAGE".equals(f.getMediaType()));
        assertThat(saved.stream().map(LocalMediaFile::getFileExtension))
                .containsExactlyInAnyOrder("jpg", "png", "gif");
    }

    @Test
    void scan_classifiesVideoAndAudioCorrectly() throws IOException {
        Files.createFile(tempDir.resolve("clip.mp4"));
        Files.createFile(tempDir.resolve("song.mp3"));
        Files.createFile(tempDir.resolve("audio.flac"));

        LocalMediaDirectory dir = LocalMediaDirectory.builder()
                .id(2L)
                .owner(user)
                .dirPath(tempDir.toString())
                .scanStatus("IDLE")
                .fileCount(0)
                .build();

        when(dirRepository.findById(2L)).thenReturn(Optional.of(dir));
        when(dirRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.rescan(2L, "testuser");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LocalMediaFile>> captor = ArgumentCaptor.forClass(List.class);
        verify(fileRepository, atLeastOnce()).saveAll(captor.capture());

        List<LocalMediaFile> saved = captor.getAllValues().stream()
                .flatMap(List::stream).toList();

        assertThat(saved).hasSize(3);
        assertThat(saved.stream()
                .filter(f -> "VIDEO".equals(f.getMediaType()))
                .map(LocalMediaFile::getFileExtension))
                .containsExactly("mp4");
        assertThat(saved.stream()
                .filter(f -> "AUDIO".equals(f.getMediaType()))
                .map(LocalMediaFile::getFileExtension))
                .containsExactlyInAnyOrder("mp3", "flac");
    }

    @Test
    void getAllFiles_searchesOnlyTheCurrentUsersFiles() {
        LocalMediaDirectory dir = LocalMediaDirectory.builder()
                .id(3L)
                .owner(user)
                .dirPath(tempDir.toString())
                .build();
        LocalMediaFile file = LocalMediaFile.builder()
                .id(10L)
                .directory(dir)
                .owner(user)
                .fileName("photo.jpg")
                .absolutePath(tempDir.resolve("photo.jpg").toString())
                .mediaType("IMAGE")
                .build();
        PageRequest pageable = PageRequest.of(0, 40);
        when(fileRepository.searchByOwner(user, "photo", "IMAGE", pageable))
                .thenReturn(new PageImpl<>(List.of(file), pageable, 1));

        var result = service.getAllFiles("testuser", "photo", "IMAGE", pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(10L);
        verify(fileRepository).searchByOwner(user, "photo", "IMAGE", pageable);
    }
}
