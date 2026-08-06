/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.integration.wechat.repository;

import com.entropybits.worknotes.spring_boot.integration.wechat.entity.WechatBinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WechatBindingRepository extends JpaRepository<WechatBinding, Long> {

    Optional<WechatBinding> findByOpenidAndStatus(String openid, WechatBinding.Status status);

    Optional<WechatBinding> findByBindCodeAndStatus(String bindCode, WechatBinding.Status status);

    List<WechatBinding> findByUserIdAndStatus(Long userId, WechatBinding.Status status);

    List<WechatBinding> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, WechatBinding.Status status);

    boolean existsByBindCodeAndStatus(String bindCode, WechatBinding.Status status);
}
