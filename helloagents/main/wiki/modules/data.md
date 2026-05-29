# data

## 目的
封装 Emby API、DTO、Repository、凭证存储、媒体详情聚合与播放地址构造。

## 模块概述
- **职责:** Retrofit 接口定义、Emby 登录、首页 Dashboard 聚合、搜索、发现页、收藏聚合、用户态写操作、媒体详情/季/集读取、播放详情读取、播放队列和播放地址构造。
- **状态:** 🚧开发中
- **最后更新:** 2026-05-29

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
- 媒体库数量统计、继续观看、最近入库和按库 latest 使用 Coroutines 受控并发加载，并发上限为 4，避免媒体库数量较多时完全串行阻塞首页。
- 不在首页首屏全量拉取全部 Movie/Episode。
- 小数量调用 `Shows/NextUp` 获取下一集候选；空结果不影响首页。

#### 场景: Emby API 复用
Repository 请求 Emby 时：
- `EmbyApiFactory` 按 normalized `baseUrl + accessToken` 缓存 Retrofit service。
- 不同服务器或不同 token 必须使用不同 service，避免旧 token 泄漏到新会话。

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

#### 场景: 搜索资源
用户输入搜索关键词后：
- 空关键词不请求 Emby。
- 调用 `Users/{userId}/Items?SearchTerm=...`，搜索 Movie、Series、Episode、BoxSet 和 Playlist。
- 搜索结果以 `Items` 为准，不仅依赖 `TotalRecordCount`。
- UI 层搜索 debounce 和网络请求处于同一个可取消 Job；旧关键词返回时会按当前 query 校验，不能覆盖新关键词状态。
- 搜索成功后，ViewModel 将规范化后的关键词和结果数量写入 `SearchHistoryStore`。

#### 场景: 搜索历史本地存储
用户完成搜索后：
- `SearchHistoryStore` 使用 DataStore Preferences 持久化搜索历史 JSON。
- `SearchHistoryItem` 记录 `query`、`timestamp` 和 `resultCount`。
- 空关键词不会写入历史；相同关键词去重并保留最新一次搜索。
- 历史记录最多保存 20 条，按最新搜索倒序提供给 UI。
- 支持按 query 删除单条历史，或清空全部历史。

#### 场景: 播放历史本地存储
播放器产生最近播放记录时：
- `PlaybackHistoryStore` 使用 DataStore Preferences 持久化播放历史 JSON。
- `PlaybackHistoryItem` 记录媒体 ID、标题、播放位置、时长、时间戳和缩略图 URL。
- 相同媒体按 `mediaId` 去重并保留最新记录。
- 播放历史最多保存 50 条，按最近播放倒序提供。

#### 场景: 显示偏好本地存储
用户调整主题或辅助设置后：
- `ThemePreferenceStore` 使用 DataStore Preferences 保存主题 ID、高对比度开关和字体大小档位。
- 非法主题 ID 或字体档位读取时回退到 Cinematic Glass 和 Normal。
- `MainActivity` 收集偏好并传入 `EmbyTvTheme`，设置页写入后可即时生效。

#### 场景: 发现页
用户进入合集、播放列表、类型或演员页后：
- 合集调用 `IncludeItemTypes=BoxSet`，详情用 `ParentId={boxSetId}`。
- 播放列表调用 `IncludeItemTypes=Playlist`，详情用 `Playlists/{playlistId}/Items`。
- 类型调用 `Genres`，详情用 `GenreIds={genreId}`。
- 演员调用 `Persons`，详情用 `PersonIds={personId}`。
- 所有入口字段按可空处理，图片复用既有 Emby 图片 URL 构造。

#### 场景: 用户态写操作
用户在详情页切换收藏、已播放或清除进度后：
- Repository 调用 Emby Playstate/UserData 写接口。
- 写接口只使用当前 session 的 `userId` 和 token。
- 成功后由 ViewModel 刷新详情或 Dashboard；失败时 UI 保持原状态并提示。

#### 场景: 多服务器/多用户凭证
登录成功后：
- 新凭证按 `serverUrl + userId` 去重写入加密凭证列表。
- 读取凭证时兼容旧版单条字段，并迁移为列表 JSON。
- `load()` 仍返回最近保存的一条凭证，保持旧启动链路兼容；`loadAll()` 提供多账号选择数据。

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
- 图片 URL 按用途追加 `MaxWidth`、`MaxHeight` 和 `Quality`，首页 poster/thumb/backdrop 使用较小尺寸，详情图可使用更高尺寸，降低 TV 端网络与解码压力。
- 图片认证不拼接 token 到 URL。Repository 提供当前 session 的 `X-Emby-Authorization`，UI 图片组件通过 Coil `ImageRequest.httpHeaders` 注入认证头。

#### 场景: 播放详情
选择媒体后：
- 调用 `Items/{itemId}/PlaybackInfo?UserId=...` 读取真实媒体源、Video/Audio/Subtitle streams。
- 生成 `PlaybackSource.details`，供播放器 OSD 展示容器、编码、画质、音轨和字幕状态。
- 播放 Episode 时优先携带当前季/搜索/发现页队列；缺失时按 `seriesId + parentId` 按需拉取同季 Episode 补队列。

#### 场景: 构造播放地址
选择媒体后：
- 生成 `Videos/{itemId}/stream?Static=true&api_key=...`。
- 对 itemId 和 token 进行 URL 编码。
- 不在日志或错误文案中输出完整播放 URL。

#### 场景: 客户端版本标识
构造 `X-Emby-Authorization` 时：
- `Version` 使用 Gradle `versionName` 生成的 `BuildConfig.VERSION_NAME`。
- 测试可注入 `clientVersion` 验证请求头，不在生产代码中散落硬编码版本号。

## API接口
见 [API 手册](../api.md)。

## 数据模型
见 [数据模型](../data.md)。新增 `DiscoveryKind`、`DiscoveryEntrySummary`、`EmbyDiscoveryContent`、`DiscoveryEntryItems`、`EmbySearchResults`、`SearchHistoryItem`、`SearchHistoryStore`、`PlaybackHistoryItem`、`PlaybackHistoryStore`、`ThemePreferenceStore`、`PlaybackQueue`、`PlayerTrackOption` 和 `SavedEmbyCredentialList`。

## 依赖
- core.network
- domain
- AndroidX DataStore Preferences
- Kotlinx Serialization JSON

## 变更历史
- [202605291553_ui_interaction_optimization_phase3](../../history/2026-05/202605291553_ui_interaction_optimization_phase3/) - 新增主题偏好、播放历史 DataStore 持久化基础和相关规则测试。
- [202605291529_ui_interaction_optimization_phase2](../../history/2026-05/202605291529_ui_interaction_optimization_phase2/) - 新增搜索历史 DataStore 持久化、去重和数量上限规则。
- [202605291035_emby_tv_feature_completion](../../history/2026-05/202605291035_emby_tv_feature_completion/) - 新增搜索、发现页、用户态写操作、播放队列和多凭证列表。
- [202605291303_emby_review_issue_fixes](../../history/2026-05/202605291303_emby_review_issue_fixes/) - 修复认证图片请求、搜索取消、危险操作确认、播放上报和版本号来源。
- [202605281948_performance_optimization](../../history/2026-05/202605281948_performance_optimization/) - 优化首页 Dashboard 受控并发、Emby API service 复用和图片尺寸化。
- [202605281300_media_detail_seasons](../../history/2026-05/202605281300_media_detail_seasons/) - 新增媒体详情、Series 季列表和季内 Episode 按需加载。
- [202605281045_favorite_resources_by_type](../../history/2026-05/202605281045_favorite_resources_by_type/) - 新增收藏资源查询和电影/电视剧聚合模型。
- [202605272217_library_browse_series_grouping](../../history/2026-05/202605272217_library_browse_series_grouping/) - 增加图片字段兜底、媒体库资源列表查询和 tvshows Series 聚合。
- [202605271602_emby_real_data_replacement](../../history/2026-05/202605271602_emby_real_data_replacement/) - 首页和播放器可见数据替换为 Emby 真实 API 数据。
- [202605271514_emby_server_mobile_sync](../../history/2026-05/202605271514_emby_server_mobile_sync/) - 保存 Emby token 凭证和用户名展示字段，不保存密码。
- [202605201342_emby_tv_init](../../history/2026-05/202605201342_emby_tv_init/) - 初始化 Emby API 与 Repository。
