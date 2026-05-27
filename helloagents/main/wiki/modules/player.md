# player

## 目的
封装 Media3 播放器创建与扩展渲染器配置。

## 模块概述
- **职责:** 创建 ExoPlayer，配置 OkHttp 数据源，启用扩展渲染器优先策略。
- **状态:** 🚧开发中
- **最后更新:** 2026-05-20

## 规范

### 需求: Emby 播放状态上报
**模块:** player
播放器需要向 Emby 后台同步开始播放、播放进度、暂停/恢复和停止播放状态。

#### 场景: 开始播放
当 `PlayerScreen` 设置媒体源并准备播放后：
- 调用 `POST Sessions/Playing`。
- 上报 `ItemId`、`MediaSourceId`、`PlaySessionId` 和 `PositionTicks`。
- 同一播放页生命周期内只发送一次开始事件。

#### 场景: 进度、暂停与 seek
播放过程中：
- 默认每 10 秒调用 `POST Sessions/Playing/Progress`。
- 播放/暂停按钮切换、生命周期暂停、快退和快进后立即强制上报。
- Media3 毫秒位置统一转换为 Emby ticks。

#### 场景: 停止播放
当播放自然结束、退出播放页或播放器释放时：
- 调用 `POST Sessions/Playing/Stopped`。
- 同一播放页生命周期内最多发送一次停止事件。
- 上报失败不阻塞 Back 退出和播放器释放。

### 需求: Media3 播放初始化
**模块:** player
播放器由 `Media3PlayerFactory` 统一创建，不在 UI 中直接拼装底层依赖。

#### 场景: FFmpeg 扩展可用
当 `app/libs` 中存在 Media3 FFmpeg 扩展 AAR 且类可被反射加载时：
- Media3 优先使用扩展渲染器。

#### 场景: FFmpeg 扩展不可用
当本地 AAR 不存在时：
- 播放器仍使用 Media3 默认渲染器工作。

## API接口
- `POST Sessions/Playing`
- `POST Sessions/Playing/Progress`
- `POST Sessions/Playing/Stopped`

## 数据模型
使用 `PlaybackSource.streamUrl`、`PlaybackSource.session`、`PlaybackSource.deviceId`、`PlaybackReportingCoordinator` 和 Emby Playback Check-ins DTO。

## 依赖
- core.network
- Media3

## 变更历史
- [202605272133_emby_playback_reporting](../../history/2026-05/202605272133_emby_playback_reporting/) - 接入 Emby Playback Check-ins，播放、暂停、进度和退出时同步后台状态。
- [202605201342_emby_tv_init](../../history/2026-05/202605201342_emby_tv_init/) - 初始化 Media3 播放工厂。
