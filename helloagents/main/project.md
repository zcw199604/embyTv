# 项目技术约定

---

## 技术栈
- **语言:** Kotlin 2.3.21
- **构建:** Android Gradle Plugin 9.2.1 / Gradle Wrapper 9.5.1
- **UI:** Jetpack Compose BOM 2026.05.01 / AndroidX TV Compose
- **播放器:** AndroidX Media3 1.10.1
- **弹幕:** com.kuaishou:akdanmaku 1.0.3
- **网络:** Retrofit 3.0.0 / OkHttp 5.3.2
- **并发:** Kotlin Coroutines 1.11.0 / Flow

---

## 开发约定
- **架构:** MVVM；UI 只消费 ViewModel 暴露的状态，不直接访问 Retrofit。
- **依赖:** 统一维护在 `gradle/libs.versions.toml`。
- **Kotlin 配置:** AGP 9 已内置 Android Kotlin 支持，app 模块不再应用 `org.jetbrains.kotlin.android` 插件；Compose 编译仍使用 `org.jetbrains.kotlin.plugin.compose`。
- **播放器:** 通过 `Media3PlayerFactory` 创建，默认启用 `EXTENSION_RENDERER_MODE_PREFER`。
- **FFmpeg 扩展:** Media3 FFmpeg 扩展未发布到 Google Maven，需自行构建 AAR 放入 `app/libs/`。
- **弹幕:** AkDanmaku 通过 `AkDanmakuBridge` 与领域模型隔离。

---

## 错误与日志
- **网络错误:** Repository 使用 `Result` 向 ViewModel 返回错误。
- **UI 错误:** Home 页面通过 `errorMessage` 展示可恢复错误。
- **日志:** 当前仅启用 OkHttp BASIC 日志；正式发布前应按构建类型降低敏感输出。

---

## 测试与流程
- **单元测试:** 优先覆盖纯 Kotlin 工具与数据转换逻辑。
- **集成测试:** 后续补充 Emby API fake server 和播放器状态测试。
- **本机 JDK:** `C:\Users\MyPC\.jdks\corretto-17.0.16` 已验证可作为 `JAVA_HOME`，Gradle Launcher JVM 为 17.0.16。
- **Android SDK:** 当前未检测到 `ANDROID_HOME` 或 `ANDROID_SDK_ROOT`；运行 Gradle Android 任务前需配置 SDK 或 `local.properties` 的 `sdk.dir`。
- **验证命令:** `.\gradlew.bat :app:testDebugUnitTest`、`.\gradlew.bat :app:assembleDebug`。
