/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.wechat.service;

import com.entropybits.worknotes.spring_boot.entity.User;
import com.entropybits.worknotes.spring_boot.exception.UnauthorizedException;
import com.entropybits.worknotes.spring_boot.integration.wechat.dto.WechatBindingResponse;
import com.entropybits.worknotes.spring_boot.integration.wechat.entity.WechatBinding;
import com.entropybits.worknotes.spring_boot.integration.wechat.repository.WechatBindingRepository;
import com.entropybits.worknotes.spring_boot.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WechatBindingServiceTest {

    @Mock private WechatBindingRepository bindingRepository;
    @Mock private UserRepository userRepository;

    private WechatBindingService service;

    private final User alice = User.builder().id(1L).username("alice").build();

    @BeforeEach
    void setUp() {
        service = new WechatBindingService(bindingRepository, userRepository);
    }

    @Test
    void generateBindCodeCreatesSixDigitPendingCodeAndExpiresOldOnes() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        WechatBinding oldPending = WechatBinding.builder()
                .id(10L).user(alice).status(WechatBinding.Status.PENDING).build();
        when(bindingRepository.findByUserIdAndStatus(1L, WechatBinding.Status.PENDING))
                .thenReturn(List.of(oldPending));
        when(bindingRepository.existsByBindCodeAndStatus(anyString(), eq(WechatBinding.Status.PENDING)))
                .thenReturn(false);

        String code = service.generateBindCode("alice");

        assertThat(code).matches("\\d{6}");
        assertThat(oldPending.getStatus()).isEqualTo(WechatBinding.Status.EXPIRED);

        ArgumentCaptor<WechatBinding> savedCaptor = ArgumentCaptor.forClass(WechatBinding.class);
        verify(bindingRepository, times(2)).save(savedCaptor.capture());
        WechatBinding newBinding = savedCaptor.getAllValues().get(1);
        assertThat(newBinding.getBindCode()).isEqualTo(code);
        assertThat(newBinding.getStatus()).isEqualTo(WechatBinding.Status.PENDING);
        assertThat(newBinding.getCodeExpiresAt()).isAfter(LocalDateTime.now().plusMinutes(9));
    }

    @Test
    void tryConsumeBindCodeBindsOnValidUnexpiredCode() {
        WechatBinding pending = WechatBinding.builder()
                .id(20L).bindCode("654321").user(alice)
                .status(WechatBinding.Status.PENDING)
                .codeExpiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
        when(bindingRepository.findByBindCodeAndStatus("654321", WechatBinding.Status.PENDING))
                .thenReturn(Optional.of(pending));

        Optional<User> result = service.tryConsumeBindCode("openid-abc", "654321");

        assertThat(result).contains(alice);
        assertThat(pending.getOpenid()).isEqualTo("openid-abc");
        assertThat(pending.getStatus()).isEqualTo(WechatBinding.Status.BOUND);
        assertThat(pending.getBoundAt()).isNotNull();
        verify(bindingRepository).save(pending);
    }

    @Test
    void tryConsumeBindCodeRejectsExpiredCode() {
        WechatBinding expired = WechatBinding.builder()
                .id(21L).bindCode("111111").user(alice)
                .status(WechatBinding.Status.PENDING)
                .codeExpiresAt(LocalDateTime.now().minusMinutes(1))
                .build();
        when(bindingRepository.findByBindCodeAndStatus("111111", WechatBinding.Status.PENDING))
                .thenReturn(Optional.of(expired));

        Optional<User> result = service.tryConsumeBindCode("openid-abc", "111111");

        assertThat(result).isEmpty();
        assertThat(expired.getStatus()).isEqualTo(WechatBinding.Status.PENDING);
    }

    @Test
    void tryConsumeBindCodeIgnoresNonSixDigitText() {
        Optional<User> result = service.tryConsumeBindCode("openid-abc", "hello mindio");

        assertThat(result).isEmpty();
        verifyNoInteractions(bindingRepository);
    }

    @Test
    void unbindRejectsOtherUsersBinding() {
        User bob = User.builder().id(2L).username("bob").build();
        WechatBinding binding = WechatBinding.builder().id(30L).user(bob).status(WechatBinding.Status.BOUND).build();
        when(bindingRepository.findById(30L)).thenReturn(Optional.of(binding));

        assertThatThrownBy(() -> service.unbind(30L, "alice"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void listBindingsMapsToResponse() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        WechatBinding bound = WechatBinding.builder()
                .id(40L).user(alice).status(WechatBinding.Status.BOUND).boundAt(LocalDateTime.now())
                .build();
        when(bindingRepository.findByUserIdAndStatusOrderByCreatedAtDesc(1L, WechatBinding.Status.BOUND))
                .thenReturn(List.of(bound));

        List<WechatBindingResponse> result = service.listBindings("alice");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(40L);
        assertThat(result.get(0).getStatus()).isEqualTo("BOUND");
    }
}
