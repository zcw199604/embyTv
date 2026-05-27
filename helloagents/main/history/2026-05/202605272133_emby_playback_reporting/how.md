# 技术设计: Emby 播放状态上报

目录: `helloagents/main/plan/202605272133_emby_playback_reporting/`

---

## 方案选择

采用最小改动方案: 在现有 `EmbyApi` + `EmbyRepository` 基础上新增播放状态上报能力，并在 `PlayerScreen` 的 Media3 生命周期和 OSD 操作中触发上报。不引入新第三方库，不重构整体导航和播放器架构。

备选方案:
- **方案 A: Repository 封装上报，PlayerScreen 触发事件（推荐）**
  - 优点: 改动小，契合当前分层，容易测试 API 请求字段。
  - 缺点: `PlayerScreen` 仍承担部分事件编排，需要注意副作用去重。
- **方案 B: 新增 PlayerViewModel 管理播放器会话**
  - 优点: 生命周期与协程更规整，后续扩展远程控制更自然。
  - 缺点: 当前改动范围较大，容易把播放器状态重构混入本次需求。
- **方案 C: 在 OkHttp/MediaSource 层监听播放流请求**
  - 优点: UI 侵入少。
  - 缺点: 无法准确表达暂停/恢复/退出，不能满足 Emby Playback Check-ins 契约。

本次采用方案 A，后续若播放器功能继续扩大，再单独规划 PlayerViewModel。

## 官方 API 依据

Emby 官方 Playback Check-ins 文档要求客户端在播放期间向服务器上报播放状态。核心接口:

| 接口 | 触发时机 | 用途 |
|------|----------|------|
| `POST /Sessions/Playing` | 播放开始 | 通知服务器当前会话开始播放某个媒体 |
| `POST /Sessions/Playing/Progress` | 播放中、暂停、恢复、seek | 更新当前位置、播放状态、是否暂停 |
| `POST /Sessions/Playing/Stopped` | 退出播放、播放结束、播放器释放 | 通知服务器停止播放并保存最终位置 |

参考链接:
- [Playback Check-ins](https://dev.emby.media/doc/restapi/Playback-Check-ins.html)
- [POST /Sessions/Playing](https://dev.emby.media/reference/RestAPI/PlaystateService/postSessionsPlaying.html)
- [POST /Sessions/Playing/Progress](https://dev.emby.media/reference/RestAPI/PlaystateService/postSessionsPlayingProgress.html)
- [POST /Sessions/Playing/Stopped](https://dev.emby.media/reference/RestAPI/PlaystateService/postSessionsPlayingStopped.html)

## 接口契约

### 请求头

沿用现有 `X-Emby-Authorization`:

```text
MediaBrowser Client="EmbyTv", Device="Android TV", DeviceId="{deviceId}", Version="0.1.0", Token="{accessToken}"
```

### DTO

新增 `EmbyPlaybackStartRequest`:

| 字段 | 类型 | 说明 |
|------|------|------|
| ItemId | String | Emby 媒体条目 ID |
| MediaSourceId | String? | `PlaybackInfo.MediaSources[].Id` |
| PlaySessionId | String? | `PlaybackInfo.PlaySessionId` |
| PositionTicks | Long | 当前播放位置 ticks，开始时通常为 0 |
| CanSeek | Boolean | 当前客户端支持 seek，固定 true |
| IsPaused | Boolean | 开始播放时 false |

新增 `EmbyPlaybackProgressRequest`:

| 字段 | 类型 | 说明 |
|------|------|------|
| ItemId | String | Emby 媒体条目 ID |
| MediaSourceId | String? | 媒体源 ID |
| PlaySessionId | String? | 播放会话 ID |
| PositionTicks | Long | 当前播放位置 ticks |
| IsPaused | Boolean | 是否暂停 |
| IsMuted | Boolean | 当前固定 false |
| PlayMethod | String | 当前固定 `DirectPlay` |

新增 `EmbyPlaybackStoppedRequest`:

| 字段 | 类型 | 说明 |
|------|------|------|
| ItemId | String | Emby 媒体条目 ID |
| MediaSourceId | String? | 媒体源 ID |
| PlaySessionId | String? | 播放会话 ID |
| PositionTicks | Long | 停止时播放位置 ticks |

### EmbyApi

新增方法:

```kotlin
@POST("Sessions/Playing")
suspend fun reportPlaybackStarted(...)

@POST("Sessions/Playing/Progress")
suspend fun reportPlaybackProgress(...)

@POST("Sessions/Playing/Stopped")
suspend fun reportPlaybackStopped(...)
```

## 播放事件编排

新增 `PlaybackReporter` 或 `EmbyPlaybackReporter`，由 `EmbyRepository` 暴露或直接注入 `AppContainer`。推荐职责:

- 持有当前 `EmbySession`、`deviceId`、`PlaybackSource` 的只读信息。
- 将 Media3 毫秒转换为 Emby ticks: `positionMs.coerceAtLeast(0) * 10_000`。
- 维护一次播放页生命周期内的上报状态:
  - `startedReported`
  - `stoppedReported`
  - `lastProgressAtMs`
  - `lastReportedPositionMs`
  - `lastPausedState`
- 所有接口调用使用 coroutine 异步执行，失败吞掉并记录为 debug 级别或 Result，不影响 UI。

### 触发规则

| 场景 | 触发 |
|------|------|
| `LaunchedEffect(playbackSource)` 完成 `player.prepare()` 后 | `reportStarted(position=0, paused=false)` |
| OSD 播放/暂停按钮切换到暂停 | `reportProgress(position=current, paused=true, force=true)` |
| OSD 播放/暂停按钮切换到播放 | `reportProgress(position=current, paused=false, force=true)` |
| 播放中轮询 | 每 10 秒 `reportProgress(position=current, paused=!player.isPlaying)` |
| 快退/快进后 | `reportProgress(position=target, paused=!player.isPlaying, force=true)` |
| 生命周期 `ON_PAUSE` | `reportProgress(position=current, paused=true, force=true)`，不发送 stopped |
| Back 第二次退出或页面关闭 | `reportStopped(position=current)` |
| Media3 播放状态到 `STATE_ENDED` | `reportStopped(position=duration)` |
| `DisposableEffect.onDispose` | 若未 stopped，兜底 `reportStopped(position=current)` |

## 现有代码落点

- `app/src/main/java/com/embytv/data/remote/EmbyApi.kt`
  - 新增三个 Playback Check-ins 接口。
- `app/src/main/java/com/embytv/data/remote/dto/`
  - 新增或扩展 DTO 文件，承载上报请求。
- `app/src/main/java/com/embytv/data/repository/EmbyRepository.kt`
  - 新增 `reportPlaybackStarted`、`reportPlaybackProgress`、`reportPlaybackStopped` 或创建专用 reporter。
- `app/src/main/java/com/embytv/domain/model/PlaybackSource.kt`
  - 当前已有 `itemId`、`details.playSessionId`、`details.mediaSourceId`，原则上无需扩展。
- `app/src/main/java/com/embytv/ui/EmbyTvApp.kt`
  - 需要把当前 session/deviceId 或 reporter 传给 `PlayerScreen`。若 `HomeViewModel` 已持有 session，可在进入播放时把必要信息塞进 `PlaybackSource` 或新增 `PlaybackContext`。
- `app/src/main/java/com/embytv/ui/player/PlayerScreen.kt`
  - 接入 reporter，并在现有 play/pause、seek、Back、dispose 逻辑中触发。

## 状态边界

- 本地播放控制仍以 Media3 为准，Emby 上报只是服务端状态同步。
- 上报失败不改变 `PlayerOsdState`。
- 生命周期 `ON_PAUSE` 只发暂停进度，不代表用户退出播放页。
- `Stopped` 必须最多发送一次；重复 Back、重组和 dispose 均不能造成重复停止请求。

## 测试策略

### RED

- Repository/service 测试: 调用 start/progress/stop 时 fake API 收到正确路径对应 DTO。
- ticks 转换测试: `1234ms -> 12_340_000 ticks`，负数归零。
- 状态机测试: 重复 start 不重复发送；重复 stop 只发送一次；暂停状态变化强制发送 progress；普通轮询少于 10 秒不发送。
- PlayerScreen 事件 adapter 可拆出纯 Kotlin coordinator 测试，避免 Compose UI 测试复杂化。

### GREEN

- 实现 Retrofit 接口和 DTO。
- 实现 reporter/coordinator。
- 在 PlayerScreen 中接入 reporter。

### VERIFY

- `.\gradlew.bat :app:testDebugUnitTest`
- `.\gradlew.bat :app:assembleDebug`
- 手工安装到 TV 后，在 Emby 后台确认播放、暂停、退出状态变化。

## 安全与性能

- 不记录 access token、完整 streamUrl、真实媒体标题。
- 上报使用现有 authenticated API provider，不在 URL 查询参数里拼接 token。
- Progress 上报默认 10 秒间隔，暂停/恢复/seek/停止为强制上报，避免后台状态滞后。
- 上报异常不得抛到 UI 层，不影响播放和 Back 退出。
- 不新增后台常驻任务；离开播放器后停止定时进度上报。

## 回滚策略

- 可单独移除 reporter 接入点，保留 Media3 播放原逻辑。
- 若 Emby 版本对字段兼容性存在差异，可先保留接口方法，减少请求字段为 `ItemId`、`PositionTicks`、`IsPaused`、`PlaySessionId`、`MediaSourceId` 的最小集合。

## ADR-005: 使用 Playback Check-ins 同步播放状态

- **状态:** 提议
- **日期:** 2026-05-27
- **决策:** 采用 Emby 官方 `Sessions/Playing`、`Sessions/Playing/Progress`、`Sessions/Playing/Stopped` 完成后台播放状态同步。
- **原因:** 这是 Emby 官方为客户端播放状态、继续观看和后台管理提供的标准交互方式，能直接复用现有认证、播放会话和媒体源信息。
- **后果:** 播放器需要维护一次播放生命周期内的上报状态；上报链路必须异步且容错。
