# 数据模型

## 概述
当前不引入本地数据库，数据以内存状态和加密 SharedPreferences 凭证存储传递。Emby 页面展示应优先来自真实 API，禁止继续使用假进度、硬编码媒体格式和样例播放数据作为正式页面内容。

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

### EmbyHomeDashboard
| 字段 | 类型 | 来源 | 说明 |
|------|------|------|------|
| libraries | List<EmbyLibrarySummary> | `Users/{userId}/Views` + 按库统计 | 首页媒体库卡片 |
| resumeItems | List<MediaItemSummary> | `Users/{userId}/Items/Resume` | 继续观看 |
| latestItems | List<MediaItemSummary> | `Users/{userId}/Items/Latest` | 最近入库兜底 |
| libraryLatestSections | List<EmbyLibraryLatestSection> | 按每个媒体库 ParentId 查询最新资源 | 首页按媒体库展示最新内容 |

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
| playbackSummaryLabel | String | 派生字段 | Direct Play + 容器 + 视频编码 |
| qualityLabel | String | 派生字段 | 分辨率 + HDR/SDR 信息 |
| audioLabel | String | 派生字段 | 默认音轨或首个音轨标题 |
| subtitleLabel | String | 派生字段 | 默认字幕或首个字幕标题 |

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
| displayTitle | String? | 展示标题 |
| channels | Int? | 声道数 |
| language | String? | 语言 |
| isDefault | Boolean | 是否默认 |
| isForced | Boolean | 是否强制字幕 |

### PlaybackSource
| 字段 | 类型 | 说明 |
|------|------|------|
| itemId | String | Emby 条目 ID |
| title | String | 标题 |
| streamUrl | String | Media3 播放地址 |
| session | EmbySession? | 播放状态上报所需认证上下文 |
| deviceId | String? | 播放状态上报所需设备 ID |
| details | PlaybackDetails | 从 `PlaybackInfo` 生成的播放详情 |
| danmaku | List<DanmakuCue> | 弹幕列表 |

播放器 OSD 已使用 `PlaybackSource.details` 展示真实容器、编码、分辨率、HDR、音轨和字幕状态；真实音轨/字幕切换仍未实现。

### PlaybackReportingCoordinator
| 字段/状态 | 类型 | 说明 |
|-----------|------|------|
| startedReported | Boolean | 防止重复发送 `Sessions/Playing` |
| stoppedReported | Boolean | 防止重复发送 `Sessions/Playing/Stopped` |
| lastProgressPositionMs | Long? | 进度上报节流基准 |
| lastPausedState | Boolean? | 暂停/恢复状态去重 |

播放状态上报事件:
| 事件 | 触发 |
|------|------|
| Started | 播放源准备后开始播放 |
| Progress | 默认每 10 秒、暂停/恢复、快退/快进 |
| Stopped | 播放自然结束、退出播放页或播放器释放 |

时间单位规则: Media3 使用毫秒，Emby 使用 ticks，转换为 `positionMs.coerceAtLeast(0) * 10000`。

### Emby Playback Check-ins DTO
| 模型 | 用途 | 核心字段 |
|------|------|----------|
| EmbyPlaybackStartRequest | `POST Sessions/Playing` | ItemId, MediaSourceId, PlaySessionId, PositionTicks, CanSeek, IsPaused, PlayMethod |
| EmbyPlaybackProgressRequest | `POST Sessions/Playing/Progress` | ItemId, MediaSourceId, PlaySessionId, PositionTicks, IsPaused, IsMuted, PlayMethod |
| EmbyPlaybackStoppedRequest | `POST Sessions/Playing/Stopped` | ItemId, MediaSourceId, PlaySessionId, PositionTicks |

### DanmakuCue
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 弹幕 ID |
| timeMs | Long | 相对播放时间 |
| text | String | 弹幕文本 |
| color | Int | RGB 颜色 |
| mode | DanmakuMode | 滚动、顶部或底部 |
