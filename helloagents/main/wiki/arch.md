# 架构设计

## 总体架构
```mermaid
flowchart TD
    Activity["MainActivity"] --> App["EmbyTvApp"]
    App --> Home["HomeScreen + HomeViewModel"]
    App --> Player["PlayerScreen"]
    Home --> Repo["EmbyRepository"]
    Home --> Sync["MobileSetupSyncServer"]
    Sync --> Phone["手机浏览器配置页"]
    Home --> Store["EncryptedEmbyCredentialStore"]
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
- **SDK:** compileSdk 36 + compileSdkMinor 1，对应本机安装的 `android-36.1`
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
    User->>Home: 输入服务器配置与账号
    Home->>Repo: authenticate
    Repo->>Api: Users/AuthenticateByName
    Api-->>Repo: AccessToken + UserId
    Home->>Store: 保存 token + username 展示字段
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
| ADR-002 | 使用 TV 本机临时 HTTP 服务完成手机同步 | 2026-05-27 | ✅已采纳 | ui, core/network | [how.md](../history/2026-05/202605271514_emby_server_mobile_sync/how.md#adr-002-使用-tv-本机临时-http-服务完成手机同步) |
| ADR-003 | 保存用户名展示字段，不保存密码，只用 Emby 访问凭证恢复认证 | 2026-05-27 | ✅已采纳 | data, domain | [how.md](../history/2026-05/202605271514_emby_server_mobile_sync/how.md#adr-003-保存用户名展示字段不保存密码只用-emby-访问凭证恢复认证) |
| ADR-008 | 详情页采用按需加载而非列表预加载 | 2026-05-28 | ✅已采纳 | data, ui/home | [how.md](../history/2026-05/202605281300_media_detail_seasons/how.md#adr-008-详情页采用按需加载而非列表预加载) |
