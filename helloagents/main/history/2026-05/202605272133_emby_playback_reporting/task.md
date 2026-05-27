# 任务清单: Emby 播放状态上报

目录: `helloagents/main/plan/202605272133_emby_playback_reporting/`

---

## 并行子代理标注

- 并行组 A: 任务 [1.1, 2.1, 2.2]；允许写入: `app/src/main/java/com/embytv/data/remote/`, `app/src/main/java/com/embytv/data/repository/`, `app/src/test/java/com/embytv/data/`；冲突域: EmbyApi 与 Repository 播放上报契约；验证: `.\gradlew.bat :app:testDebugUnitTest`
- 并行组 B: 任务 [1.2, 2.3, 2.4]；允许写入: `app/src/main/java/com/embytv/ui/player/`, `app/src/main/java/com/embytv/ui/`, `app/src/test/java/com/embytv/ui/player/`；冲突域: PlayerScreen 播放生命周期和事件协调器；验证: `.\gradlew.bat :app:testDebugUnitTest`
- 不可并行任务: [3.1, 3.2, 4.1, 5.1, 5.2, 5.3, 6.1, 6.2, 6.3]；原因: 集成接入、文档同步和最终验证需在数据/UI 合并后执行。

---

## 0. 方案边界确认
- [√] 0.1 确认本次只实现 Emby Playback Check-ins 上报，不实现远程控制、后台主动管理和音轨/字幕切换。
- [√] 0.2 确认上报失败不得影响 Media3 播放、暂停、seek、Back 退出和弹幕状态。
- [√] 0.3 确认最小改动策略: 不引入 PlayerViewModel 重构，不改导航结构，优先复用现有 Repository 和 `PlaybackSource.details`。

## 1. RED: API 契约与播放事件测试
- [√] 1.1 RED: 新增 `app/src/test/java/com/embytv/data/repository/EmbyPlaybackReportingTest.kt`，断言 start/progress/stop 会向 fake API 发送 `ItemId`、`MediaSourceId`、`PlaySessionId`、`PositionTicks`、`IsPaused` 等字段，验证 why.md#成功标准。
- [√] 1.2 RED: 新增 `app/src/test/java/com/embytv/ui/player/PlaybackReportingCoordinatorTest.kt`，断言播放开始只上报一次、进度少于 10 秒不重复上报、暂停/恢复/seek 强制上报、stop 只上报一次。
- [√] 1.3 RED: 增加 ticks 转换测试，断言 Media3 毫秒转换为 Emby ticks，负值归零。

## 2. GREEN: Emby Playback Check-ins 数据层
- [√] 2.1 扩展 `app/src/main/java/com/embytv/data/remote/EmbyApi.kt`，新增 `POST Sessions/Playing`、`POST Sessions/Playing/Progress`、`POST Sessions/Playing/Stopped`。
- [√] 2.2 新增 `app/src/main/java/com/embytv/data/remote/dto/EmbyPlaybackReportingDtos.kt`，定义 start/progress/stopped 请求 DTO，并使用 `@SerializedName` 对齐 Emby 字段名。
- [√] 2.3 在 `app/src/main/java/com/embytv/data/repository/EmbyRepository.kt` 或新增 `EmbyPlaybackReporter` 中封装 `reportPlaybackStarted`、`reportPlaybackProgress`、`reportPlaybackStopped`，统一构造认证头和容错 `Result`。
- [√] 2.4 实现 `positionMs.toEmbyTicks()`，确保单位转换集中、可测试、负值归零。

## 3. GREEN: 播放器事件协调
- [√] 3.1 新增 `PlaybackReportingCoordinator`，维护 `startedReported`、`stoppedReported`、`lastProgressAtMs`、`lastPausedState`，提供 `onStarted`、`onProgressTick`、`onPauseChanged`、`onSeek`、`onStopped`。
- [√] 3.2 调整 `app/src/main/java/com/embytv/ui/player/PlayerScreen.kt`，在 `LaunchedEffect(playbackSource)`、播放/暂停按钮、快退/快进、进度轮询、生命周期 `ON_PAUSE`、播放结束和 dispose 中调用 coordinator。
- [√] 3.3 调整 `app/src/main/java/com/embytv/ui/EmbyTvApp.kt` 或 `PlaybackSource` 传参方式，使 `PlayerScreen` 能拿到当前 session/deviceId 或可用 reporter；不得把 token 写入日志或 UI 文案。

## 4. 安全、容错与性能
- [√] 4.1 检查所有上报调用在独立 coroutine 中执行，失败不抛到 Compose 层，不阻塞播放和 Back 退出。
- [√] 4.2 检查 Progress 默认 10 秒节流，暂停/恢复/seek/stop 为强制上报，离开播放器后不再继续轮询。
- [√] 4.3 检查日志、测试 fixture 和知识库不包含真实 token、密码、完整播放 URL 或真实私有媒体标题。

## 5. 文档更新
- [√] 5.1 更新 `helloagents/main/wiki/api.md`，补充 Playback Check-ins 三个官方接口、请求字段和触发时机。
- [√] 5.2 更新 `helloagents/main/wiki/data.md`，补充播放上报 DTO、状态协调器和 ticks 转换规则。
- [√] 5.3 更新 `helloagents/main/wiki/modules/player.md` 与必要的 `helloagents/main/wiki/modules/ui.md`，记录播放器播放/暂停/退出上报契约。
- [√] 5.4 更新 `helloagents/main/CHANGELOG.md`。

## 6. 验证
- [√] 6.1 GREEN: 运行 `.\gradlew.bat :app:testDebugUnitTest`，确认新增上报契约和 coordinator 测试通过。
- [√] 6.2 VERIFY: 运行 `.\gradlew.bat :app:assembleDebug`，确认 Debug 构建通过。
- [-] 6.3 TDD-EXEMPT: 真实 Emby 后台手工验收，原因: 当前环境未连接真实 TV/模拟器与 Emby 后台交互；替代验证: 安装 APK 后进入播放、暂停、恢复、退出，确认 Emby 后台设备会话和继续观看进度变化。

---

## 执行总结

- RED: 已新增 Repository 上报契约测试与 PlaybackReportingCoordinator 状态机测试，初次运行因缺少目标实现失败。
- GREEN: 已实现 Emby Playback Check-ins DTO/API/Repository 方法、ticks 转换、播放事件 coordinator 和 PlayerScreen 生命周期接入。
- VERIFY: `.\gradlew.bat :app:testDebugUnitTest` 与 `.\gradlew.bat :app:assembleDebug` 通过。
- 未执行: 真实 Emby 后台手工验收，原因是当前环境未连接真实 TV/模拟器与后台观察界面。
