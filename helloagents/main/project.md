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

---

## 开发约定
- **架构:** MVVM；UI 只消费 ViewModel 暴露的状态，不直接访问 Retrofit。
- **依赖:** 统一维护在 `gradle/libs.versions.toml`。
- **图片加载:** Compose 网络图片统一使用 Coil Compose；媒体图片缺失或加载失败时必须提供稳定占位。
- **TV 视觉:** 核心页面采用 Cinematic Glass 深色玻璃拟态；焦点态使用 Emby Green，并保留大屏安全区。
- **Kotlin 配置:** AGP 9 已内置 Android Kotlin 支持，app 模块不再应用 `org.jetbrains.kotlin.android` 插件；Compose 编译仍使用 `org.jetbrains.kotlin.plugin.compose`。
- **播放器:** 通过 `Media3PlayerFactory` 创建，默认启用 `EXTENSION_RENDERER_MODE_PREFER`；播放页关闭 Media3 默认控制器，使用 Compose OSD 管理 TV 操作。
- **FFmpeg 扩展:** Media3 FFmpeg 扩展未发布到 Google Maven，需自行构建 AAR 放入 `app/libs/`。
- **弹幕:** AkDanmaku 通过 `AkDanmakuBridge` 与领域模型隔离。
- **服务器配置:** TV 端使用结构化字段生成 Emby `baseUrl`；手机扫码同步通过 TV 本机临时 HTTP 服务完成。
- **凭证:** Emby 用户名和密码只用于 `/Users/AuthenticateByName`；本地保存 `accessToken`、`userId`、`serverId`、`serverUrl`、`deviceId`、`username` 展示字段和保存时间，不保存密码。
- **新增依赖:** ZXing Core 3.5.4 生成二维码，NanoHTTPD 2.3.1 提供临时手机同步页，AndroidX Security Crypto 1.1.0 保存访问凭证。

---

## 错误与日志
- **网络错误:** Repository 使用 `Result` 向 ViewModel 返回错误。
- **UI 错误:** Home 页面通过 `errorMessage` 展示可恢复错误。
- **日志:** 当前仅启用 OkHttp BASIC 日志；正式发布前应按构建类型降低敏感输出。
- **敏感信息:** 密码、手机同步请求体和 Emby `accessToken` 禁止进入日志、错误文案或 URL。

---

## 测试与流程
- **单元测试:** 优先覆盖纯 Kotlin 工具、数据转换逻辑和 UI 状态 reducer。
- **集成测试:** 后续补充 Emby API fake server 和播放器状态测试。
- **本机 JDK:** `C:\Users\MyPC\.jdks\corretto-17.0.16` 已验证可作为 `JAVA_HOME`，Gradle Launcher JVM 为 17.0.16。
- **Android SDK:** 当前项目通过 `local.properties` 指向 `C:\Users\MyPC\AppData\Local\Android\Sdk`；已安装 `android-36.1`、Build Tools `36.0.0/36.1.0/37.0.0` 和 Platform Tools。
- **验证命令:** `.\gradlew.bat :app:testDebugUnitTest`、`.\gradlew.bat :app:assembleDebug`。
