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
| type | String | Movie 或 Episode |
| overview | String? | 简介 |
| imageUrl | String? | 主图地址 |
| seriesName | String? | 剧集所属剧名 |
| seasonName | String? | 季名称 |
| runTimeTicks | Long? | Emby ticks 总时长 |
| playbackPositionTicks | Long | 当前播放位置 ticks |
| playedPercentage | Double? | Emby 已播放百分比 |
| productionYear | Int? | 制作年份 |

### EmbyLibrarySummary
| 字段 | 类型 | 来源 | 说明 |
|------|------|------|------|
| id | String | `Views.Items[].Id` | 媒体库 ID |
| name | String | `Views.Items[].Name` | 媒体库名称 |
| collectionType | String? | `Views.Items[].CollectionType` | movies/tvshows 等 |
| type | String | `Views.Items[].Type` | CollectionFolder/Channel |
| itemCount | Int | `Users/{userId}/Items?ParentId=...` 的 `TotalRecordCount` | 视频数量 |
| imageUrl | String? | `Views.Items[].ImageTags.Primary` | 媒体库封面 |

### EmbyHomeDashboard
| 字段 | 类型 | 来源 | 说明 |
|------|------|------|------|
| libraries | List<EmbyLibrarySummary> | `Users/{userId}/Views` + 按库统计 | 首页媒体库卡片 |
| resumeItems | List<MediaItemSummary> | `Users/{userId}/Items/Resume` | 继续观看 |
| latestItems | List<MediaItemSummary> | `Users/{userId}/Items/Latest` | 最近入库兜底 |

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
| details | PlaybackDetails | 从 `PlaybackInfo` 生成的播放详情 |
| danmaku | List<DanmakuCue> | 弹幕列表 |

播放器 OSD 已使用 `PlaybackSource.details` 展示真实容器、编码、分辨率、HDR、音轨和字幕状态；真实音轨/字幕切换仍未实现。

### DanmakuCue
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 弹幕 ID |
| timeMs | Long | 相对播放时间 |
| text | String | 弹幕文本 |
| color | Int | RGB 颜色 |
| mode | DanmakuMode | 滚动、顶部或底部 |
