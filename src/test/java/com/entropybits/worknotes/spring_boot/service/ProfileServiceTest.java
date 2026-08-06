/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.service;

import com.entropybits.worknotes.spring_boot.dto.ProfileResponse;
import com.entropybits.worknotes.spring_boot.repository.ProfileRepository;
import com.entropybits.worknotes.spring_boot.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock ProfileRepository profileRepository;
    @Mock UserRepository userRepository;

    @Test
    void getOwnerProfileReturnsEmptyResponseWhenNoProfileExists() {
        ProfileService service = new ProfileService(profileRepository, userRepository);
        when(profileRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());

        ProfileResponse response = service.getOwnerProfile();

        assertThat(response).isNotNull();
        assertThat(response.getId()).isNull();
        assertThat(response.getUsername()).isNull();
    }
}
