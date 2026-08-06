/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.service;

import com.entropybits.worknotes.spring_boot.entity.User;
import com.entropybits.worknotes.spring_boot.entity.UserCredential;
import com.entropybits.worknotes.spring_boot.exception.BadRequestException;
import com.entropybits.worknotes.spring_boot.exception.ResourceNotFoundException;
import com.entropybits.worknotes.spring_boot.integration.feishu.crypto.AesGcmStringEncryptor;
import com.entropybits.worknotes.spring_boot.repository.UserCredentialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserCredentialService {

    public static final String PROVIDER_FEISHU = "feishu";

    private final UserCredentialRepository repository;

    /**
     * 加密 key 统一沿用 feishu.token-crypto-key（已有配置 + 已用于 token 加密存储）。
     * 这样避免引入第二把密钥导致运维复杂。
     */
    private final com.entropybits.worknotes.spring_boot.integration.feishu.FeishuProperties feishuProperties;

    private AesGcmStringEncryptor encryptor() {
        return new AesGcmStringEncryptor(feishuProperties.getTokenCryptoKey());
    }

    @Transactional(readOnly = true)
    public boolean isConfigured(User user, String provider) {
        return repository.existsByUserAndProvider(user, provider);
    }

    @Transactional(readOnly = true)
    public UserCredential getRequired(User user, String provider) {
        return repository.findByUserAndProvider(user, provider)
                .orElseThrow(() -> new ResourceNotFoundException("未配置平台凭证：" + provider));
    }

    @Transactional
    public UserCredential upsertFeishu(User user, String appId, String appSecretPlain) {
        if (appId == null || appId.isBlank()) throw new BadRequestException("App ID 不能为空");
        if (appSecretPlain == null || appSecretPlain.isBlank()) throw new BadRequestException("App Secret 不能为空");

        UserCredential cred = repository.findByUserAndProvider(user, PROVIDER_FEISHU)
                .orElse(UserCredential.builder()
                        .user(user)
                        .provider(PROVIDER_FEISHU)
                        .build());

        cred.setAppId(appId.trim());
        cred.setAppSecretEnc(encryptor().encrypt(appSecretPlain.trim()));
        // updatedAt 由 @UpdateTimestamp 自动维护，但这里确保“脏检查”触发
        cred.setUpdatedAt(LocalDateTime.now());
        return repository.save(cred);
    }

    @Transactional(readOnly = true)
    public String decryptAppSecret(UserCredential credential) {
        if (credential == null) throw new BadRequestException("credential 不能为空");
        return encryptor().decrypt(credential.getAppSecretEnc());
    }
}


