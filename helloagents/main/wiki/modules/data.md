# data

## 目的
封装 Emby API、DTO、Repository、凭证存储、媒体详情聚合与播放地址构造。

## 模块概述
- **职责:** Retrofit 接口定义、Emby 登录、首页 Dashboard 聚合、收藏聚合、媒体详情/季/集读取、播放详情读取、播放地址构造。
- **状态:** 🚧开发中
- **最后更新:** 2026-05-28

## 规范

### 需求: Emby 基础接入
**模块:** data
Repository 对 UI 暴露 `Result`，避免 UI 层直接处理 Retrofit 异常。

#### 场景: 登录成功
Emby 返回用户 ID 和访问令牌后：
- 生成 `EmbySession`。
- 保存 `serverUrl`、`userId`、`username`、`accessToken`、`serverId`、`deviceId` 和保存时间。
- 使用 session 加载首页 Dashboard 聚合数据。

#### 场景: 首页 Dashboard
登录成功或恢复凭证后：
- 调用 `Users/{userId}/Views` 读取用户可见媒体库。
- 按媒体库调用 `Users/{userId}/Items?ParentId=...&Limit=0` 读取真实视频数量。
- 调用 `Users/{userId}/Items/Resume` 读取继续观看。
- 调用 `Users/{userId}/Items/Latest` 作为继续观看为空时的最近入库兜底。
- 按媒体库调用 `Users/{userId}/Items/Latest?ParentId=...&Limit=8` 读取首页横排最新资源；tvshows 使用 `IncludeItemTypes=Episode&GroupItems=true` 并兜底聚合为 Series。
- 不在首页首屏全量拉取全部 Movie/Episode。

#### 场景: 媒体库资源列表
用户进入媒体库后：
- 调用 `Users/{userId}/Items?ParentId=...&StartIndex=0&Limit=60` 获取首屏资源。
- movies 使用 `IncludeItemTypes=Movie`，tvshows 使用 `IncludeItemTypes=Series`，未知库使用 `Movie,Series`。
- 不做全库 Episode 扫描计算剩余集数，优先使用 Emby 返回的 `UserData.UnplayedItemCount`。

#### 场景: 收藏资源聚合
用户进入收藏页后：
- 调用 `Users/{userId}/Items?Filters=IsFavorite&IncludeItemTypes=Movie,Series,Episode&StartIndex=0&Limit=60` 获取收藏首屏。
- Movie 直接进入电影收藏分组。
- Series 直接进入电视剧收藏分组。
- Episode 按 `SeriesId` 或 `SeriesName` 聚合为 Series，避免同一剧集重复卡片。
- 聚合卡片保留图片 URL、剧集名字和 `UserData.UnplayedItemCount`；缺少名字时用条目 ID 兜底。

#### 场景: 媒体详情聚合
用户在 Movie 或 Series 卡片按 OK 后：
- 调用 `Users/{userId}/Items/{itemId}` 读取详情基础信息。
- 映射 `Overview`、`People`、`Genres`、`Studios`、`ProductionYear`、`CommunityRating`、`OfficialRating`、图片和播放进度字段。
- UI 层会将详情聚合结果整理为媒体信息卡片和演员卡片；Repository 不额外扩展详情接口，仍以 Emby 详情接口返回字段为准。
- Movie 详情不额外加载季列表。
- Series 详情继续调用 `Shows/{seriesId}/Seasons` 获取季列表，并将 `UserData.UnplayedItemCount > 0` 映射为季角标来源。
- 不在首页、媒体库列表或收藏页预加载详情，避免首屏请求量扩大。

#### 场景: 季内 Episode 按需加载
用户在 Series 详情页选择某一季后：
- 调用 `Shows/{seriesId}/Episodes?SeasonId=...` 获取该季 Episode。
- 映射 Episode 的 `ParentIndexNumber`、`IndexNumber`、`SeriesName`、缩略图和播放进度。
- Episode OK 后继续走既有 `Items/{itemId}/PlaybackInfo` 播放详情读取和播放状态上报。

#### 场景: 图片兜底
展示媒体库或媒体卡片时：
- 优先使用当前条目的 `ImageTags.Primary`、`PrimaryImageTag`、`ImageTags.Thumb` 和 `BackdropImageTags`。
- 当前条目缺图时使用 `ParentThumbItemId`、`ParentBackdropItemId`、`SeriesId` 与对应 image tag 构造父级图片 URL。
- 只有 item id 而没有 tag 时允许使用 Emby 无 tag 图片端点兜底；仍缺图则由 UI 展示占位。

#### 场景: 播放详情
选择媒体后：
- 调用 `Items/{itemId}/PlaybackInfo?UserId=...` 读取真实媒体源、Video/Audio/Subtitle streams。
- 生成 `PlaybackSource.details`，供播放器 OSD 展示容器、编码、画质、音轨和字幕状态。

#### 场景: 构造播放地址
选择媒体后：
- 生成 `Videos/{itemId}/stream?Static=true&api_key=...`。
- 对 itemId 和 token 进行 URL 编码。
- 不在日志或错误文案中输出完整播放 URL。

## API接口
见 [API 手册](../api.md)。

## 数据模型
见 [数据模型](../data.md)。

## 依赖
- core.network
- domain

## 变更历史
- [202605281300_media_detail_seasons](../../history/2026-05/202605281300_media_detail_seasons/) - 新增媒体详情、Series 季列表和季内 Episode 按需加载。
- [202605281045_favorite_resources_by_type](../../history/2026-05/202605281045_favorite_resources_by_type/) - 新增收藏资源查询和电影/电视剧聚合模型。
- [202605272217_library_browse_series_grouping](../../history/2026-05/202605272217_library_browse_series_grouping/) - 增加图片字段兜底、媒体库资源列表查询和 tvshows Series 聚合。
- [202605271602_emby_real_data_replacement](../../history/2026-05/202605271602_emby_real_data_replacement/) - 首页和播放器可见数据替换为 Emby 真实 API 数据。
- [202605271514_emby_server_mobile_sync](../../history/2026-05/202605271514_emby_server_mobile_sync/) - 保存 Emby token 凭证和用户名展示字段，不保存密码。
- [202605201342_emby_tv_init](../../history/2026-05/202605201342_emby_tv_init/) - 初始化 Emby API 与 Repository。
