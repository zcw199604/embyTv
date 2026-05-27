# 变更提案: Emby 播放状态上报

目录: `helloagents/main/plan/202605272133_emby_playback_reporting/`

---

## 背景

当前客户端已经能通过 Emby API 获取播放源、使用 Media3 播放视频，并在播放器 OSD 中控制播放、暂停和退出。但播放状态还没有回写到 Emby 服务器，因此 Emby 后台无法准确看到当前设备正在播放的条目、播放进度、暂停状态和停止播放事件，继续观看进度也无法稳定更新。

Emby 官方文档将这类交互称为 Playback Check-ins，需要客户端在开始播放、播放进度变化、暂停/恢复以及停止播放时调用对应接口。官方参考:
- [Playback Check-ins](https://dev.emby.media/doc/restapi/Playback-Check-ins.html)
- [POST /Sessions/Playing](https://dev.emby.media/reference/RestAPI/PlaystateService/postSessionsPlaying.html)
- [POST /Sessions/Playing/Progress](https://dev.emby.media/reference/RestAPI/PlaystateService/postSessionsPlayingProgress.html)
- [POST /Sessions/Playing/Stopped](https://dev.emby.media/reference/RestAPI/PlaystateService/postSessionsPlayingStopped.html)

## 目标

- 播放开始时通知 Emby，后台能识别当前设备开始播放的媒体。
- 播放中定期上报进度，Emby 能更新继续观看位置。
- 用户暂停/恢复播放时通知 Emby，后台能区分 pause/unpause 状态。
- 用户退出播放页、播放器释放或播放结束时通知 Emby 停止播放。
- 上报失败不影响本地播放、暂停、退出和弹幕体验。

## 范围内

- 扩展 Retrofit API，新增 `Sessions/Playing`、`Sessions/Playing/Progress`、`Sessions/Playing/Stopped`。
- 新增播放状态上报请求模型，按 Emby ticks 上报 `PositionTicks`。
- 在 `EmbyRepository` 或独立 service/use case 中封装播放上报。
- 在 `PlayerScreen` 生命周期中接入:
  - 首次播放源准备后发送 Playing。
  - 播放中按固定间隔发送 Progress。
  - OSD 播放/暂停切换时发送 Progress，并携带暂停状态。
  - Back 退出、Composable dispose、播放自然结束时发送 Stopped。
- 增加单元测试覆盖请求字段和事件节流策略。
- 更新知识库 API、数据模型和 player/ui 模块说明。

## 范围外

- 不实现 Emby 远程控制会话。
- 不实现 Emby 后台主动控制 TV 播放。
- 不实现转码参数协商、码率选择和音轨/字幕真实切换。
- 不实现播放历史列表或管理后台页面。
- 不改变当前 Media3 FFmpeg 扩展接入方式。

## 用户价值

- Emby 管理后台能看到 TV 客户端播放状态。
- 继续观看进度更准确，跨设备续播更可靠。
- 用户暂停或退出播放后，服务器状态不会长期停留在“正在播放”。
- 为后续“下一集”“播放历史”“多设备续播”打基础。

## 成功标准

- 进入播放页并开始播放后，客户端调用 `POST /Sessions/Playing`。
- 每隔约 10 秒或发生暂停/恢复/seek 后，客户端调用 `POST /Sessions/Playing/Progress`。
- 暂停时 Progress 请求包含暂停状态，恢复时清除暂停状态。
- 退出播放页或播放自然结束时，客户端调用 `POST /Sessions/Playing/Stopped`。
- 网络错误、服务器 4xx/5xx 不导致播放器崩溃，不阻塞 Back 退出。
- `.\gradlew.bat :app:testDebugUnitTest` 和 `.\gradlew.bat :app:assembleDebug` 通过。

## TDD 适用性

强制启用。该需求涉及新增外部 API 契约、播放状态业务逻辑和可观察行为变化，必须先补 Repository/service 与 reducer/adapter 层测试，再实现生产代码。

## 风险

- **重复上报:** Compose 重组或生命周期重复触发可能导致多次 Playing/Stopped，需要会话内幂等状态控制。
- **进度单位错误:** Media3 使用毫秒，Emby 使用 ticks，需要统一转换 `ms * 10_000`。
- **退出丢上报:** Back 后释放播放器过快，Stopped 需要在 `DisposableEffect` 或退出分支中尽力发送。
- **网络阻塞播放:** 上报必须异步 fire-and-forget 或在独立 coroutine 中执行，失败只记录内部状态，不影响播放链路。
- **敏感信息:** 日志和知识库不能输出 token、完整播放 URL 或真实私有媒体标题。
