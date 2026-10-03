# bakabaka

东方 Project 主题的聊天应用：AI 智能体聊天 + 真人私信 + 个人空间动态。

## 功能

- **AI 智能体聊天**：与东方 Project 官方角色或用户自建 AI 角色聊天（支持发图让角色识别、逐字打字效果）
- **真人私信**：用户间私信，支持文字、图片、语音输入、语音通话（WebRTC）
- **个人空间**：发动态（文字 + 图片 + 投票），点赞、评论
- **用户主页**：头像、昵称、签名、性别、公司、地址、IP 属地
- **应用内自动更新**：启动检测、强制更新、进度条、后台下载（AES-GCM 加密 + SHA256 校验）

## 项目结构

```
android/   # Android 客户端（Kotlin + Jetpack Compose）
server/    # 后端（Node.js + Express + MySQL）
```

## 构建

### 后端

```bash
cd server
npm install
cp .env.example .env   # 填入真实配置
node src/index.js
```

### 客户端

1. 用 Android Studio 打开 `android/` 目录
2. 在 `app/src/main/java/com/touhou/aichat/Api.kt` 里把 `baseUrl` 改成你的服务器地址
3. 在 `app/src/main/java/com/touhou/aichat/UpdateCrypto.kt` 里把 `KEY_HEX` 改成你自己的 AES 密钥（与 server `.env` 的 `UPDATE_KEY` 一致）
4. 构建安装

## 说明

- 客户端头像、聊天图片均为 base64 存库（MySQL），自用规模足够
- 语音通话为纯 P2P（WebRTC + 免费 STUN），部分蜂窝网络（对称 NAT）可能连不通
- 如需可靠通话/大规模使用，请接入 TURN 服务器和对象存储

## 许可

MIT
