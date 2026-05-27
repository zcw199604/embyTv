# Changelog

本文件记录项目所有重要变更。
格式基于 Keep a Changelog，版本号遵循语义化版本。

## [Unreleased]

### 新增
- 落地 Cinematic Glass TV 核心体验: 服务器配置页、首页媒体中心、播放 Compose OSD 与弹幕开关。
- 新增 Coil Compose 3.4.0 用于 Emby 媒体图片加载。
- 新增首页 Dashboard 映射和播放器 OSD reducer 单元测试。
- 新增 Emby 首页真实 Dashboard 聚合: `Views`、按库统计、`Resume`、`Latest`。
- 新增 `PlaybackDetails`，播放器 OSD 可展示 Emby `PlaybackInfo` 返回的真实容器、编码、画质、音轨和字幕状态。
- 新增首页按媒体库展示最新入库资源分区，数据来自每个媒体库的 `ParentId + DateCreated` 查询。

### 变更
- 验证 `C:\Users\MyPC\.jdks\corretto-17.0.16` 可用于 Gradle，记录当前 Android SDK 路径仍缺失。
- 配置本机 Android SDK 路径 `C:\Users\MyPC\AppData\Local\Android\Sdk`，并将 `compileSdk` 调整为 36 + `compileSdkMinor = 1` 以匹配已安装的 `android-36.1`。
- 播放页关闭 Media3 默认控制器，改用 Compose OSD 管理播放、进度、返回键和弹幕快捷入口。
- 完善 TV 遥控器操作闭环: 抽屉 Back/焦点管理、首页禁用入口提示、播放 OSD 焦点与未实现入口反馈。
- 拆分 Emby 服务器配置字段，新增手机扫码同步到 TV 表单，并保存用户名展示字段与 Emby token 凭证，不保存密码。
- 首页首屏不再全量拉取 Movie/Episode 列表，改为按 Emby 真实聚合接口加载媒体库、继续观看和最近入库。
- 首页媒体库卡片显示真实媒体库名称与封面；继续观看和按库最新资源卡片优先展示 Thumb/Backdrop 缩略图，并为 Episode 展示真实剧名和 SxxExx 信息。
- 压缩 TV 端 Emby 配置页布局，用户名和密码改为同一行显示，连接按钮文案改为“确定连接”，避免 1080p 电视上底部字段被裁剪。

### 修复
- 移除 AGP 9 下不再需要的 `org.jetbrains.kotlin.android` 插件配置，避免 Gradle 构建在插件应用阶段失败。
- 修复 Android SDK 36.1 的 Gradle 配置方式，避免误用 `compileSdk = "android-36.1"` 或 `compileSdkExtension = 20`。
- 移除生产入口中的样例播放、Big Buck Bunny 和播放器硬编码 `HEVC / 4K HDR` 展示。
- 修复手机扫码同步提交时 NanoHTTPD 表单参数读取不稳定导致的“配对令牌无效”，同步成功后 token 在设置页生命周期内继续有效。

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
