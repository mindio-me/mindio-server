# 文件上传功能说明

## 概述

文件上传功能已从 crmeb java 商城系统移植到 worknotes 项目中。目前支持本地存储，后续可扩展云存储功能（七牛云、阿里云OSS、腾讯云COS、京东云等）。

## 功能特性

1. **图片上传** - 支持常见图片格式（jpg, jpeg, png, gif, bmp, webp, svg）
2. **文件上传** - 支持常见文件格式（pdf, doc, docx, xls, xlsx, ppt, pptx, txt, zip, rar, 7z）
3. **Base64图片上传** - 支持Base64编码的图片上传
4. **附件管理** - 支持附件列表查询、删除等操作

## 配置说明

### application.yml 配置

```yaml
# 文件上传配置
worknotes:
  upload:
    # 文件上传根路径（绝对路径或相对路径）
    path: ./uploads
    # URL前缀（用于访问上传的文件，如：http://localhost:8082/api/uploads）
    url-prefix: 

# Spring文件上传配置
spring:
  servlet:
    multipart:
      # 单个文件最大大小
      max-file-size: 50MB
      # 单次请求文件总大小限制
      max-request-size: 50MB
      # 文件写入磁盘的阈值
      file-size-threshold: 10MB
```

## API 接口

### 1. 图片上传

**接口地址：** `POST /api/v1/upload/image`

**请求参数：**
- `file` (MultipartFile): 图片文件（必填）
- `model` (String): 模块名称，默认 "note"（可选）
- `pid` (Integer): 分类ID，默认 0（可选）
  - 0: 编辑器
  - 1: 笔记图片
  - 2: 用户头像
  - 3: 其他

**响应示例：**
```json
{
  "fileName": "example.jpg",
  "extName": "jpg",
  "fileSize": 123456,
  "url": "worknotesimage/public/note/2024/01/15/abc123def456.jpg",
  "type": "image/jpeg"
}
```

### 2. 文件上传

**接口地址：** `POST /api/v1/upload/file`

**请求参数：**
- `file` (MultipartFile): 文件（必填）
- `model` (String): 模块名称，默认 "note"（可选）
- `pid` (Integer): 分类ID，默认 0（可选）

**响应格式：** 同图片上传

### 3. Base64图片上传

**接口地址：** `POST /api/v1/upload/base64`

**请求体：**
```json
{
  "base64Url": "data:image/jpeg;base64,/9j/4AAQSkZJRg...",
  "model": "note",
  "pid": 0
}
```

**响应格式：** 同图片上传

### 4. 获取附件列表

**接口地址：** `GET /api/v1/attachments`

**请求参数：**
- `pid` (Integer): 分类ID（可选）
- `attType` (String): 附件类型（可选）
- `page` (Integer): 页码，从0开始，默认 0
- `size` (Integer): 每页大小，默认 20
- `sortBy` (String): 排序字段，默认 "createTime"
- `direction` (String): 排序方向，默认 "DESC"

### 5. 获取附件详情

**接口地址：** `GET /api/v1/attachments/{id}`

### 6. 删除附件

**接口地址：** `DELETE /api/v1/attachments`

**请求体：**
```json
[1, 2, 3]
```

## 数据库表结构

附件信息存储在 `attachments` 表中，主要字段：

- `att_id`: 附件ID（主键）
- `name`: 附件名称
- `att_dir`: 附件路径
- `satt_dir`: 压缩图片路径
- `att_size`: 附件大小（字节）
- `att_type`: 附件类型
- `pid`: 分类ID
- `image_type`: 图片上传类型（1-本地, 2-七牛云, 3-OSS, 4-COS, 5-京东云）
- `owner`: 资源归属方（-1-平台, 其他为用户ID）
- `user_id`: 关联的用户ID
- `create_time`: 创建时间
- `update_time`: 更新时间

## 文件存储结构

上传的文件按以下结构存储：

```
uploads/
└── worknotesimage/
    └── public/
        └── {model}/
            └── {year}/
                └── {month}/
                    └── {day}/
                        └── {filename}
```

例如：
```
uploads/worknotesimage/public/note/2024/01/15/abc123def456.jpg
```

## 安全说明

1. 所有上传接口都需要 JWT 认证
2. 文件类型验证：只允许配置的扩展名
3. 文件大小限制：图片最大 10MB，文件最大 50MB
4. 文件名自动生成，避免文件名冲突和安全问题

## 后续扩展

可以扩展以下功能：

1. **云存储支持**
   - 七牛云存储
   - 阿里云OSS
   - 腾讯云COS
   - 京东云存储

2. **图片处理**
   - 图片压缩
   - 缩略图生成
   - 水印添加

3. **文件管理**
   - 文件分类管理
   - 文件移动
   - 文件重命名

4. **CDN支持**
   - 自动添加CDN域名前缀
   - 多CDN切换

## 注意事项

1. 确保上传目录有写入权限
2. 生产环境建议配置 `url-prefix` 使用CDN或静态资源服务器
3. 定期清理未使用的附件文件
4. 建议配置文件大小限制，避免服务器存储空间不足

