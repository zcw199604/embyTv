# API 手册

## 概述
当前客户端通过 Emby HTTP API 完成认证、首页 Dashboard 聚合和播放详情读取。请求头使用 `X-Emby-Authorization` 标识客户端和设备；普通 API 通过请求头携带 token，视频流地址通过 `api_key` 查询参数兼容 Emby 播放接口。

## 认证方式
- 登录接口返回 `AccessToken`。
- 后续请求使用 `X-Emby-Authorization` 的 `Token` 字段，并通过 `api_key` 查询参数兼容 Emby 流媒体接口。

---

## 接口列表

### Emby 认证

#### POST Users/AuthenticateByName
**描述:** 使用用户名密码获取 Emby 访问令牌。

**请求参数:**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| Username | string | 是 | Emby 用户名 |
| Pw | string | 否 | Emby 密码 |

**响应:**
```json
{
  "AccessToken": "token",
  "ServerId": "server-id",
  "User": {
    "Id": "user-id",
    "Name": "name"
  }
}
```

### 媒体列表

#### GET Users/{userId}/Items
**描述:** 递归获取 Movie 和 Episode 条目。当前不再作为首页首屏数据源，保留为兼容接口；首页改用 `Views`、`Resume`、`Latest` 和按库统计，避免启动时全量拉取大媒体库。

**请求参数:**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | string | 是 | 当前用户 ID |
| Recursive | boolean | 是 | 当前固定为 true |
| IncludeItemTypes | string | 是 | 当前为 Movie,Episode |
| Fields | string | 是 | Overview,PrimaryImageAspectRatio,ImageTags |

**真实接口探测:** 2026-05-27 使用测试服务器验证通过。`Fields` 已扩展为包含 `PrimaryImageTag`、`ParentThumbItemId`、`ParentThumbImageTag`、`ParentBackdropItemId`、`ParentBackdropImageTags`、`SeriesId`、`SeriesPrimaryImageTag`、`RecursiveItemCount`、`ChildCount`、`DateCreated` 和 `UserData` 等字段，可支撑封面兜底、剧集聚合、未播放集数和播放页标题信息。

### 媒体库视图

#### GET Users/{userId}/Views
**描述:** 获取用户可见的媒体库/频道视图，用于替换首页硬编码 LibraryCard。

**真实接口探测:** 2026-05-27 使用测试服务器验证通过，返回 `CollectionFolder` 和 `Channel`。样例字段结构包含 `Id`、`Name`、`Type`、`CollectionType`、`ChildCount`、`ImageTags`、`UserData`。测试服务器返回 8 个视图，包含 movies/tvshows 等 CollectionType。

**落地实现:** `EmbyRepository.loadHomeDashboard()` 调用该接口生成 `EmbyLibrarySummary`，媒体库名称、类型和封面均来自 Emby 返回值。

#### GET Users/{userId}/Items?ParentId={viewId}&Recursive=true&IncludeItemTypes=Movie,Episode&Limit=0
**描述:** 按媒体库统计可播放视频数量，用于显示媒体库真实数量。

**真实接口探测:** 2026-05-27 使用测试服务器验证通过，`TotalRecordCount` 可用于媒体库数量展示。

**落地实现:** 首页逐个 View 查询 `TotalRecordCount`，只取计数，不把该接口作为列表数据源。

#### GET Users/{userId}/Items?ParentId={viewId}&Recursive=true&IncludeItemTypes={types}&StartIndex=0&Limit=60&SortBy=SortName&SortOrder=Ascending
**描述:** 进入媒体库后获取首屏资源列表。

**请求参数:**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| ParentId | string | 是 | 媒体库 View ID |
| Recursive | boolean | 是 | 当前固定为 true |
| IncludeItemTypes | string | 是 | movies 使用 Movie，tvshows 使用 Series，未知库使用 Movie,Series |
| StartIndex | int | 是 | 当前固定为 0 |
| Limit | int | 是 | 当前固定为 60，分页后续规划 |
| SortBy | string | 是 | 当前为 SortName |
| SortOrder | string | 是 | 当前为 Ascending |
| Fields | string | 是 | 包含图片、剧集、播放进度和未播放计数字段 |

**落地实现:** `EmbyRepository.loadLibraryContent()` 返回 `EmbyLibraryContent`，供 `LibraryContentScreen` 展示资源列表。

### 继续观看

#### GET Users/{userId}/Items/Resume
**描述:** 获取用户未完成播放的视频，用于替换首页 `Continue Watching` 假进度。

**请求参数:**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| Recursive | boolean | 是 | 建议 true |
| MediaTypes | string | 是 | Video |
| Fields | string | 是 | 建议包含 UserData,RunTimeTicks,ImageTags,Overview,MediaSources |

**真实接口探测:** 2026-05-27 使用测试服务器验证通过，返回 `UserData.PlaybackPositionTicks`、`UserData.PlayedPercentage`、`RunTimeTicks`、`SeriesName`、`SeasonName`、`ImageTags` 等字段，可直接计算首页进度条和剩余时长。

**落地实现:** `HomeDashboardMapper` 优先展示该接口返回的条目，进度条使用 `PlayedPercentage`，缺失时使用 `PlaybackPositionTicks / RunTimeTicks`。

### 最新媒体

#### GET Users/{userId}/Items/Latest
**描述:** 获取最近入库视频，用于首页“最新添加”或空继续观看时的兜底列表。

**真实接口探测:** 2026-05-27 使用测试服务器验证通过。该接口返回数组而不是带 `Items` 包装的对象。

**落地实现:** 当继续观看为空时，首页媒体横排显示最近入库条目，标题改为“最近入库”。

#### GET Users/{userId}/Items/Latest?ParentId={viewId}&IncludeItemTypes={types}&GroupItems={bool}&Limit=8
**描述:** 按媒体库获取首页横排最新入库资源。

**请求参数:**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| ParentId | string | 是 | 媒体库 View ID |
| IncludeItemTypes | string | 是 | movies 使用 Movie，tvshows 使用 Episode |
| GroupItems | boolean | 否 | tvshows 固定 true，让 Episode 按 Series 聚合 |
| Limit | int | 是 | 当前固定为 8 |
| Fields | string | 是 | 包含图片、剧集、未播放计数和日期字段 |

**落地实现:** `EmbyRepository.loadHomeDashboard()` 对 movies 直接映射 Movie；对 tvshows 请求 `GroupItems=true`，若返回仍是 Episode，则按 `SeriesId/SeriesName` 本地聚合为 Series 卡片。

### 图片资源

#### GET Items/{itemId}/Images/{imageType}?tag={tag}
**描述:** 根据 Emby 返回的图片 tag 构造媒体库和媒体条目图片 URL。

**图片类型:**
| 类型 | 来源字段 | 当前用途 |
|------|----------|----------|
| Primary | `ImageTags.Primary` | 媒体库封面、媒体条目主图兜底 |
| Thumb | `ImageTags.Thumb` | 继续观看和最新资源缩略图优先来源 |
| Backdrop | `BackdropImageTags[0]` | 缩略图缺失时的背景图兜底 |

**落地实现:** 图片兜底顺序覆盖 `ImageTags.Primary`、`PrimaryImageTag`、`ImageTags.Thumb`、`BackdropImageTags[0]`、`ParentThumbItemId + ParentThumbImageTag`、`ParentBackdropItemId + ParentBackdropImageTags[0]`、`SeriesId + SeriesPrimaryImageTag`。当只有 item id 而没有 tag 时，允许构造 `/Items/{itemId}/Images/{type}` 或 `/Items/{itemId}/Images/Backdrop/0` 作为 Emby 图片端点兜底；仍缺失时显示本地占位。

### 剧集下一集

#### GET Shows/NextUp
**描述:** 获取剧集下一集候选，可作为剧集首页模块。当前不作为主闭环依赖。

**真实接口探测:** 2026-05-27 使用测试服务器请求成功，但当前返回 `TotalRecordCount=0`。

### 播放信息

#### GET Items/{itemId}/PlaybackInfo
**描述:** 获取媒体源和音视频/字幕流信息，用于替换播放器 OSD 中硬编码的 `HEVC · 4K HDR`、`2160p · HDR10`、Audio/Subtitles 状态。

**请求参数:**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| itemId | string | 是 | 媒体条目 ID |
| UserId | string | 是 | 当前用户 ID |

**真实接口探测:** 2026-05-27 使用测试服务器验证通过。响应包含 `PlaySessionId` 和 `MediaSources`；`MediaSources[0].MediaStreams` 中 Video 流包含 `Codec`、`Width`、`Height`、`VideoRange`、`BitRate`，Audio 流包含 `Codec`、`Channels`、`DisplayTitle`，Subtitle 流按存在情况返回。

**落地实现:** 用户点击媒体时才调用该接口生成 `PlaybackSource.details`，播放器 OSD 顶部副标题、右上角质量标签、Audio/Subtitles 状态均从 `PlaybackDetails` 读取；音轨/字幕切换能力仍为后续独立切片。

### 播放状态上报

Emby 官方将播放状态同步称为 Playback Check-ins。当前客户端使用 `X-Emby-Authorization` 认证头调用以下接口，不在查询参数中拼接 token。

#### POST Sessions/Playing
**描述:** 播放开始时通知 Emby 当前设备开始播放指定媒体。

**请求字段:**
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| ItemId | string | 是 | 媒体条目 ID |
| MediaSourceId | string | 否 | `PlaybackInfo.MediaSources[].Id` |
| PlaySessionId | string | 否 | `PlaybackInfo.PlaySessionId` |
| PositionTicks | long | 是 | 当前播放位置 ticks |
| CanSeek | boolean | 是 | 当前固定 true |
| IsPaused | boolean | 是 | 开始播放时 false |
| PlayMethod | string | 是 | 当前固定 DirectPlay |

#### POST Sessions/Playing/Progress
**描述:** 播放中定期上报进度，也用于暂停、恢复和 seek 后立即同步状态。

**请求字段:**
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| ItemId | string | 是 | 媒体条目 ID |
| MediaSourceId | string | 否 | 媒体源 ID |
| PlaySessionId | string | 否 | 播放会话 ID |
| PositionTicks | long | 是 | 当前播放位置 ticks |
| IsPaused | boolean | 是 | 是否暂停 |
| IsMuted | boolean | 是 | 当前固定 false |
| PlayMethod | string | 是 | 当前固定 DirectPlay |

#### POST Sessions/Playing/Stopped
**描述:** 退出播放页、播放自然结束或播放器释放时通知 Emby 停止播放。

**请求字段:**
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| ItemId | string | 是 | 媒体条目 ID |
| MediaSourceId | string | 否 | 媒体源 ID |
| PlaySessionId | string | 否 | 播放会话 ID |
| PositionTicks | long | 是 | 停止时播放位置 ticks |

**落地实现:** `PlayerScreen` 通过 `PlaybackReportingCoordinator` 控制 Playing/Progress/Stopped 去重和节流；进度默认 10 秒上报一次，暂停、恢复、快退、快进和停止为强制上报。

### 播放流

#### GET Videos/{itemId}/stream
**描述:** Media3 使用该地址播放视频流。

**请求参数:**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| itemId | string | 是 | 媒体条目 ID |
| Static | boolean | 是 | 当前固定为 true |
| api_key | string | 是 | Emby AccessToken |
