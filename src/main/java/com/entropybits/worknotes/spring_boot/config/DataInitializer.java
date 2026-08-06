/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot.config;

import com.entropybits.worknotes.spring_boot.entity.Note;
import com.entropybits.worknotes.spring_boot.entity.Tag;
import com.entropybits.worknotes.spring_boot.entity.User;
import com.entropybits.worknotes.spring_boot.repository.NoteRepository;
import com.entropybits.worknotes.spring_boot.repository.TagRepository;
import com.entropybits.worknotes.spring_boot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 数据初始化配置
 * 仅在开发环境（dev profile）下运行
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final NoteRepository noteRepository;
    private final TagRepository tagRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner initData() {
        return args -> {
            // 检查是否已有数据
            if (noteRepository.count() > 0) {
                log.info("数据库已有笔记数据，跳过初始化");
                return;
            }

            log.info("开始初始化示例数据...");

            // 1. 获取或创建测试用户
            User user = userRepository.findByUsername("admin")
                    .orElseGet(() -> {
                        log.info("创建测试用户: admin");
                        User newUser = User.builder()
                                .username("admin")
                                .password(passwordEncoder.encode("admin123"))
                                .email("admin@example.com")
                                .role("ADMIN")
                                .build();
                        User savedUser = userRepository.save(newUser);
                        return savedUser != null ? savedUser : newUser;
                    });

            // 2. 创建标签
            Tag tagVue = getOrCreateTag("Vue.js", user);
            Tag tagBackend = getOrCreateTag("Backend", user);
            Tag tagJavaScript = getOrCreateTag("JavaScript", user);
            Tag tagDevOps = getOrCreateTag("DevOps", user);
            Tag tagTutorial = getOrCreateTag("Tutorial", user);

            // 3. 创建笔记
            createNote(
                    "Building Scalable SaaS Applications with Vue.js and Node.js",
                    "<h1>Building Scalable SaaS Applications</h1><p>A comprehensive guide to architecting multi-tenant SaaS platforms. Learn about database design, authentication, and deployment strategies.</p><p>This article covers:</p><ul><li>Multi-tenant architecture patterns</li><li>Database design for SaaS applications</li><li>Authentication and authorization</li><li>Deployment strategies</li></ul>",
                    "A comprehensive guide to architecting multi-tenant SaaS platforms. Learn about database design, authentication, and deployment strategies...",
                    user,
                    Set.of(tagVue),
                    LocalDateTime.of(2024, 11, 18, 10, 0)
            );

            createNote(
                    "Optimizing API Performance with Caching Strategies",
                    "<h1>Optimizing API Performance</h1><p>Exploring Redis caching, database query optimization, and CDN integration to reduce response times by 80%.</p><p>Key topics:</p><ul><li>Redis caching strategies</li><li>Database query optimization</li><li>CDN integration</li><li>Performance monitoring</li></ul>",
                    "Exploring Redis caching, database query optimization, and CDN integration to reduce response times by 80%...",
                    user,
                    Set.of(tagBackend),
                    LocalDateTime.of(2024, 11, 12, 10, 0)
            );

            createNote(
                    "Modern JavaScript: ES2024 Features You Should Know",
                    "<h1>Modern JavaScript: ES2024 Features</h1><p>Deep dive into the latest JavaScript features including decorators, pipeline operator, and temporal API.</p><p>Features covered:</p><ul><li>Decorators</li><li>Pipeline operator</li><li>Temporal API</li><li>Pattern matching</li></ul>",
                    "Deep dive into the latest JavaScript features including decorators, pipeline operator, and temporal API...",
                    user,
                    Set.of(tagJavaScript),
                    LocalDateTime.of(2024, 11, 5, 10, 0)
            );

            createNote(
                    "CI/CD Pipeline Setup with GitHub Actions",
                    "<h1>CI/CD Pipeline Setup</h1><p>Step-by-step tutorial on automating your deployment workflow. Includes testing, building, and deploying to production.</p><p>Topics:</p><ul><li>GitHub Actions workflow</li><li>Automated testing</li><li>Build and deployment</li><li>Production deployment</li></ul>",
                    "Step-by-step tutorial on automating your deployment workflow. Includes testing, building, and deploying to production...",
                    user,
                    Set.of(tagDevOps),
                    LocalDateTime.of(2024, 10, 28, 10, 0)
            );

            createNote(
                    "Vue 3 Composition API: Best Practices",
                    "<h1>Vue 3 Composition API: Best Practices</h1><p>Practical tips for writing clean, maintainable code with Composition API. Includes examples of custom hooks and state management.</p><p>Best practices:</p><ul><li>Composition API patterns</li><li>Custom hooks</li><li>State management</li><li>Code organization</li></ul>",
                    "Practical tips for writing clean, maintainable code with Composition API. Includes examples of custom hooks and state management...",
                    user,
                    Set.of(tagTutorial),
                    LocalDateTime.of(2024, 10, 21, 10, 0)
            );

            createNote(
                    "Microservices Architecture: From Monolith to Services",
                    "<h1>Microservices Architecture</h1><p>Lessons learned from migrating a monolithic application to microservices. Covers service communication, data consistency, and monitoring.</p><p>Migration topics:</p><ul><li>Service communication</li><li>Data consistency</li><li>Monitoring and logging</li><li>Deployment strategies</li></ul>",
                    "Lessons learned from migrating a monolithic application to microservices. Covers service communication, data consistency, and monitoring...",
                    user,
                    Set.of(tagBackend),
                    LocalDateTime.of(2024, 10, 15, 10, 0)
            );

            createNote(
                    "Docker Best Practices for Development and Production",
                    "<h1>Docker Best Practices</h1><p>Optimize your Docker workflow with multi-stage builds, layer caching, and security best practices.</p><p>Best practices:</p><ul><li>Multi-stage builds</li><li>Layer caching</li><li>Security practices</li><li>Production optimization</li></ul>",
                    "Optimize your Docker workflow with multi-stage builds, layer caching, and security best practices...",
                    user,
                    Set.of(tagDevOps),
                    LocalDateTime.of(2024, 10, 8, 10, 0)
            );

            createNote(
                    "Building Real-time Applications with WebSockets",
                    "<h1>Building Real-time Applications</h1><p>Implementing real-time features using Socket.io and Vue.js. Includes chat applications, live notifications, and collaborative editing.</p><p>Real-time features:</p><ul><li>WebSocket connections</li><li>Chat applications</li><li>Live notifications</li><li>Collaborative editing</li></ul>",
                    "Implementing real-time features using Socket.io and Vue.js. Includes chat applications, live notifications, and collaborative editing...",
                    user,
                    Set.of(tagJavaScript),
                    LocalDateTime.of(2024, 10, 2, 10, 0)
            );

            log.info("示例数据初始化完成！共创建 {} 条笔记", noteRepository.count());
        };
    }

    /**
     * 获取或创建标签
     */
    private Tag getOrCreateTag(String name, User owner) {
        return tagRepository.findByNameAndOwner(name, owner)
                .orElseGet(() -> {
                    Tag tag = Tag.builder()
                            .name(name)
                            .owner(owner)
                            .build();
                    Tag savedTag = tagRepository.save(tag);
                    return savedTag != null ? savedTag : tag;
                });
    }

    /**
     * 创建笔记
     */
    private void createNote(String title, String content, String summary, User owner, Set<Tag> tags, LocalDateTime createdAt) {
        Note note = Note.builder()
                .title(title)
                .content(content)
                .contentType("richtext")
                .owner(owner)
                .isPublic(true)
                .summary(summary)
                .tags(new HashSet<>(tags))
                .viewCount(0)
                .build();

        // 设置创建时间和修改时间
        note.setCreatedAt(createdAt);
        note.setModifiedAt(createdAt);

        noteRepository.save(note);
        log.info("创建笔记: {}", title);
    }
}

