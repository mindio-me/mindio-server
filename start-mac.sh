#!/bin/bash

# Mac环境启动脚本
# 设置正确的上传路径和其他环境变量

export WORKNOTES_UPLOAD_PATH=/Users/wukaiqi/Documents/WorkDB/worknotes/spring-boot/uploads
export WORKNOTES_UPLOAD_URL_PREFIX=http://localhost:8080/api
export SPRING_PROFILES_ACTIVE=dev

echo "=== Mac环境配置 ==="
echo "上传路径: $WORKNOTES_UPLOAD_PATH"
echo "URL前缀: $WORKNOTES_UPLOAD_URL_PREFIX"
echo "运行环境: $SPRING_PROFILES_ACTIVE"
echo "==================="

# 确保上传目录存在
mkdir -p "$WORKNOTES_UPLOAD_PATH"

# 启动Spring Boot应用
./mvnw spring-boot:run
