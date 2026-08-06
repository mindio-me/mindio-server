/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.wechat.service;

import com.entropybits.worknotes.spring_boot.entity.Note;
import com.entropybits.worknotes.spring_boot.entity.User;
import com.entropybits.worknotes.spring_boot.exception.BadRequestException;
import com.entropybits.worknotes.spring_boot.integration.wechat.*;
import com.entropybits.worknotes.spring_boot.integration.wechat.dto.WechatPublishLogResponse;
import com.entropybits.worknotes.spring_boot.integration.wechat.dto.WechatPublishRequest;
import com.entropybits.worknotes.spring_boot.integration.wechat.entity.WechatPublishLog;
import com.entropybits.worknotes.spring_boot.integration.wechat.repository.WechatPublishLogRepository;
import com.entropybits.worknotes.spring_boot.repository.NoteRepository;
import com.entropybits.worknotes.spring_boot.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WechatIntegrationServiceTest {

    @Mock NoteRepository noteRepository;
    @Mock UserRepository userRepository;
    @Mock WechatPublishLogRepository logRepository;
    @Mock WechatApiClient apiClient;
    @Mock EditorJsToHtmlConverter converter;

    private WechatIntegrationService service;
    private WechatConfig config;
    private Note note;
    private User user;

    @BeforeEach
    void setUp() throws Exception {
        config = new WechatConfig();
        config.setAppId("app-id");
        config.setAppSecret("app-secret");
        config.setDefaultThumbMediaId("default-thumb");

        service = new WechatIntegrationService(
            noteRepository, userRepository, logRepository,
            apiClient, converter, config);

        user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        note = Note.builder()
            .id(10L)
            .title("Test Note")
            .content("{\"blocks\":[]}")
            .contentType("editorjs")
            .owner(user)
            .build();

        when(noteRepository.findById(10L)).thenReturn(Optional.of(note));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(converter.extractImageUrls(any())).thenReturn(List.of());
        when(converter.convert(any(), any())).thenReturn("<p>content</p>");
        when(converter.extractPlainText(any(), anyInt())).thenReturn("digest text");
        when(apiClient.createDraft(any(), any(), any(), any(), any())).thenReturn("draft-media-id");
        when(logRepository.save(any())).thenAnswer(i -> {
            WechatPublishLog logEntry = i.getArgument(0);
            logEntry.setId(100L);
            return logEntry;
        });
    }

    @Test
    void pushDraftSavesSuccessLog() {
        WechatPublishLogResponse response = service.pushDraft(10L, new WechatPublishRequest(), "testuser");

        assertThat(response.getMediaId()).isEqualTo("draft-media-id");
        assertThat(response.getMode()).isEqualTo("DRAFT");
        assertThat(response.getStatus()).isEqualTo("SUCCESS");

        ArgumentCaptor<WechatPublishLog> captor = ArgumentCaptor.forClass(WechatPublishLog.class);
        verify(logRepository).save(captor.capture());
        assertThat(captor.getValue().getWxTitle()).isEqualTo("Test Note");
    }

    @Test
    void pushDraftUsesRequestTitleOverride() {
        WechatPublishRequest request = new WechatPublishRequest();
        request.setTitle("Custom Title");

        service.pushDraft(10L, request, "testuser");

        ArgumentCaptor<WechatPublishLog> captor = ArgumentCaptor.forClass(WechatPublishLog.class);
        verify(logRepository).save(captor.capture());
        assertThat(captor.getValue().getWxTitle()).isEqualTo("Custom Title");
    }

    @Test
    void pushDraftSavesFailLogOnApiError() throws Exception {
        doThrow(new RuntimeException("API error"))
            .when(apiClient).createDraft(any(), any(), any(), any(), any());

        assertThatThrownBy(() -> service.pushDraft(10L, new WechatPublishRequest(), "testuser"))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("发布失败");

        ArgumentCaptor<WechatPublishLog> captor = ArgumentCaptor.forClass(WechatPublishLog.class);
        verify(logRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("FAILED");
    }

    @Test
    void publishBlocksWhenAlreadyPublishedToday() {
        when(logRepository.countByModeAndStatusAndCreatedAtBetween(
            eq("PUBLISHED"), eq("SUCCESS"), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(1L);

        assertThatThrownBy(() -> service.publish(10L, new WechatPublishRequest(), "testuser"))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("今日已群发");
    }

    @Test
    void getLogsReturnsLogsForNote() {
        WechatPublishLog logEntry = WechatPublishLog.builder()
            .id(1L).note(note).user(user)
            .wxTitle("title").mode("DRAFT").status("SUCCESS")
            .build();
        when(logRepository.findByNoteIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(logEntry));

        List<WechatPublishLogResponse> logs = service.getLogs(10L, "testuser");

        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getMode()).isEqualTo("DRAFT");
    }
}
