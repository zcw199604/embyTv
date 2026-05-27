# Changelog

本文件记录项目所有重要变更。
格式基于 Keep a Changelog，版本号遵循语义化版本。

## [Unreleased]

### 新增
- 落地 Cinematic Glass TV 核心体验: 服务器配置页、首页媒体中心、播放 Compose OSD 与弹幕开关。
- 新增 Coil Compose 3.4.0 用于 Emby 媒体图片加载。
- 新增首页 Dashboard 映射和播放器 OSD reducer 单元测试。

### 变更
- 验证 `C:\Users\MyPC\.jdks\corretto-17.0.16` 可用于 Gradle，记录当前 Android SDK 路径仍缺失。
- 配置本机 Android SDK 路径 `C:\Users\MyPC\AppData\Local\Android\Sdk`，并将 `compileSdk` 调整为 36 + `compileSdkMinor = 1` 以匹配已安装的 `android-36.1`。
- 播放页关闭 Media3 默认控制器，改用 Compose OSD 管理播放、进度、返回键和弹幕快捷入口。

### 修复
- 移除 AGP 9 下不再需要的 `org.jetbrains.kotlin.android` 插件配置，避免 Gradle 构建在插件应用阶段失败。
- 修复 Android SDK 36.1 的 Gradle 配置方式，避免误用 `compileSdk = "android-36.1"` 或 `compileSdkExtension = 20`。

### 验证
- `.\gradlew.bat :app:testDebugUnitTest` 通过。
- `.\gradlew.bat :app:assembleDebug` 通过。

## [0.1.0] - 2026-05-20

### 新增
- 初始化 Android TV 工程，接入 Jetpack Compose、TV Compose、Media3、Retrofit、OkHttp、AkDanmaku。
- 建立 MVVM + Coroutines + Flow 的基础分层。
- 增加 Emby 登录、媒体列表、播放 URL 构造与样例播放入口。
- 增加 Media3 FFmpeg 扩展 AAR 的本地接入预留。

### 变更
- README 更新为工程初始化说明与环境要求。

### 修复
- 无。

### 移除
- 无。
