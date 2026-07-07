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
.\scripts\player-runtime-preflight.ps1
.\scripts\player-runtime-preflight.ps1 -Install -Launch -CaptureLogcat
.\scripts\player-runtime-preflight.ps1 -Install -Launch -CaptureLogcat -LogcatSeconds 90 -RequirePlaybackReports
```

连接设备后，预检脚本会优先按 `com.embytv` 进程采集 logcat，并扫描启动崩溃、ANR、Media3/ExoPlayer 关键错误；如需只保留日志不阻断，可追加 `-AllowLogcatIssues`。真实 Emby 播放验收时可追加 `-RequirePlaybackReports`，脚本会检查 `EmbyTvPlaybackReport` 中是否出现 Started、Progress、Stopped 的成功上报诊断。

## 目录概览

```text
app/src/main/java/com/embytv
  core/        基础设施、网络、播放器、弹幕桥接
  data/        Emby DTO、API、Repository
  domain/      领域模型
  ui/          Compose 页面、ViewModel、主题
```
