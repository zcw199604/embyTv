# embyTv

面向 Android TV 的 Emby 客户端初始化工程，目标技术栈如下：

- UI: Jetpack Compose + AndroidX TV Compose
- 播放器: AndroidX Media3，预留 FFmpeg 扩展 AAR 接入
- 弹幕: 快手 AkDanmaku
- 网络: Retrofit + OkHttp
- 架构: MVVM + Kotlin Coroutines + Flow

## 环境要求

- Android Studio 可识别的 Android SDK，建议安装 `compileSdk 36`
- JDK 17 或更高版本
- 使用仓库内 Gradle Wrapper: `./gradlew` 或 `gradlew.bat`

当前仓库会自动包含 `app/libs/*.aar` 和 `app/libs/*.jar`。Media3 FFmpeg 扩展未发布到 Google Maven，需要从 Media3 源码自行构建后放入 `app/libs/`，播放器已配置 `EXTENSION_RENDERER_MODE_PREFER`。

## 常用命令

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

## 目录概览

```text
app/src/main/java/com/embytv
  core/        基础设施、网络、播放器、弹幕桥接
  data/        Emby DTO、API、Repository
  domain/      领域模型
  ui/          Compose 页面、ViewModel、主题
```
