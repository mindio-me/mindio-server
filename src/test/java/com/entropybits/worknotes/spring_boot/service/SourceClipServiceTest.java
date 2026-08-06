/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.service;

import com.entropybits.worknotes.spring_boot.dto.SourceClipResponse;
import com.entropybits.worknotes.spring_boot.entity.SourceClip;
import com.entropybits.worknotes.spring_boot.exception.ResourceNotFoundException;
import com.entropybits.worknotes.spring_boot.repository.SourceClipRepository;
import com.entropybits.worknotes.spring_boot.repository.TagRepository;
import com.entropybits.worknotes.spring_boot.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SourceClipServiceTest {

    @Mock SourceClipRepository clipRepository;
    @Mock UserRepository userRepository;
    @Mock TagRepository tagRepository;

    private SourceClipService service;

    @Test
    void updateTitle_changesOnlyTitle_preservesOtherFields() {
        service = new SourceClipService(clipRepository, userRepository, tagRepository);
        SourceClip clip = SourceClip.builder()
                .id(1L)
                .sourceType(SourceClip.SourceType.WEBPAGE)
                .title("Old Title")
                .content("<p>original content</p>")
                .extractionMode(SourceClip.ExtractionMode.FULL)
                .extractionStatus(SourceClip.ExtractionStatus.SUCCESS)
                .build();
        when(clipRepository.findById(1L)).thenReturn(Optional.of(clip));
        when(clipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SourceClipResponse response = service.updateTitle(1L, "New Title");

        assertThat(response.getTitle()).isEqualTo("New Title");
        assertThat(clip.getContent()).isEqualTo("<p>original content</p>");
        assertThat(clip.getExtractionStatus()).isEqualTo(SourceClip.ExtractionStatus.SUCCESS);
    }

    @Test
    void updateTitle_throwsWhenClipNotFound() {
        service = new SourceClipService(clipRepository, userRepository, tagRepository);
        when(clipRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateTitle(99L, "x"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
