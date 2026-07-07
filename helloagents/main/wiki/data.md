# 数据模型

## 概述
当前不引入本地数据库，数据以内存状态、加密 SharedPreferences 凭证存储和 DataStore Preferences 搜索历史传递。Emby 页面展示应优先来自真实 API，禁止继续使用假进度、硬编码媒体格式和样例播放数据作为正式页面内容。

---

## 领域模型

### ServerConfig
| 字段 | 类型 | 说明 |
|------|------|------|
| baseUrl | String | Emby 服务器地址 |
| username | String | 用户名 |
| password | String | 密码 |
| deviceId | String | 当前客户端设备 ID |

### EmbySession
| 字段 | 类型 | 说明 |
|------|------|------|
| serverUrl | String | 服务器地址 |
| userId | String | 用户 ID |
| accessToken | String | 访问令牌 |
| serverId | String? | 服务器 ID |

### MediaItemSummary
| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | Emby 条目 ID |
| name | String | 标题 |
| type | String | Movie、Episode 或 Series |
| overview | String? | 简介 |
| imageUrl | String? | 主图地址 |
| thumbImageUrl | String? | Thumb 缩略图地址 |
| backdropImageUrl | String? | Backdrop 背景图地址 |
| seriesId | String? | 剧集 ID，Episode 聚合为 Series 时使用 |
| seriesName | String? | 剧集所属剧名 |
| seasonName | String? | 季名称 |
| parentIndexNumber | Int? | 季序号 |
| indexNumber | Int? | 集序号 |
| parentId | String? | 父级条目或媒体库 ID |
| runTimeTicks | Long? | Emby ticks 总时长 |
| playbackPositionTicks | Long | 当前播放位置 ticks |
| playedPercentage | Double? | Emby 已播放百分比 |
| productionYear | Int? | 制作年份 |
| unplayedItemCount | Int? | 剩余未播放集数，Series 卡片角标来源 |
| childCount | Int? | 子条目数量 |
| recursiveItemCount | Int? | 递归子条目数量 |
| dateCreated | String? | 入库时间，用于最新资源排序兜底 |
| seekThumbnails | List<SeekThumbnail> | Emby `Chapters` 映射出的 seek 预览缩略图时间线 |

### SeekThumbnail
| 字段 | 类型 | 说明 |
|------|------|------|
| positionMs | Long | 章节起始时间，Emby `StartPositionTicks / 10000`；负数或缺失归零，超大 ticks 通过先除法换算避免溢出 |
| imageUrl | String | `/Items/{itemId}/Images/Chapter/{index}` 章节图片 URL；仅 `ImageTag` 非空白的章节进入 seek 时间线 |

### EmbyLibrarySummary
| 字段 | 类型 | 来源 | 说明 |
|------|------|------|------|
| id | String | `Views.Items[].Id` | 媒体库 ID |
| name | String | `Views.Items[].Name` | 媒体库名称 |
| collectionType | String? | `Views.Items[].CollectionType` | movies/tvshows 等 |
| type | String | `Views.Items[].Type` | CollectionFolder/Channel |
| itemCount | Int | `Users/{userId}/Items?ParentId=...` 的 `TotalRecordCount` | 视频数量 |
| imageUrl | String? | `ImageTags.Primary` 或 `PrimaryImageTag` | 媒体库封面；缺 tag 时允许使用无 tag 图片端点兜底 |

### EmbyLibraryLatestSection
| 字段 | 类型 | 来源 | 说明 |
|------|------|------|------|
| library | EmbyLibrarySummary | `Users/{userId}/Views` | 当前分区所属媒体库 |
| items | List<MediaItemSummary> | `Users/{userId}/Items/Latest?ParentId=...&Limit=8` | 媒体库最新资源；tvshows 聚合为 Series |

### EmbyLibraryContent
| 字段 | 类型 | 来源 | 说明 |
|------|------|------|------|
| library | EmbyLibrarySummary | `Users/{userId}/Views` | 当前媒体库 |
| items | List<MediaItemSummary> | `Users/{userId}/Items?ParentId=...&StartIndex=0&Limit=60` | 媒体库首屏资源列表；movies 为 Movie，tvshows 为 Series |

### EmbyFavoriteDashboard
| 字段 | 类型 | 来源 | 说明 |
|------|------|------|------|
| movies | List<MediaItemSummary> | `Users/{userId}/Items?Filters=IsFavorite&IncludeItemTypes=Movie,Series,Episode` | 收藏电影列表 |
| series | List<MediaItemSummary> | 同上 | 收藏电视剧列表；Series 直接保留，Episode 按 `SeriesId/SeriesName` 聚合 |
| totalCount | Int | 同上 | 本次收藏查询返回条目数 |

收藏展示规则:
- 每个收藏卡片必须有图片区域和资源名字；缺图片时 UI 显示占位。
- 资源名字优先使用 `name`，电视剧聚合优先使用 `seriesName`，仍为空时用条目 ID 兜底。
- 电视剧卡片可使用 `unplayedItemCount` 显示“剩 n 集”角标。

### EmbyPersonSummary
| 字段 | 类型 | 来源 | 说明 |
|------|------|------|------|
| id | String? | `People[].Id` | 人物 ID |
| name | String | `People[].Name` | 人物名称 |
| role | String? | `People[].Role` | 角色名 |
| type | String? | `People[].Type` | Actor、Director 等 |

### EmbySeasonSummary
| 字段 | 类型 | 来源 | 说明 |
|------|------|------|------|
| id | String | `Shows/{seriesId}/Seasons.Items[].Id` | Season 条目 ID |
| name | String | `Name` | 季名称 |
| indexNumber | Int? | `IndexNumber` | 季序号 |
| imageUrl | String? | `ImageTags.Primary` 或 `PrimaryImageTag` | 季封面 |
| episodeCount | Int? | `ChildCount` | 本季集数 |
| unplayedItemCount | Int? | `UserData.UnplayedItemCount` | 剩余未播放集数；小于等于 0 时置空 |

### EmbyMediaDetail
| 字段 | 类型 | 来源 | 说明 |
|------|------|------|------|
| item | MediaItemSummary | `Users/{userId}/Items/{itemId}` | Movie 或 Series 基础条目 |
| people | List<EmbyPersonSummary> | `People` | 演员等人物信息 |
| genres | List<String> | `Genres` | 类型 |
| studios | List<String> | `Studios[].Name` | 制片方 |
| communityRating | Double? | `CommunityRating` | 社区评分 |
| officialRating | String? | `OfficialRating` | 分级 |
| premiereDate | String? | `PremiereDate` | 首映日期 |
| criticRating | Double? | `CriticRating` | 影评评分 |
| providerIds | Map<String, String> | `ProviderIds` | IMDb、Douban 等外部来源 ID，仅保留非空键值 |
| seasons | List<EmbySeasonSummary> | `Shows/{seriesId}/Seasons` | Series 详情页季列表；Movie 为空 |

### EmbySeasonEpisodes
| 字段 | 类型 | 来源 | 说明 |
|------|------|------|------|
| season | EmbySeasonSummary | 用户选择的季 | 当前季上下文 |
| episodes | List<MediaItemSummary> | `Shows/{seriesId}/Episodes?SeasonId=...` | 季内 Episode 列表 |

### EmbyHomeDashboard
| 字段 | 类型 | 来源 | 说明 |
|------|------|------|------|
| libraries | List<EmbyLibrarySummary> | `Users/{userId}/Views` + 按库统计 | 首页媒体库卡片 |
| resumeItems | List<MediaItemSummary> | `Users/{userId}/Items/Resume` | 继续观看 |
| latestItems | List<MediaItemSummary> | `Users/{userId}/Items/Latest` | 最近入库兜底 |
| libraryLatestSections | List<EmbyLibraryLatestSection> | 按每个媒体库 ParentId 查询最新资源 | 首页按媒体库展示最新内容 |

### SearchHistoryItem
| 字段 | 类型 | 来源 | 说明 |
|------|------|------|------|
| query | String | 用户搜索关键词 | 历史记录展示和点击复搜的关键词 |
| timestamp | Long | 本机时间戳 | 最近搜索排序依据 |
| resultCount | Int | 搜索返回条目数量 | 历史记录辅助信息 |

搜索历史存储规则:
- `SearchHistoryStore` 使用 DataStore Preferences 保存 JSON 字符串。
- 空关键词不保存。
- 相同关键词去重并保留最新记录。
- 最多保留 20 条历史记录。

### PlaybackDetails
| 字段 | 类型 | 来源 | 说明 |
|------|------|------|------|
| playSessionId | String? | `PlaybackInfo.PlaySessionId` | 播放会话 |
| mediaSourceId | String? | `MediaSources[].Id` | 媒体源 |
| container | String? | `MediaSources[].Container` | 容器格式 |
| bitrate | Int? | `MediaSources[].Bitrate` | 总码率 |
| video | PlaybackVideoStream? | Video Stream | 视频流概要 |
| audioTracks | List<PlaybackTrack> | Audio Streams | 音轨 |
| subtitleTracks | List<PlaybackTrack> | Subtitle Streams | 字幕 |
| bitrateLabel | String? | 派生字段 | Mbps 码率标签，使用固定英文小数点技术格式 |
| playbackSummaryLabel | String | 派生字段 | Direct Play + 容器 + 视频编码 |
| qualityLabel | String | 派生字段 | 分辨率 + HDR/SDR 信息 + 码率 |
| audioLabel | String | 派生字段 | 默认音轨或首个音轨标题 |
| subtitleLabel | String | 派生字段 | 默认字幕或首个字幕标题 |

`PlaybackDetails` 的派生标签保留 domain 层默认值；`bitrateLabel` 和 `qualityLabel` 中的码率始终使用 `Locale.US` 技术格式，例如系统 Locale 为法语时仍输出 `4.0 Mbps`。容器、视频编码、音频编码和外挂字幕未知格式等技术大小写同样显式使用 `Locale.US`，避免系统 Locale 改变 OSD/Overlay 技术标签。播放器 OSD 展示时通过 `PlayerPlaybackDetailsLabelResolver` 重新生成 `summary/quality/audio/subtitles`，并由 UI 层传入本地化的播放模式、未知画质、无音轨、无字幕 fallback。

### PlaybackVideoStream
| 字段 | 类型 | 说明 |
|------|------|------|
| codec | String? | 视频编码 |
| width | Int? | 宽度 |
| height | Int? | 高度 |
| videoRange | String? | SDR/HDR 信息 |

### PlaybackTrack
| 字段 | 类型 | 说明 |
|------|------|------|
| index | Int | Emby MediaStream Index |
| codec | String? | 编码 |
| displayTitle | String? | 展示标题；进入 `PlaybackTrack.label` 前会 trim 首尾空白 |
| channels | Int? | 声道数 |
| language | String? | 语言 |
| isDefault | Boolean | 是否默认 |
| isForced | Boolean | 是否强制字幕 |
| isExternal | Boolean | 是否外挂字幕 |
| deliveryMethod | String? | Emby 字幕交付方式 |
| externalUrl | String? | 外挂字幕交付 URL，来自 `MediaStream.DeliveryUrl` 并规范化为可带 `api_key` 的绝对 URL；已带 token 的绝对 URL 按 query 参数名精确识别，大小写不敏感并保持原样；进入 Media3 注入前会 trim 首尾空白 |

`PlaybackTrack.label` 会将 Emby 原始语言码归一化后生成友好标签；`displayTitle` 会先 trim，空白标题按缺失处理，避免 OSD 顶部和快捷胶囊展示带首尾空格的轨道名。`zh_Hans`、`zh_CN`、`en_US` 等下划线变体与 `zh-Hans`、`zh-CN`、`en-US` 等连字符变体一样展示为 `Chinese (Simplified)` 或 `English`，避免 OSD 顶部和轨道面板暴露底层语言码。外挂字幕还会将 `subrip` / `webvtt` 等 Emby codec 折叠为 `SRT` / `VTT`，未知字幕格式使用 `Locale.US` 技术大写，保持与 Media3 字幕 MIME 映射和 OSD 展示一致。若外部字幕 `codec` 缺失或为空，标签会回退解析 `externalUrl` 路径后缀生成 `SRT` / `VTT` / `ASS` 格式名。

### PlaybackOverlayDetails
| 字段 | 类型 | 说明 |
|------|------|------|
| mediaDetail | EmbyMediaDetail | 并行加载得到的完整媒体详情 |
| playbackDetails | PlaybackDetails | 并行加载得到的 PlaybackInfo 播放详情 |

`EmbyRepository.loadPlaybackOverlayDetails` 使用协程并行请求 `loadMediaDetail` 与 `getPlaybackInfo`；回归测试通过阻塞 metadata 响应验证 PlaybackInfo 请求会先启动，避免播放页暂停或 OSD 呼出时被串行网络等待拖慢。

`PlaybackOverlayDetails.playbackDetails` 在 UI 层加载成功后会进入 `PlayerDetailOverlayState.playbackDetails`；`PlayerScreen` 计算顶部技术标签、轨道可用性和音轨/字幕快捷胶囊摘要时优先使用该值，缺失时再回退到初始 `PlaybackSource.details`。

播放页 Overlay 展示 `communityRating` 和 `criticRating` 时分别通过 `Double.toCommunityRatingLabel()` 与 `Double.toCriticRatingLabel()` 固定格式；社区/IMDb 类评分保留 1 位小数，影评评分显示整数，两者均使用 `Locale.US`，避免系统 Locale 改变评分小数点。`officialRating` 通过 `String?.toOfficialRatingLabel(format)` 展示，format 来自 `player_official_rating_label` 中英文资源；空白分级跳过，非空分级 trim 后填入本地化前缀，例如“分级 PG-13”或 `Rated PG-13`。

`PlayerDetailProviderIdsLabelResolver` 从 `EmbyMediaDetail.providerIds` 生成 Overlay 外部来源标识，只展示非空 IMDb 与 Douban 值，忽略 provider key 大小写并固定输出顺序为 IMDb、Douban。该规则保证 Emby 返回 `IMDB`、`Imdb`、`douban` 等不同 key 形态时，播放器详情 Overlay 仍有稳定展示。

`PlayerDetailCastLabelResolver` 从 `EmbyMediaDetail.people` 生成 Overlay 演员摘要，只选择 `type=Actor` 的人物，最多取 4 个演员；若没有 Actor 则不显示演员行，避免导演、编剧或其他人员被误标为演员。角色名连接格式由 UI 层注入，中文资源为 `姓名 饰 角色`，英文资源为 `Name as Role`，resolver 本身不硬编码具体语言文案。

### PlayerDetailOverlayLoadSnapshot
| 字段 | 类型 | 说明 |
|------|------|------|
| currentItemId | String | 当前播放源的 Emby 条目 ID |
| overlayItemId | String? | 现有 Overlay 状态归属的 Emby 条目 ID |
| shouldDisplayOverlay | Boolean | 当前 OSD 可见、播放器处于暂停态或播放自然结束，详情 Overlay 需要展示 |
| sessionAvailable | Boolean | `PlaybackSource.session` 和 `deviceId` 是否可用 |
| isLoading | Boolean | Overlay 详情是否正在加载 |
| hasDetail | Boolean | 是否已有完整媒体详情 |
| hasError | Boolean | 是否已有本播放项的 Overlay 加载失败信息 |

`PlayerDetailOverlayLoadPolicy` 使用该快照决定是否发起 `loadPlaybackOverlayDetails`，避免 OSD 可见、暂停或结束态期间播放状态细跳导致重复请求或取消重启；同一播放项失败后不自动重复请求，但当 `overlayItemId != currentItemId` 时旧播放项的 loading、loaded 或 error 不会阻止新媒体重新进入加载门禁。

### PlayerDetailOverlayState
| 字段 | 类型 | 说明 |
|------|------|------|
| itemId | String? | 当前 Overlay loading、loaded 或 failed 状态归属的 Emby 条目 ID |
| isLoading | Boolean | Overlay 详情是否正在加载 |
| detail | EmbyMediaDetail? | 已加载的完整媒体详情 |
| playbackDetails | PlaybackDetails? | Overlay 并行请求返回并保留的最新 PlaybackInfo 播放详情 |
| errorMessage | String? | 当前播放项的 Overlay 加载失败文案 |

`PlayerOsdReducer` 在 `DetailOverlayLoading`、`DetailOverlayLoaded` 和 `DetailOverlayFailed` 时都会写入对应 `itemId`；`PlayerDetailOverlayLoadPolicy` 只把同一 `itemId` 的 loading、loaded 或 error 视为重复请求门禁，旧播放项的状态不得阻止新媒体加载 Overlay 详情。若当前 Overlay 已归属其它 `itemId`，`DetailOverlayLoaded` 和 `DetailOverlayFailed` 的晚到结果会被忽略；正常成功时写入 `detail` 与 `playbackDetails`，失败时只写入错误状态并同步 `feedbackMessage`，不会覆盖播放控制。

### DanmakuOverlaySettings
| 字段 | 类型 | 说明 |
|------|------|------|
| opacity | Float | 弹幕视图透明度，归一化范围 0.2..1.0 |
| textSizeScale | Float | AkDanmaku 字号缩放，归一化范围 0.8..1.6 |
| displayArea | DanmakuDisplayArea | 弹幕显示区域，支持顶部或全屏 |

`DanmakuOverlaySettings.normalized()` 会先处理非有限数：`opacity` 为 `NaN` 或无穷时回退到 1.0，`textSizeScale` 为 `NaN` 或无穷时回退到 1.15，然后再执行范围限制，避免 Compose alpha 和 AkDanmaku `DanmakuConfig` 接收非法浮点值。

### DanmakuPlaybackConfigKey
| 字段 | 类型 | 说明 |
|------|------|------|
| textSizeScale | Float | 归一化后的 AkDanmaku 字号缩放 |
| displayArea | DanmakuDisplayArea | 弹幕显示区域 |

`DanmakuOverlay` 使用该 key 控制 `LaunchedEffect`，因此单独调整 `opacity` 只更新 Compose alpha，不重新 `updateConfig/start` akdanmaku；字号或显示区域变化才更新底层弹幕配置。

### DanmakuQuickPanelLayout
| 字段 | 类型 | 说明 |
|------|------|------|
| rows | List<List<DanmakuQuickOption>> | 弹幕快捷设置的 TV OSD 分行顺序 |

`DanmakuQuickPanelLayoutPolicy.TvDefault` 将 9 个弹幕快捷项分为开关、透明度、字号和显示区域 4 行，单行最多 3 个按钮；`PlayerScreen` 按该策略渲染弹幕设置面板，避免 1080p TV 上 9 个按钮挤在同一行。

### PlayerQuickPanelLayoutSpec
| 字段 | 类型 | 说明 |
|------|------|------|
| maxItemsPerRow | Int | OSD 快捷面板每行最大按钮数，`PlayerQuickPanelLayoutPolicy.TvDefault` 为 3 |

`PlayerQuickPanelLayoutPolicy.TvDefault.rowsFor(items)` 用于倍速、音轨和字幕快捷面板；它按原始顺序切分短行，让 6 个倍速按钮显示为两行，让完整音轨/字幕列表按每行最多 3 个按钮排列，降低长语言标签在 TV OSD 上横向挤压的风险。播放页轨道按钮区带最大高度与滚动容器，避免多语言媒体返回大量轨道时挤压进度条和底部控制。

### DanmakuPlaybackCommand
| 命令 | 字段 | 说明 |
|------|------|------|
| Start | settings, config | 弹幕启用且未暂停时应用归一化设置和 `DanmakuConfig` |
| Pause | 无 | 弹幕关闭、播放器暂停、加载或缓冲时暂停渲染 |

### DanmakuSyncCommand
| 命令 | 字段 | 说明 |
|------|------|------|
| ClearAndSeek | positionMs | Seek 或 Media3 位置跳变时清理旧弹幕帧并跳转到非负毫秒位置 |

`DanmakuPlaybackPolicy` 从 `DanmakuOverlaySettings`、启用状态、暂停状态和目标毫秒位置派生命令，`PlayerScreen` 与 `DanmakuOverlay` 只执行命令，不直接重复判断播放/暂停/seek 分支。`PlayerScreen.syncDanmakuTo` 消费 `ClearAndSeek` 时必须先 `stop()` 清理旧帧，再 `seekTo(positionMs)`，最后按当前 OSD 弹幕状态恢复 `Start` 或 `Pause`；即时遥控器 seek 和 `PlayerPlaybackEffect.SyncDanmaku` 都走同一同步路径。

### PlaybackSource
| 字段 | 类型 | 说明 |
|------|------|------|
| itemId | String | Emby 条目 ID |
| title | String | 标题 |
| streamUrl | String | Media3 播放地址 |
| playlistItemId | String? | 播放列表条目 ID，来自 `MediaItemSummary.playlistItemId`，用于 Emby Playback Check-ins 关联播放列表项 |
| session | EmbySession? | 播放状态上报所需认证上下文 |
| deviceId | String? | 播放状态上报所需设备 ID |
| details | PlaybackDetails | 从 `PlaybackInfo` 生成的播放详情 |
| queue | PlaybackQueue? | 播放上下文队列，用于上一集、下一集和自然结束自动播放 |
| danmaku | List<DanmakuCue> | 弹幕列表 |
| previewThumbnailUrl | String? | seek 预览缩略图兜底 URL；空白值视为不可用 |
| seekThumbnails | List<SeekThumbnail> | API 提供章节图时的 seek 预览时间线 |
| startPositionMs | Long | 从 Emby `UserData.PlaybackPositionTicks` 转换得到的续播起点毫秒值，播放器启动、弹幕 seek 和 `Sessions/Playing` 开始上报共用该值 |
| contextLabel | String? | 播放上下文，如剧集 `S01E01` |

播放器 OSD 已使用 `PlaybackSource.details` 展示真实容器、编码、分辨率、HDR、码率、音轨和字幕状态；音轨/字幕切换通过 Media3 `TrackSelectionParameters` 执行。`PlayerMediaItemFactory.create(source)` 会把 `itemId` 写入 Media3 `MediaItem.mediaId`，并把 `title` 写入 `MediaMetadata.title`，让 Media3 回调、日志和后续会话集成保留稳定 Emby 身份。`startPositionMs` 在仓库层由 ticks 转为毫秒并在播放页归一化为非负值，避免续播时 Media3、AkDanmaku 和 Emby Playback Check-ins 起点不一致；Repository 回归测试覆盖负数 ticks 归零和 `Long.MAX_VALUE` ticks 安全换算。`previewThumbnailFor(positionMs)` 只返回 trim 后的非空白章节图或非空白兜底缩略图，避免 seek 预览向 Compose 图片组件传递空白 URL。`PlayerManager.requestSeekPreview()` 会 trim thumbnail provider 返回值，空白值不覆盖上一张有效 `SeekPreviewState.thumbnailUrl`。`playlistItemId` 在播放列表来源媒体上保留到播放源，并随 Started/Progress/Stopped 一起上报，避免 Emby 后台无法把播放事件关联回 playlist item。

### PlaybackQueue
| 字段 | 类型 | 说明 |
|------|------|------|
| previous | MediaItemSummary? | 上一集/上一项 |
| current | MediaItemSummary | 当前播放项 |
| next | MediaItemSummary? | 下一集/下一项，可由同季 Episode 队列或 `Shows/NextUp` 兜底生成 |
| autoPlayNext | Boolean | 自然播放结束后是否自动切换到下一项 |

`EmbyRepository.createPlaybackSourceWithDetails` 会优先使用调用方提供的显式队列；缺失时对 Episode 通过 Emby `Shows/{seriesId}/Episodes` 构造同季队列，季末没有 next 时再用 `Shows/NextUp` 补下一集。

### PlayerQueueNavigationState
| 字段 | 类型 | 说明 |
|------|------|------|
| previous | PlayerQueueNavigationItemState | 上一集按钮的可用性和禁用提示 |
| next | PlayerQueueNavigationItemState | 下一集按钮的可用性和禁用提示 |
| autoPlayNextTarget | MediaItemSummary? | 播放自然结束时允许自动切换的下一集目标；仅在 `PlaybackQueue.autoPlayNext=true`、存在 next 且 next 不是当前媒体时非空 |

### PlayerQueueNavigationItemState
| 字段 | 类型 | 说明 |
|------|------|------|
| enabled | Boolean | 当前方向是否存在可播放队列目标 |
| disabledReason | String? | 无目标时展示给 OSD 的本地化提示 |

`PlayerQueueNavigationPolicy.resolve(queue, noPreviousReason, noNextReason)` 从 `PlaybackQueue` 派生该状态；上一集/下一集没有目标时按钮禁用但保留可聚焦反馈，按 OK 只触发禁用原因提示。previous/next 若与 `current.id` 相同会被视为无效目标，防止异常队列或 NextUp 把播放器切回当前条目。手动下一集按钮只依赖有效 `next` 是否存在，自动下一集则额外要求 `autoPlayNext=true`，避免用户关闭自动连播后仍被自然结束自动切集。

### PlayerExternalSubtitle
| 字段 | 类型 | 说明 |
|------|------|------|
| url | String | 可交给 Media3 的外挂字幕绝对 URL |
| mimeType | String | Media3 字幕 MIME，目前支持 SubRip、WebVTT、ASS/SSA |
| language | String? | 传给 Media3 的规范化字幕语言标签，如 `zh-CN`、`zh-TW`、`en` |
| label | String | OSD/轨道展示标签 |
| selectionFlags | Int | Media3 默认/强制字幕选择标记 |

`PlayerMediaItemFactory.externalSubtitlesFor(source)` 从 `PlaybackSource.details.subtitleTracks` 派生该模型；`create(source)` 再转换为 `MediaItem.SubtitleConfiguration`，同时保留 `source.itemId` 到 `MediaItem.mediaId`、`source.title` 到 `MediaMetadata.title`，保证 `PlayerScreen` 不直接处理字幕 MIME、语言码、选择标记和 Media3 身份字段。`url` 来自 trim 后的 `externalUrl`，空白 URL 会被忽略，避免 `SubtitleConfiguration` 或后缀 MIME 推断携带首尾空白。语言标签规范化显式使用 `Locale.US`，例如系统 Locale 为土耳其语时 `az_ir` 仍输出 `az-IR`。展示标签仍来自 `PlaybackTrack.label`，可保留 `Chinese (Simplified) · SRT · External` 等友好文案。MIME 优先来自 `PlaybackTrack.codec`；若 codec 缺失或为空，则从 trim 后 `externalUrl` 路径后缀推断 SRT/VTT/ASS/SSA，避免真实外部字幕源只给文件 URL 时被过滤。

### PlayerTrackOptionMapper
| 输入 | 输出 | 说明 |
|------|------|------|
| Media3 `Tracks` + `C.TRACK_TYPE_AUDIO` | List<PlayerTrackOption> | 将 Media3 判定为支持的音轨转换为 OSD 菜单项；优先使用 trim 后 `Format.label`，缺失时组合友好语言名、音频编码和声道标签，例如 `English AAC 5.1` |
| Media3 `Tracks` + `C.TRACK_TYPE_TEXT` | List<PlayerTrackOption> | 将 Media3 判定为支持的字幕转换为 OSD 菜单项；优先使用 trim 后 `Format.label`，缺失时组合友好语言名和字幕格式，例如 `Chinese (Simplified) SRT` |

该 mapper 过滤 `group.isTrackSupported(trackIndex)=false` 的单条轨道，并保留 Media3 `TrackGroup`、`trackIndex` 与 selected 状态，供 `PlayerTrackSelections` 写入 `TrackSelectionOverride`。Media3 原始 `Format.label` 会先 trim，空白标签按缺失处理，避免轨道菜单展示带首尾空格的按钮文本。语言标签会折叠常见地区和脚本变体，例如 `en_US` / `en-US` 显示为 `English`，`zh_Hans` / `zh-Hans` 显示为 `Chinese (Simplified)`；编码与 MIME 兜底标签大小写显式使用 `Locale.US`。

### PlayerTrackSummaryLabelResolver
| 输入 | 输出 | 说明 |
|------|------|------|
| PlayerOsdState + PlaybackDetails | PlayerTrackSummaryLabels | 生成 OSD 音轨/字幕快捷胶囊摘要 |

`PlayerTrackSummaryLabelResolver` 优先读取 `PlayerOsdState.audioTracks/subtitleTracks` 中当前 selected 的 Media3 轨道标签；没有 selected 时回退到 `PlaybackDetails` 默认/首个轨道；`subtitleDisabled=true` 时字幕摘要固定使用 UI 层传入的本地化无字幕标签，避免关闭字幕后继续显示 PlaybackInfo 默认字幕。

### PlayerOsdFocusSnapshot
| 字段 | 类型 | 说明 |
|------|------|------|
| playbackItemId | String | 当前播放条目 ID |
| visible | Boolean | OSD 是否可见 |
| interactionRevision | Int | OSD 交互修订号 |
| selectedQuickPanel | PlayerQuickPanel? | 当前打开的快捷面板；从非空变为空时需要恢复主播放按钮焦点 |

`PlayerOsdFocusController` 使用该快照判断是否需要把焦点恢复到主播放按钮：首次显示、隐藏后重新显示、播放条目切换或快捷面板关闭时请求焦点；OSD 已显示时的普通交互不抢焦点。

### PlayerQuickPanelFocusSnapshot
| 字段 | 类型 | 说明 |
|------|------|------|
| visible | Boolean | OSD 是否可见 |
| selectedQuickPanel | PlayerQuickPanel? | 当前打开的快捷面板 |
| focusableOptionCount | Int | 当前面板内可请求焦点的选项数量 |

`PlayerQuickPanelFocusPolicy` 使用该快照判断是否需要把焦点移入快捷面板：OSD 可见、面板已打开且面板内有可聚焦选项时，如果从无面板/其他面板切换过来，或同一面板从 0 个选项变为有选项，则请求首个选项焦点；同一面板内已有选项时的普通交互不重复请求，避免遥控器用户刚移动到某个选项又被抢回第一项。

### PlayerRemoteKeyCommand
| 命令 | 字段 | 说明 |
|------|------|------|
| Ignore | 无 | 当前按键不由播放器根节点消费，交给焦点控件或系统处理 |
| Dispatch | action | 转发为 `PlayerOsdAction`，例如 Back 或 UserInteraction |
| SeekBy | deltaMs | 隐藏态方向键 seek 增量，目前左/右为 -10000 / +10000 毫秒 |

`PlayerRemoteKeyPolicy` 输入 `PlayerRemoteKeyEventType`、`PlayerRemoteKey` 和 OSD 可见性，输出上述命令。KeyDown 不触发动作；Back 始终转发给 OSD reducer；OSD 隐藏时中心键/Enter/上/下呼出 OSD，隐藏态左/右执行 seek；OSD 可见时方向键和确认键保留给当前焦点控件。`PlayerScreen` 的 `onPreviewKeyEvent` 只提前消费 `SeekBy` 与 Back，普通 `onKeyEvent` 再按当前 `osdState.visible` 调用该策略，避免预览层吞掉焦点控件的 OK/方向键。

### PlayerOsdAction 反馈文案约定
| Action | 反馈字段 | 说明 |
|--------|----------|------|
| BackPressed | 无 | OSD 可见且快捷面板打开时先清空 `selectedQuickPanel`、seek 预览和反馈并保留 OSD；OSD 可见但无面板时隐藏 OSD；OSD 已隐藏时请求退出播放器。 |
| SelectQuickPanel | panel | 快捷胶囊选择目标面板；若目标面板已是当前 `selectedQuickPanel`，则清空面板作为同入口关闭；否则切换到目标面板；同时清理反馈和 `seekPreview`。 |
| ProgressChanged | positionMs, durationMs, bufferedFraction | 播放进度刷新；位置和总时长归一为非负值，缓冲比例只允许 0..1，`NaN` 或正负无穷值归零，避免 OSD 进度条渲染非有限数。 |
| TracksChanged | audioTracks, subtitleTracks | Media3 轨道回调同步 OSD 轨道列表；当 `subtitleDisabled=true` 时必须清空传入字幕的本地 selected 并保留关闭意图，避免延迟回调恢复旧字幕。 |
| DisableSubtitles | feedbackMessage: String? | 用户关闭字幕时由 Compose `stringResource` 传入本地化提示；为空时 reducer 保留既有反馈，不硬编码中文。 |
| SelectPlaybackSpeed | feedbackMessage: String? | 用户切换倍速时由 UI 传入本地化倍速提示并呼出速度面板；Media3 底层倍速回调传空值时只同步 `playbackSpeed`，不显示 OSD、不打开速度面板、不覆盖既有反馈；`NaN` 或正负无穷倍速回退为 1.0x。 |

### PlayerPlaybackController
| 方法 | 说明 |
|------|------|
| onPlayerError(message, fallbackMessage) | 将 Media3 播放错误映射为 `PlaybackEngineStatus.Error`；`fallbackMessage` 必须由 UI 层使用 `stringResource` 提供，控制器不持有具体语言文案。 |
| onIsPlayingChanged(isPlaying, playbackState) | 必须结合 Media3 `playbackState` 解释 `isPlaying=false`；`STATE_BUFFERING` 保持 `Buffering`，`STATE_ENDED` 保持 `Ended`，避免后续回调把 OSD 覆盖为 `Paused`。 |
| onPositionDiscontinuity(newPositionMs, isPlaying, reason) | 所有 Media3 位置跳变都会生成 `SyncDanmaku`，保证弹幕时间轴跟随播放器；只有 `DISCONTINUITY_REASON_SEEK` 和 `DISCONTINUITY_REASON_SEEK_ADJUSTMENT` 会额外生成 `ReportSeek`，自动切集等非 seek 跳变不得触发 Emby seek Progress。 |

`PlayerScreen` 通过 `remember(playbackSource, startPositionMs)` 创建 `PlayerManager`，因此播放源任意字段变化都会重建 OSD 状态流；这用于清理旧媒体的 seek 预览、详情 Overlay、轨道本地选中态和瞬态反馈，同时保留续播起点作为初始进度。

### PlayerTrackSelections
| 方法 | 说明 |
|------|------|
| selectTrack(option) | 选择 Media3 音轨或字幕轨道；同类型只保留最新 override，音轨和字幕 override 跨类型互不清理；选择字幕时会重新启用 `C.TRACK_TYPE_TEXT`，选择音轨时即使字幕已关闭也不得重新启用文字轨道。 |
| disableSubtitles() | 禁用 `C.TRACK_TYPE_TEXT` 并清理 text track overrides，避免关闭字幕后旧字幕 override 在后续重新启用文字轨道时被意外恢复。 |

### SeekPreviewState
| 字段 | 类型 | 说明 |
|------|------|------|
| targetPositionMs | Long | 当前预览目标播放位置 |
| deltaMs | Long | 本次遥控器 seek 步长 |
| speedLabel | String | OSD 显示的累计 seek 偏移标签，如 `+20s` |
| thumbnailUrl | String? | 当前目标位置对应的章节图或兜底缩略图 |
| originPositionMs | Long | 本轮连续 seek 开始前的原始播放位置 |

连续方向键 seek 会保留 `originPositionMs`，因此第二次及后续预览标签显示从原始位置到当前目标位置的实际可达偏移量；如果总时长边界截断目标，标签也按截断后的实际偏移显示。目标位置计算使用饱和加法，避免未知总时长且当前位置异常接近 `Long.MAX_VALUE` 时继续右键 seek 发生溢出回绕；`PlayerManager` 传给缩略图 provider 的目标位置必须复用同一饱和目标，避免缩略图时间点和实际 seek 目标不一致；provider 返回值必须先 trim，空白字符串归一化为 null，本次没有有效缩略图时保留上一张有效 `thumbnailUrl`；累计标签使用饱和差值生成，避免异常 origin 超出归一化时间轴时 `target - origin` 溢出并翻转 seek 方向。

OSD 播放时间与剩余时间由 `Long.toClockLabel()` 生成，固定使用 `Locale.US` 和西文数字，例如 `01:05`、`1:01:05`；该标签属于播放器时间轴技术信息，不跟随系统 Locale 切换数字形态。

### PlayerOsdAutoHideSnapshot
| 字段 | 类型 | 说明 |
|------|------|------|
| visible | Boolean | OSD 当前是否可见 |
| status | PlaybackEngineStatus | 当前播放状态 |
| quickPanelOpen | Boolean | 音轨、字幕、倍速或弹幕快捷面板是否打开；为 true 时不调度 5 秒自动隐藏 |

`PlayerOsdAutoHidePolicy.AUTO_HIDE_DELAY_MS` 固定为 5000ms。`shouldScheduleAutoHide` 仅对可见、未打开快捷面板且处于 `Playing` / `Paused` 的交互态返回 true；`Loading`、`Buffering`、`Ended` 和 `Error` 不调度自动隐藏，避免阻塞提示、错误信息或设置面板被定时器清除。

`PlayerOsdState.toAutoHideSnapshot()` 负责把 `selectedQuickPanel != null` 映射为 `quickPanelOpen=true`；`PlayerScreen` 的自动隐藏 effect 监听该字段变化，保证打开快捷面板会取消旧的自动隐藏计时。

### PlayerOsdMotionSpec
| 字段 | 类型 | 说明 |
|------|------|------|
| enterDurationMs | Int | OSD 进入动画时长，`PlayerOsdMotionPolicy.TvDefault` 为 160ms |
| exitDurationMs | Int | OSD 退出动画时长，`PlayerOsdMotionPolicy.TvDefault` 为 120ms |
| slideDistanceFraction | Float | 竖向位移占 Overlay 高度的比例，`TvDefault` 为 0.025 |

`PlayerScreen` 使用 `AnimatedVisibility` 按 `PlayerOsdMotionPolicy.TvDefault` 执行短时 fade + 小幅 slide 转场；单元测试约束进入时长不超过 180ms、退出时长不超过 150ms、1080p 高度下位移不超过 32px，避免 OSD 转场在弱 TV 硬件上引入明显掉帧风险。

### PlaybackReportingCoordinator
| 字段/状态 | 类型 | 说明 |
|-----------|------|------|
| startedReported | Boolean | 防止重复发送 `Sessions/Playing` |
| stoppedReported | Boolean | 防止重复发送 `Sessions/Playing/Stopped` |
| effectiveProgressIntervalMs | Long | 进度节流间隔；构造参数为 0 或负数时回退到默认 10 秒 |
| lastProgressPositionMs | Long? | 进度上报节流基准，也用于同位置 seek 去重 |
| lastPausedState | Boolean? | 暂停/恢复状态去重 |

`PlaybackReportingCoordinator` 在 `Started` 之前忽略 Progress、暂停/恢复、seek 和 Stopped 事件，保证 Emby Check-ins 顺序为先 `Sessions/Playing`，再发送 `Sessions/Playing/Progress` 或 `Sessions/Playing/Stopped`。周期 Progress Tick 默认按位置增量做 10 秒节流，但当 `isPaused` 与 `lastPausedState` 不一致时会立即上报，避免 Emby 后台会话暂停/恢复状态滞后。一次遥控器 seek 可能同时由播放页即时路径和 Media3 `onPositionDiscontinuity` 触发，上报协调器会对相同位置且相同暂停状态的 seek Progress 去重，避免 Emby 后台收到重复 check-in。Media3 自动切集、播放列表迁移等非 seek 位置跳变只应由 `PlayerPlaybackController` 同步弹幕，不进入该 seek 上报路径。

播放状态上报事件:
| 事件 | 触发 |
|------|------|
| Started | 播放源准备后开始播放；续播媒体使用 `PlaybackSource.startPositionMs` 作为 `PositionTicks` 来源 |
| Progress | 默认每 10 秒、暂停/恢复状态变化、快退/快进；非正数节流间隔会回退到默认 10 秒 |
| Stopped | 播放自然结束、退出播放页或播放器释放 |

时间单位规则: Media3 使用毫秒，Emby 使用 ticks，转换为 `positionMs.coerceAtLeast(0) * 10000`；当毫秒值大于 `Long.MAX_VALUE / 10000` 时饱和为 `Long.MAX_VALUE`，避免极端输入溢出为负 ticks。

### PlayerLifecyclePlaybackPolicy
| 模型 | 字段/效果 | 说明 |
|------|-----------|------|
| PlayerLifecyclePlaybackSnapshot | isPlaying, danmakuEnabled, danmakuPaused, danmakuSettings | 生命周期恢复时读取的最新 OSD 播放意图和弹幕设置快照 |
| PlayerLifecyclePlaybackEffect | PlayPlayer, PausePlayer, PauseDanmaku, ReportPauseChanged, ApplyDanmaku | `PlayerScreen` 生命周期 observer 消费的 Media3、弹幕和 Emby pause check-in 副作用 |

`PlayerLifecyclePlaybackPolicy.onPause()` 固定生成暂停上报、暂停 Media3 和暂停弹幕效果；`onResume(snapshot)` 只有在 `snapshot.isPlaying=true` 时才生成恢复播放和恢复上报效果，并始终按最新弹幕开关/暂停/设置生成 AkDanmaku 播放命令，避免用户手动暂停后切后台再返回时被旧 observer 状态自动恢复播放或弹幕。

### Emby Playback Check-ins DTO
| 模型 | 用途 | 核心字段 |
|------|------|----------|
| EmbyPlaybackStartRequest | `POST Sessions/Playing` | ItemId, MediaSourceId, PlaySessionId, PlaylistItemId, PositionTicks, CanSeek, IsPaused, PlayMethod |
| EmbyPlaybackProgressRequest | `POST Sessions/Playing/Progress` | ItemId, MediaSourceId, PlaySessionId, PlaylistItemId, PositionTicks, IsPaused, IsMuted, PlayMethod |
| EmbyPlaybackStoppedRequest | `POST Sessions/Playing/Stopped` | ItemId, MediaSourceId, PlaySessionId, PlaylistItemId, PositionTicks |

### DanmakuCue
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 弹幕 ID |
| timeMs | Long | 相对播放时间 |
| text | String | 弹幕文本 |
| color | Int | RGB 颜色 |
| mode | DanmakuMode | 滚动、顶部或底部 |
