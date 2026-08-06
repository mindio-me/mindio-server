/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.service;

import com.entropybits.worknotes.spring_boot.dto.ProfileRequest;
import com.entropybits.worknotes.spring_boot.dto.ProfileResponse;
import com.entropybits.worknotes.spring_boot.entity.Profile;
import com.entropybits.worknotes.spring_boot.entity.User;
import com.entropybits.worknotes.spring_boot.exception.ResourceNotFoundException;
import com.entropybits.worknotes.spring_boot.repository.ProfileRepository;
import com.entropybits.worknotes.spring_boot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Profile 业务逻辑层
 */
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public ProfileResponse getProfileByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        Profile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("用户资料不存在"));

        return ProfileResponse.fromEntity(profile);
    }

    @Transactional(readOnly = true)
    public ProfileResponse getCurrentUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        Profile profile = profileRepository.findByUser(user)
                .orElse(null);

        // 如果 Profile 不存在，返回一个空的 ProfileResponse（包含用户名）
        if (profile == null) {
            ProfileResponse response = new ProfileResponse();
            response.setUsername(user.getUsername());
            return response;
        }

        return ProfileResponse.fromEntity(profile);
    }

    @Transactional(readOnly = true)
    public ProfileResponse getOwnerProfile() {
        return profileRepository.findFirstByOrderByIdAsc()
                .map(ProfileResponse::fromEntity)
                .orElseGet(ProfileResponse::new);
    }

    @Transactional
    public ProfileResponse createOrUpdateProfile(ProfileRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        Profile profile = profileRepository.findByUser(user)
                .orElse(Profile.builder().user(user).build());

        profile.setFullName(request.getFullName());
        profile.setTitle(request.getTitle());
        profile.setBio(request.getBio());
        profile.setAvatarUrl(request.getAvatarUrl());
        profile.setLocation(request.getLocation());
        profile.setWebsite(request.getWebsite());
        profile.setGithub(request.getGithub());
        profile.setLinkedin(request.getLinkedin());
        profile.setTwitter(request.getTwitter());
        profile.setWechat(request.getWechat());
        profile.setWechatQrUrl(request.getWechatQrUrl());
        profile.setEmail(request.getEmail());
        profile.setSkills(request.getSkills());
        profile.setExperience(request.getExperience());
        profile.setEducation(request.getEducation());

        Profile savedProfile = profileRepository.save(profile);
        return ProfileResponse.fromEntity(savedProfile);
    }
}
