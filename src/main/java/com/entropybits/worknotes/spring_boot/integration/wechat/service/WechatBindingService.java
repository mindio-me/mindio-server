/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.wechat.service;

import com.entropybits.worknotes.spring_boot.entity.User;
import com.entropybits.worknotes.spring_boot.exception.ResourceNotFoundException;
import com.entropybits.worknotes.spring_boot.exception.UnauthorizedException;
import com.entropybits.worknotes.spring_boot.integration.wechat.dto.WechatBindingResponse;
import com.entropybits.worknotes.spring_boot.integration.wechat.entity.WechatBinding;
import com.entropybits.worknotes.spring_boot.integration.wechat.repository.WechatBindingRepository;
import com.entropybits.worknotes.spring_boot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WechatBindingService {

    private static final long CODE_TTL_MINUTES = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final WechatBindingRepository bindingRepository;
    private final UserRepository userRepository;

    @Transactional
    public String generateBindCode(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        bindingRepository.findByUserIdAndStatus(user.getId(), WechatBinding.Status.PENDING)
                .forEach(old -> {
                    old.setStatus(WechatBinding.Status.EXPIRED);
                    bindingRepository.save(old);
                });

        String code = generateUniqueCode();
        WechatBinding binding = WechatBinding.builder()
                .bindCode(code)
                .user(user)
                .status(WechatBinding.Status.PENDING)
                .codeExpiresAt(LocalDateTime.now().plusMinutes(CODE_TTL_MINUTES))
                .build();
        bindingRepository.save(binding);
        return code;
    }

    @Transactional
    public Optional<User> tryConsumeBindCode(String openid, String messageText) {
        if (messageText == null || !messageText.trim().matches("\\d{6}")) {
            return Optional.empty();
        }
        return bindingRepository.findByBindCodeAndStatus(messageText.trim(), WechatBinding.Status.PENDING)
                .filter(b -> b.getCodeExpiresAt() != null && b.getCodeExpiresAt().isAfter(LocalDateTime.now()))
                .map(b -> {
                    b.setOpenid(openid);
                    b.setStatus(WechatBinding.Status.BOUND);
                    b.setBoundAt(LocalDateTime.now());
                    bindingRepository.save(b);
                    return b.getUser();
                });
    }

    @Transactional(readOnly = true)
    public Optional<User> findBoundUser(String openid) {
        return bindingRepository.findByOpenidAndStatus(openid, WechatBinding.Status.BOUND)
                .map(WechatBinding::getUser);
    }

    @Transactional(readOnly = true)
    public List<WechatBindingResponse> listBindings(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        return bindingRepository.findByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), WechatBinding.Status.BOUND)
                .stream().map(WechatBindingResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional
    public void unbind(Long bindingId, String username) {
        WechatBinding binding = bindingRepository.findById(bindingId)
                .orElseThrow(() -> new ResourceNotFoundException("绑定关系不存在"));
        if (!binding.getUser().getUsername().equals(username)) {
            throw new UnauthorizedException("无权限操作此绑定关系");
        }
        bindingRepository.delete(binding);
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = String.valueOf(100000 + RANDOM.nextInt(900000));
        } while (bindingRepository.existsByBindCodeAndStatus(code, WechatBinding.Status.PENDING));
        return code;
    }
}
