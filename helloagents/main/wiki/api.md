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

**真实接口探测:** 2026-05-27 使用测试服务器验证通过。`Fields` 可扩展为 `Overview,PrimaryImageAspectRatio,ImageTags,UserData,RunTimeTicks,MediaSources,Genres,ProductionYear,CommunityRating,CriticRating,OfficialRating,DateCreated,PremiereDate,ParentId,SeriesName,SeasonName,IndexNumber,ParentIndexNumber`，可支撑媒体卡片、进度、年份和播放页标题信息。

### 媒体库视图

#### GET Users/{userId}/Views
**描述:** 获取用户可见的媒体库/频道视图，用于替换首页硬编码 LibraryCard。

**真实接口探测:** 2026-05-27 使用测试服务器验证通过，返回 `CollectionFolder` 和 `Channel`。样例字段结构包含 `Id`、`Name`、`Type`、`CollectionType`、`ChildCount`、`ImageTags`、`UserData`。测试服务器返回 8 个视图，包含 movies/tvshows 等 CollectionType。

**落地实现:** `EmbyRepository.loadHomeDashboard()` 调用该接口生成 `EmbyLibrarySummary`，媒体库名称、类型和封面均来自 Emby 返回值。

#### GET Users/{userId}/Items?ParentId={viewId}&Recursive=true&IncludeItemTypes=Movie,Episode&Limit=0
**描述:** 按媒体库统计可播放视频数量，用于显示媒体库真实数量。

**真实接口探测:** 2026-05-27 使用测试服务器验证通过，`TotalRecordCount` 可用于媒体库数量展示。

**落地实现:** 首页逐个 View 查询 `TotalRecordCount`，只取计数，不把该接口作为列表数据源。

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

### 播放流

#### GET Videos/{itemId}/stream
**描述:** Media3 使用该地址播放视频流。

**请求参数:**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| itemId | string | 是 | 媒体条目 ID |
| Static | boolean | 是 | 当前固定为 true |
| api_key | string | 是 | Emby AccessToken |
