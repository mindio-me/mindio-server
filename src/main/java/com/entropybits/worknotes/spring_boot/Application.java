/*
 * Copyright (c) 2026 Fasong Wu
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.entropybits.worknotes.spring_boot;

import com.entropybits.worknotes.spring_boot.integration.feishu.FeishuProperties;
import com.entropybits.worknotes.spring_boot.integration.wechat.WechatConfig;
import com.entropybits.worknotes.spring_boot.integration.wechat.WechatInboundConfig;
import com.entropybits.worknotes.spring_boot.integration.reddit.RedditProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({FeishuProperties.class, WechatConfig.class, WechatInboundConfig.class, RedditProperties.class})
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
