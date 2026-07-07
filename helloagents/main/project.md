# 项目技术约定

---

## 技术栈
- **语言:** Kotlin 2.3.21
- **构建:** Android Gradle Plugin 9.2.1 / Gradle Wrapper 9.5.1
- **Android SDK:** compileSdk 36 + compileSdkMinor 1，本机 SDK 路径为 `C:\Users\MyPC\AppData\Local\Android\Sdk`
- **UI:** Jetpack Compose BOM 2026.05.01 / AndroidX TV Compose / Coil Compose 3.4.0
- **播放器:** AndroidX Media3 1.10.1
- **弹幕:** com.kuaishou:akdanmaku 1.0.3
- **网络:** Retrofit 3.0.0 / OkHttp 5.3.2
- **并发:** Kotlin Coroutines 1.11.0 / Flow
- **本地状态:** AndroidX DataStore Preferences 1.2.0 / Kotlinx Serialization JSON 1.10.0

---

## 开发约定
- **架构:** MVVM；UI 只消费 ViewModel 暴露的状态，不直接访问 Retrofit。
- **UI组件化:** 正在进行组件库建设，目标是将可复用组件从Screen文件中提取到独立模块。
- **依赖:** 统一维护在 `gradle/libs.versions.toml`。
- **图片加载:** Compose 网络图片统一使用 Coil Compose；媒体图片缺失或加载失败时必须提供稳定占位。
- **TV 视觉:** 核心页面采用 Cinematic Glass 深色玻璃拟态；焦点态使用 Emby Green 到强调黄的渐变边框、200ms 动画和 8dp 阴影，并保留大屏安全区；播放器 OSD 控件通过 `OsdFocusVisualResolver` 统一 focused/selected/primary/disabled 视觉规则。
- **状态面板:** 加载失败和空数据场景优先使用 `ErrorStatePanel` / `EmptyStatePanel`，错误状态需要提供明确图标、说明和可选重试按钮。
- **加载体验:** 主要内容加载态优先使用骨架屏组件，骨架尺寸应贴近实际卡片、列表或详情结构。
- **搜索体验:** 搜索成功后保存最近 20 条历史记录，搜索页空关键词时显示历史记录，支持点击复搜、单条删除和清空。
- **长列表导航:** 媒体库资源数量较多时显示字母索引和滚动位置指示器，索引仅对存在内容的首字母启用。
- **主题系统:** `EmbyTvTheme` 通过 `ThemePreferences` 提供当前颜色和字体偏好；旧组件继续通过 `CinematicGlassColors` 读取主题色。
- **多语言:** 可见 UI 文案优先使用 Android string resource；`ThemePreferences.language` 通过 MainActivity 的 localized Context / Configuration 应用于 Compose `stringResource`，固定语言下 localized Context 必须随 `LocalConfiguration` 变化重建以保留字体、屏幕和系统配置更新；语言偏好通过 `HomeThemePreferenceObserver` 进入 `HomeUiState.themePreferences` 供 ViewModel 状态监听；当前设置页可写入跟随系统、简体中文和英文。
- **可访问性:** 核心可聚焦卡片需要提供 `contentDescription` 或 `Modifier.accessibilityLabel()`，设置页提供高对比度和字体大小偏好。
- **Kotlin 配置:** AGP 9 已内置 Android Kotlin 支持，app 模块不再应用 `org.jetbrains.kotlin.android` 插件；Compose 编译仍使用 `org.jetbrains.kotlin.plugin.compose`。
- **播放器:** 通过 `Media3PlayerFactory` 创建，默认启用 `EXTENSION_RENDERER_MODE_PREFER`；播放页关闭 Media3 默认控制器，使用 Compose OSD 管理 TV 操作。
- **播放器增强:** 播放 OSD 支持 Media3 播放速度切换，当前支持 0.5x、0.75x、1x、1.25x、1.5x、2x；方向键 seek 支持预览缩略图，优先使用 Emby `Chapters` 章节图并以 Thumb/Backdrop/Primary 兜底；顶部展示 Episode 上下文、分辨率、编码和码率；外挂字幕展示规范化语言标签和 External 标记；Media3 事件通过 `PlayerPlaybackController` 映射为可测试 action/effect，OSD 位置以 250ms 受控节奏刷新。
- **FFmpeg 扩展:** Media3 FFmpeg 扩展未发布到 Google Maven，需自行构建 AAR 放入 `app/libs/`。
- **弹幕:** AkDanmaku 通过 `AkDanmakuBridge` 与领域模型隔离，播放器页面通过 `DanmakuOverlay` 应用透明度、字号和显示区域设置。
- **服务器配置:** TV 端使用结构化字段生成 Emby `baseUrl`；手机扫码同步通过 TV 本机临时 HTTP 服务完成。
- **凭证:** Emby 用户名和密码只用于 `/Users/AuthenticateByName`；本地保存 `accessToken`、`userId`、`serverId`、`serverUrl`、`deviceId`、`username` 展示字段和保存时间，不保存密码。
- **新增依赖:** ZXing Core 3.5.4 生成二维码，NanoHTTPD 2.3.1 提供临时手机同步页，AndroidX Security Crypto 1.1.0 保存访问凭证，DataStore Preferences 1.2.0 + Kotlinx Serialization JSON 1.10.0 保存搜索历史、播放历史和显示偏好。

---

## 错误与日志
- **网络错误:** Repository 使用 `Result` 向 ViewModel 返回错误。
- **UI 错误:** Home 页面通过 `errorMessage` 展示可恢复错误。
- **日志:** 当前仅启用 OkHttp BASIC 日志；正式发布前应按构建类型降低敏感输出。
- **敏感信息:** 密码、手机同步请求体和 Emby `accessToken` 禁止进入日志、错误文案或 URL。

---

## 测试与流程
- **单元测试:** 优先覆盖纯 Kotlin 工具、数据转换逻辑和 UI 状态 reducer。
- **Android API 单测:** app 模块启用 `unitTests.isReturnDefaultValues = true`，用于在 JVM 单测中构造 Media3 `TrackGroup` 等会触发轻量 Android framework API 的对象；测试仍应断言业务可观察结果，避免依赖未实现 framework 行为。
- **集成测试:** 后续补充 Emby API fake server 和播放器状态测试。
- **本机 JDK:** `C:\Users\MyPC\.jdks\corretto-17.0.16` 已验证可作为 `JAVA_HOME`，Gradle Launcher JVM 为 17.0.16。
- **Android SDK:** 当前项目通过 `local.properties` 指向 `C:\Users\MyPC\AppData\Local\Android\Sdk`；已安装 `android-36.1`、Build Tools `36.0.0/36.1.0/37.0.0` 和 Platform Tools。
- **验证命令:** `.\gradlew.bat :app:testDebugUnitTest`、`.\gradlew.bat :app:assembleDebug`；播放器运行前置预检使用 `.\scripts\player-runtime-preflight.ps1`，默认运行播放器 JVM 测试、Debug 构建、APK 存在性检查、ADB 设备检查和 AVD 列表检查。连接 Android TV 或模拟器后可追加 `-Install -Launch -CaptureLogcat` 安装 Debug APK、启动 `com.embytv/.MainActivity` 并采集短时 logcat 到 `build/player-runtime/`；脚本会优先按 `com.embytv` 应用进程过滤日志并扫描启动崩溃、ANR、Media3/ExoPlayer 关键错误。真实 Emby 播放验收可追加 `-RequirePlaybackReports`，要求 logcat 中出现 `EmbyTvPlaybackReport` 的 Started、Progress 和 Stopped 成功诊断。
- **本机执行提示:** 若 Gradle 报 JVM 版本不足，先设置 `JAVA_HOME=C:\Users\MyPC\.jdks\corretto-17.0.16` 并将其 `bin` 放入 `PATH` 后重试。
