# 飞书集成配置指南

本指南将帮助你完成飞书应用的配置和对接。

## 📋 前置条件

1. ✅ 已在飞书开放平台创建应用
2. ✅ 已获得以下信息：
   - **应用名称**
   - **App ID**
   - **App Secret**

## 🔧 配置步骤

### 1. 配置应用凭证

飞书应用凭证（App ID / App Secret）已改为 **按用户存储**（`user_credentials` 表，App Secret 加密存储），不再通过 `application.yml` / 环境变量在服务端全局配置。

你需要做的仅有：
- 配置 **Token 加密密钥**（用于加密存储 access/refresh token 以及 App Secret）

**Windows PowerShell:**
```powershell
$env:FEISHU_TOKEN_CRYPTO_KEY="5IFpnYBy+rssaP2No5u2lgK/NaXDeBKCI9whCf2tQQc="
```

**Linux/Mac:**
```bash
export FEISHU_TOKEN_CRYPTO_KEY="5IFpnYBy+rssaP2No5u2lgK/NaXDeBKCI9whCf2tQQc="
```

随后在前端页面：
- 工作台点击「新建笔记」→「导入飞书文档」时，若未配置会弹窗要求输入 App ID / App Secret
- 或在「个人信息设置」中预先填写并保存

### 2. 生成 Token 加密密钥（可选）

如果你需要生成新的加密密钥，可以使用以下命令：

**Windows PowerShell:**
```powershell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))
```

**Linux/Mac:**
```bash
openssl rand -base64 32
```

生成的密钥应该是 32 bytes 的 Base64 编码字符串。

### 3. 配置飞书开放平台

#### 3.1 设置 OAuth 回调地址

1. 登录 [飞书开放平台](https://open.feishu.cn/)
2. 进入你的应用管理页面
3. 找到 **"安全设置"** 或 **"OAuth 设置"**
4. 添加 **重定向 URL（Redirect URI）**：
   ```
   http://localhost:8082/api/v1/integrations/feishu/oauth/callback
   ```
   
   > 📝 **生产环境**：如果部署到生产环境，请将 `localhost:8082` 替换为你的实际域名，例如：
   > ```
   > https://yourdomain.com/api/v1/integrations/feishu/oauth/callback
   > ```

#### 3.2 配置应用权限

确保你的应用已申请以下权限：

- ✅ **获取用户基本信息**（`user:read`）
- ✅ **查看、编辑和管理云空间文档**（`wiki:read`、`wiki:write`）
- ✅ **查看、编辑和管理云文档**（`docx:read`、`docx:write`）

> 📝 具体权限名称可能因飞书版本而异，请参考飞书开放平台最新文档。

#### 3.3 发布应用（如需要）

- **企业自建应用**：通常无需发布，直接在企业内使用
- **商店应用**：需要提交审核

## 🚀 使用流程

### 1. 启动应用

确保已配置好所有参数后，启动 Spring Boot 应用：

```bash
cd spring-boot
./mvnw spring-boot:run
```

### 2. 用户授权流程

#### 步骤 1：获取授权 URL

前端调用接口获取飞书授权 URL：

```http
GET /api/v1/integrations/feishu/oauth/url
Authorization: Bearer <用户JWT Token>
```

响应示例：
```json
{
  "oauthUrl": "https://open.feishu.cn/open-apis/authen/v1/authorize?app_id=...&redirect_uri=...&state=..."
}
```

#### 步骤 2：用户跳转授权

前端将用户重定向到返回的 `oauthUrl`，用户在飞书页面完成授权。

#### 步骤 3：OAuth 回调

授权成功后，飞书会回调到：
```
http://localhost:8082/api/v1/integrations/feishu/oauth/callback?code=xxx&state=xxx
```

系统会自动：
- 使用 `code` 换取 `access_token`
- 加密存储 token 到数据库
- 重定向到前端首页

#### 步骤 4：检查绑定状态

```http
GET /api/v1/integrations/feishu/status
Authorization: Bearer <用户JWT Token>
```

响应示例：
```json
{
  "bound": true,
  "tenantKey": "xxx",
  "feishuUserId": "xxx",
  "feishuOpenId": "xxx"
}
```

### 3. 使用飞书 Wiki 导入功能

#### 3.1 获取 Wiki 空间列表

```http
GET /api/v1/integrations/feishu/wiki/spaces
Authorization: Bearer <用户JWT Token>
```

#### 3.2 获取空间下的节点列表

```http
GET /api/v1/integrations/feishu/wiki/nodes?spaceId=xxx
Authorization: Bearer <用户JWT Token>
```

#### 3.3 导入单个文档

```http
POST /api/v1/integrations/feishu/wiki/import
Authorization: Bearer <用户JWT Token>
Content-Type: application/json

{
  "spaceId": "xxx",
  "nodeToken": "xxx",
  "nodeTitle": "文档标题（可选）",
  "objType": "docx"
}
```

## 🔍 故障排查

### 问题 1：OAuth 授权失败

**错误信息**：`飞书 OAuth 失败: xxx`

**可能原因**：
- App ID 或 App Secret 配置错误（当前为按用户配置）
- 回调地址未在飞书开放平台配置
- 应用权限不足

**解决方法**：
1. 在前端「连接飞书」弹窗或「个人信息设置」中重新保存正确的 App ID / App Secret
2. 确认飞书开放平台中的回调地址配置正确
3. 检查应用权限是否已申请并生效

### 问题 2：未绑定飞书账号

**错误信息**：`未绑定飞书账号`

**解决方法**：
- 用户需要先完成 OAuth 授权流程（见上面的"用户授权流程"）

### 问题 3：Token 加密密钥错误

**错误信息**：解密失败相关错误

**解决方法**：
- 确保 `token-crypto-key` 是有效的 Base64 编码字符串（32 bytes）
- 如果更换了密钥，需要用户重新授权（因为旧的加密 token 无法解密）

### 问题 4：无法获取 Wiki 数据

**可能原因**：
- 应用权限不足（需要 `wiki:read` 权限）
- Access Token 已过期（需要刷新 token）

**解决方法**：
1. 检查飞书开放平台中的应用权限配置
2. 如果 token 过期，用户需要重新授权

## 📚 API 接口列表

| 接口 | 方法 | 说明 |
|------|------|------|
| `/v1/integrations/feishu/status` | GET | 获取绑定状态 |
| `/v1/integrations/feishu/oauth/url` | GET | 获取 OAuth 授权 URL |
| `/v1/integrations/feishu/oauth/callback` | GET | OAuth 回调（公开接口） |
| `/v1/integrations/feishu/disconnect` | POST | 解绑飞书账号 |
| `/v1/integrations/feishu/wiki/spaces` | GET | 获取 Wiki 空间列表 |
| `/v1/integrations/feishu/wiki/nodes` | GET | 获取节点列表 |
| `/v1/integrations/feishu/wiki/import` | POST | 导入单个文档 |

## 🔐 安全注意事项

1. **不要将 App Secret 提交到代码库**
   - 使用环境变量或配置中心管理敏感信息
   - 生产环境建议使用密钥管理服务（如 AWS Secrets Manager、Azure Key Vault）

2. **Token 加密存储**
   - Access Token 和 Refresh Token 使用 AES-GCM 加密后存储
   - 加密密钥（`token-crypto-key`）应妥善保管

3. **HTTPS 部署**
   - 生产环境必须使用 HTTPS
   - OAuth 回调地址也必须是 HTTPS

4. **权限最小化**
   - 只申请应用实际需要的权限
   - 定期审查权限使用情况

## 📖 参考资源

- [飞书开放平台文档](https://open.feishu.cn/document/)
- [飞书 OAuth 授权流程](https://open.feishu.cn/document/ukTMukTMukTM/ukDNz4SO0MjL5QzM)
- [飞书 Wiki API](https://open.feishu.cn/document/server-docs/docs/wiki-v2/space/get)

---

**配置完成后，重启应用即可开始使用飞书集成功能！** 🎉

