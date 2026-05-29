# Changelog

本文件记录项目所有重要变更。
格式基于 Keep a Changelog，版本号遵循语义化版本。

## [Unreleased]

### 计划中
- **[Phase 2 - v0.4.0]** 功能增强：搜索历史、骨架屏加载、长列表快速导航、组件库重构
- **[Phase 3 - v0.5.0]** 架构优化：完整组件库、多主题支持、高级播放器功能、可访问性完善

### 新增
- 新增搜索页，支持按关键词查询 Movie、Series、Episode、BoxSet 和 Playlist，并以 `Items` 为准展示结果。
- 新增合集、播放列表、类型、演员发现页，入口详情可展示真实 Emby 媒体资源。
- 新增 Emby 用户态写操作: 收藏/取消收藏、标记已播放/未播放、清除继续观看进度。
- 新增剧集播放队列，播放器 OSD 上一集/下一集可切换队列资源，自然结束可自动播放下一集。
- 新增播放器 Media3 音轨/字幕轨道面板，支持选择音轨、选择字幕和关闭字幕。
- 新增加密凭证列表模型，兼容旧单凭证并按服务器+用户去重保存。
- 新增 Movie/Series 媒体详情页，Movie 显示简介、演员、类型、年份、评分和播放按钮，Series 显示多季列表。
- 新增 Emby 详情、季列表和季内 Episode 接口聚合: `Users/{userId}/Items/{itemId}`、`Shows/{seriesId}/Seasons`、`Shows/{seriesId}/Episodes`。
- 新增季内 Episode 列表，Episode 卡片可继续进入既有播放详情读取和播放状态上报路径。

### 变更
- 图片加载改为通过 Coil `ImageRequest` 注入 `X-Emby-Authorization`，避免认证 Emby 服务器封面请求缺少 token。
- 搜索请求改为单一可取消协程链路，旧关键词慢返回不会覆盖当前关键词结果。
- 删除保存身份和清除继续观看进度改为二次确认，Back/取消不会执行危险操作。
- Emby 客户端认证头和首页显示版本统一使用 Gradle `versionName`。
- 播放器上一集/下一集切换前会停止当前媒体上报，播放进度轮询跟随当前 `PlaybackSource`。
- 抽屉导航整合搜索、收藏、合集、播放列表、类型、演员和媒体库入口，均支持 TV OK/Enter 操作。
- 详情页增加收藏、播放状态和清除进度操作按钮，成功后刷新 Emby 真实状态。
- 首页 Dashboard 额外读取 `Shows/NextUp`，空结果不影响原有首页展示。
- 首页、媒体库列表和收藏页中 Movie/Series 卡片 OK 行为改为进入详情页；Episode 保持直接播放。
- Series 季卡片使用 `UserData.UnplayedItemCount` 显示”剩 n 集”角标，缺失或为 0 时不显示。
- 媒体详情页补齐媒体信息和演员信息独立展示区，电影详情提供更明显的”播放”按钮，电视剧详情提供”查看季列表”入口。
- 已保存 token 冷启动时优先恢复凭证，只有无凭证或凭证失效时才启动手机扫码同步服务。
- 首页 Dashboard 聚合改为受控并发加载，Emby API service 按服务器和 token 复用，图片 URL 按用途追加尺寸参数。

### 修复
- 修复自动/手动切集后进度、暂停或退出可能继续上报到旧媒体的问题。
- 修复认证服务器图片接口可能因 Coil 图片请求未带 Emby 认证头而显示占位图的问题。
- 修复快速输入搜索关键词时旧搜索结果可能反写覆盖新结果的问题。
- 修复首页和 Emby 后台会话版本仍显示 `0.2.0` 的硬编码问题。
- 补齐 Coil 3 `coil-network-okhttp` 依赖，修复 Emby 媒体库、媒体卡片和详情页网络封面全部显示占位的问题。
- 修复 TV 遥控器在媒体卡片、媒体库卡片和通用图标按钮上需要按两次 OK/Enter 才触发点击的问题。

### 验证
- `.\gradlew.bat :app:testDebugUnitTest` 通过。
- `.\gradlew.bat :app:assembleDebug` 通过。

## [0.3.0] - 2026-05-29

### 新增
- 新增 UI 状态面板组件，错误状态支持网络、认证、404、服务器和未知类型图标，并可展示遥控器可聚焦的重试按钮。
- 新增空状态面板、通用动画规范和 UI Modifier 扩展，沉淀 Cinematic Glass 组件尺寸、语义色和进度条尺寸常量。
- 播放器 OSD 新增缓冲进度状态，进度条改为灰色缓冲层 + 绿色播放层的双层展示。

### 变更
- 网络图片统一构建 Coil `ImageRequest`，保留 Emby 图片认证 Header，增加 300ms crossfade、加载占位和失败占位。
- 通用 `GlassPanel` 焦点态升级为 200ms 动画、3dp 渐变边框和 8dp 阴影，增强 TV 遥控器焦点可见性。
- 收藏页、媒体库页、凭证页等原有状态提示通过新状态面板展示，错误场景可直接提供重试操作。
- 应用版本提升至 `0.3.0`。

### 验证
- `.\gradlew.bat :app:testDebugUnitTest` 通过。
- `.\gradlew.bat :app:assembleDebug` 通过。

## [0.2.0] - 2026-05-28

### 新增
- 落地 Cinematic Glass TV 核心体验: 服务器配置页、首页媒体中心、播放 Compose OSD 与弹幕开关。
- 新增 Coil Compose 3.4.0 用于 Emby 媒体图片加载。
- 新增首页 Dashboard 映射和播放器 OSD reducer 单元测试。
- 新增 Emby 首页真实 Dashboard 聚合: `Views`、按库统计、`Resume`、`Latest`。
- 新增 `PlaybackDetails`，播放器 OSD 可展示 Emby `PlaybackInfo` 返回的真实容器、编码、画质、音轨和字幕状态。
- 新增首页按媒体库展示最新入库资源分区，数据来自每个媒体库的 `ParentId + DateCreated` 查询。
- 新增 Emby Playback Check-ins 上报，播放开始、进度、暂停/恢复、快退/快进和停止播放会同步到服务器后台。
- 新增媒体库资源列表页，首页或抽屉对媒体库按 OK/Enter 可进入该库首屏资源列表。
- 新增收藏资源页，抽屉进入后按电影和电视剧两个维度展示收藏资源，并为每个卡片显示图片区域和资源名字。

### 变更
- 验证 `C:\Users\MyPC\.jdks\corretto-17.0.16` 可用于 Gradle，记录当前 Android SDK 路径仍缺失。
- 配置本机 Android SDK 路径 `C:\Users\MyPC\AppData\Local\Android\Sdk`，并将 `compileSdk` 调整为 36 + `compileSdkMinor = 1` 以匹配已安装的 `android-36.1`。
- 播放页关闭 Media3 默认控制器，改用 Compose OSD 管理播放、进度、返回键和弹幕快捷入口。
- 完善 TV 遥控器操作闭环: 抽屉 Back/焦点管理、首页禁用入口提示、播放 OSD 焦点与未实现入口反馈。
- 拆分 Emby 服务器配置字段，新增手机扫码同步到 TV 表单，并保存用户名展示字段与 Emby token 凭证，不保存密码。
- 首页首屏不再全量拉取 Movie/Episode 列表，改为按 Emby 真实聚合接口加载媒体库、继续观看和最近入库。
- 首页媒体库卡片显示真实媒体库名称与封面；继续观看和按库最新资源卡片优先展示 Thumb/Backdrop 缩略图，并为 Episode 展示真实剧名和 SxxExx 信息。
- 压缩 TV 端 Emby 配置页布局，用户名和密码改为同一行显示，连接按钮文案改为“确定连接”，避免 1080p 电视上底部字段被裁剪。
- 首页按库最新资源改为使用 `Items/Latest?ParentId=...`；电影库按 Movie 展示，剧集库按 Series 维度展示并显示剩余未播放集数角标。
- 收藏页使用 `Filters=IsFavorite` 拉取 Movie、Series 和 Episode，Episode 收藏会聚合为 Series 卡片，避免电视剧重复刷屏。

### 修复
- 移除 AGP 9 下不再需要的 `org.jetbrains.kotlin.android` 插件配置，避免 Gradle 构建在插件应用阶段失败。
- 修复 Android SDK 36.1 的 Gradle 配置方式，避免误用 `compileSdk = "android-36.1"` 或 `compileSdkExtension = 20`。
- 移除生产入口中的样例播放、Big Buck Bunny 和播放器硬编码 `HEVC / 4K HDR` 展示。
- 修复手机扫码同步提交时 NanoHTTPD 表单参数读取不稳定导致的“配对令牌无效”，同步成功后 token 在设置页生命周期内继续有效。
- 修复媒体库、电影和剧集封面字段兼容不足的问题，支持 `PrimaryImageTag`、父级图片字段、`SeriesPrimaryImageTag` 和无 tag 图片端点兜底。

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
