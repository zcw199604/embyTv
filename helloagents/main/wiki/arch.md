# 架构设计

## 总体架构
```mermaid
flowchart TD
    Activity["MainActivity"] --> App["EmbyTvApp"]
    App --> Home["HomeScreen + HomeViewModel"]
    App --> Player["PlayerScreen"]
    Home --> Repo["EmbyRepository"]
    Repo --> Api["Retrofit EmbyApi"]
    Repo --> Url["EmbyStreamUrlBuilder"]
    Player --> Media3["Media3PlayerFactory / ExoPlayer"]
    Player --> Danmaku["AkDanmakuBridge / DanmakuPlayer"]
    Core["DefaultAppContainer"] --> Repo
    Core --> Media3
    Core --> Danmaku
```

## 技术栈
- **客户端:** Android TV / Kotlin / Compose
- **网络:** Retrofit + OkHttp
- **播放:** AndroidX Media3 + 本地 FFmpeg 扩展预留
- **弹幕:** AkDanmaku
- **状态管理:** ViewModel + StateFlow

## 核心流程
```mermaid
sequenceDiagram
    participant User as 用户
    participant Home as HomeViewModel
    participant Repo as EmbyRepository
    participant Api as EmbyApi
    participant Player as PlayerScreen
    User->>Home: 输入服务器与账号
    Home->>Repo: authenticate
    Repo->>Api: Users/AuthenticateByName
    Api-->>Repo: AccessToken + UserId
    Home->>Repo: loadMediaItems
    Repo->>Api: Users/{userId}/Items
    User->>Home: 选择媒体
    Home->>Repo: createPlaybackSource
    Home-->>Player: PlaybackSource
    Player->>Player: Media3 播放 + AkDanmaku 覆盖
```

## 重大架构决策
完整的 ADR 存储在各变更的 how.md 中，本章节提供索引。

| adr_id | title | date | status | affected_modules | details |
|--------|-------|------|--------|------------------|---------|
| ADR-001 | 使用本地 AAR 预留 Media3 FFmpeg 扩展 | 2026-05-20 | ✅已采纳 | player, build | [how.md](../history/2026-05/202605201342_emby_tv_init/how.md#adr-001-使用本地-aar-预留-media3-ffmpeg-扩展) |
