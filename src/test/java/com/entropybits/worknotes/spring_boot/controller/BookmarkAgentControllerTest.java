/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.controller;

import com.entropybits.worknotes.spring_boot.dto.BookmarkAgentJobResponse;
import com.entropybits.worknotes.spring_boot.entity.BookmarkAgentJob;
import com.entropybits.worknotes.spring_boot.entity.User;
import com.entropybits.worknotes.spring_boot.repository.BookmarkAgentJobRepository;
import com.entropybits.worknotes.spring_boot.repository.UserRepository;
import com.entropybits.worknotes.spring_boot.service.BookmarkAgentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookmarkAgentControllerTest {

    @Mock BookmarkAgentService agentService;
    @Mock BookmarkAgentJobRepository jobRepository;
    @Mock UserRepository userRepository;

    private BookmarkAgentController controller;
    private final UserDetails principal =
            org.springframework.security.core.userdetails.User.withUsername("alice").password("x").authorities("USER").build();

    private void setUp() {
        controller = new BookmarkAgentController(agentService, jobRepository, userRepository);
    }

    @Test
    void generate_resolvesUserAndDelegatesToService() {
        setUp();
        User user = User.builder().id(1L).username("alice").build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        BookmarkAgentJob job = BookmarkAgentJob.builder().id(5L).type(BookmarkAgentJob.Type.CLUSTER)
                .status(BookmarkAgentJob.Status.RUNNING).build();
        when(agentService.generate(BookmarkAgentJob.Type.CLUSTER, user)).thenReturn(job);

        ResponseEntity<BookmarkAgentJobResponse> response = controller.generate(BookmarkAgentJob.Type.CLUSTER, principal);

        assertThat(response.getBody().getId()).isEqualTo(5L);
        assertThat(response.getBody().getStatus()).isEqualTo(BookmarkAgentJob.Status.RUNNING);
    }

    @Test
    void current_returnsLatestJobForType() {
        setUp();
        User user = User.builder().id(1L).username("alice").build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        BookmarkAgentJob job = BookmarkAgentJob.builder().id(5L).type(BookmarkAgentJob.Type.TIMELINE)
                .status(BookmarkAgentJob.Status.DONE).resultNoteId(99L).build();
        when(jobRepository.findTopByOwnerAndTypeOrderByCreatedAtDesc(user, BookmarkAgentJob.Type.TIMELINE))
                .thenReturn(Optional.of(job));

        ResponseEntity<BookmarkAgentJobResponse> response = controller.current(BookmarkAgentJob.Type.TIMELINE, principal);

        assertThat(response.getBody().getResultNoteId()).isEqualTo(99L);
    }

    @Test
    void current_returnsEmptyStateWhenNeverGenerated() {
        setUp();
        User user = User.builder().id(1L).username("alice").build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(jobRepository.findTopByOwnerAndTypeOrderByCreatedAtDesc(user, BookmarkAgentJob.Type.CLUSTER))
                .thenReturn(Optional.empty());

        ResponseEntity<BookmarkAgentJobResponse> response = controller.current(BookmarkAgentJob.Type.CLUSTER, principal);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().getType()).isEqualTo(BookmarkAgentJob.Type.CLUSTER);
        assertThat(response.getBody().getStatus()).isNull();
        assertThat(response.getBody().getId()).isNull();
    }
}
