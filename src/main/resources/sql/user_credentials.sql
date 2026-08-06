-- 用户第三方平台凭证表（按用户存储）
-- 注意：项目当前使用 spring.jpa.hibernate.ddl-auto=update，运行时会自动建表；
-- 本文件用于生产环境/手工初始化时参考。

CREATE TABLE IF NOT EXISTS user_credentials (
  id BIGINT NOT NULL AUTO_INCREMENT,
  uid BIGINT NOT NULL,
  provider VARCHAR(50) NOT NULL,
  app_id VARCHAR(200) NOT NULL,
  app_secret VARCHAR(2000) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_credentials_uid_provider (uid, provider),
  KEY idx_user_credentials_uid_provider (uid, provider),
  CONSTRAINT fk_user_credentials_uid FOREIGN KEY (uid) REFERENCES users(id) ON DELETE CASCADE
);


