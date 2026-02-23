# JsonShow

Android JSON 可视化工具，支持多种视图模式和 Google Drive 云同步。

## 功能

- **多视图模式** — 列表、闪卡、树形、语法高亮、表格，5 种方式查看 JSON
- **本地存储** — 保存 JSON 文件到本地，随时打开
- **云同步** — 登录 Google 账号，将文件同步到 Google Drive（存在用户自己的 Drive 中）
- **数据追加** — 向已有 JSON 数组追加数据，自动去重
- **Prompt 模板** — 一键复制 AI Prompt，让 ChatGPT/Claude 生成可用的 JSON 格式
- **隐私优先** — 云同步使用 appDataFolder，仅访问应用专属隐藏文件夹，不触碰用户其他文件

## 技术栈

- Kotlin + Jetpack Compose + Material 3
- Navigation Compose
- Google Sign-In + Google Drive REST API v3
- DataStore Preferences
- 零后端，纯客户端架构

## 构建

```bash
./gradlew assembleDebug
```

## 云同步配置（开发者）

如需启用 Google Drive 同步功能，需在 [Google Cloud Console](https://console.cloud.google.com) 完成：

1. 创建项目并启用 Google Drive API
2. 创建 Android OAuth Client ID（包名 `com.jsonshow` + SHA-1 签名）
3. 创建 Web OAuth Client ID
4. 配置 OAuth 同意屏幕，添加 `drive.appdata` 范围

## 许可证

MIT
