# 变更提案: 全量替换为 Emby 真实数据

## 需求背景
当前客户端已经接入 Emby 认证、媒体列表、图片和播放 URL，但首页和播放器仍存在本地假数据：固定媒体库卡片、`Anime = 0 items`、`Continue Watching` 假进度、样例播放入口、播放器硬编码格式 `HEVC · 4K HDR`、固定音轨/字幕未支持提示等。用户要求页面显示数据全部通过 Emby 交互获得，并提供了测试服务器用于验证真实数据结构。

## 产品分析

### 目标用户与场景
- **用户群体:** 使用 Android TV 浏览真实 Emby 媒体库的家庭用户。
- **使用场景:** 登录后进入首页，看到自己的媒体库、继续观看、最近入库和真实播放信息，而不是演示数据或占位文案。
- **核心痛点:** 假数据会误导用户判断功能状态，播放器格式和进度如果不来自 Emby，也无法反映真实媒体质量与播放历史。

### 价值主张与成功指标
- **价值主张:** 页面展示与 Emby 服务器状态一致，用户看到的媒体库、进度、播放信息都可追溯到真实接口。
- **成功指标:** 首页无样例播放入口、无假进度、无硬编码媒体库；播放器 OSD 的标题、格式、分辨率、HDR、音轨/字幕状态来自 Emby `PlaybackInfo` 或当前播放器状态。

### 人文关怀
使用真实 Emby 数据时，仍需避免输出访问令牌、密码和具体私密媒体标题到日志或知识库。知识库只记录接口字段结构和聚合结果，不记录用户私有媒体明细。

## 变更内容
1. 扩展 Emby API 层，新增 `Views`、按库统计、`Resume`、`Latest`、`PlaybackInfo` 等接口。
2. 扩展 DTO 和领域模型，覆盖媒体库、继续观看、最近入库、播放进度、媒体源、音轨和字幕。
3. 首页 Dashboard 改为由 Emby 数据驱动，移除固定 Movies/TV Shows/Anime 卡片和本地假进度。
4. 移除 `samplePlaybackSource()`、`样例播放` 按钮和 Big Buck Bunny 演示数据。
5. 播放器打开前加载真实 `PlaybackInfo`，OSD 用真实容器、编码、分辨率、HDR、音轨和字幕状态替换硬编码文案。
6. 更新知识库中 Emby API 与数据模型说明。

## 范围边界
- **范围内:** 首页展示数据、播放入口、播放详情、播放器 OSD、Emby API/DTO/Repository、UI 状态模型、单元测试、知识库。
- **范围外:** 真实弹幕源接入、播放历史回写到 Emby、音轨/字幕切换实际生效、多服务器选择页、离线缓存、复杂分页无限滚动。
- **拆分说明:** 本方案只覆盖“当前页面可见数据不再造假”的闭环；音轨/字幕切换和弹幕真实源是后续独立切片。

## 影响范围
- **模块:** data、domain、ui/home、ui/player、core/di、knowledge base。
- **文件:** `EmbyApi.kt`、DTO、`EmbyRepository.kt`、`MediaItemSummary.kt` 或新增领域模型、`PlaybackSource.kt`、`HomeDashboardModels.kt`、`HomeViewModel.kt`、`HomeScreen.kt`、`PlayerScreen.kt`、相关测试和知识库文档。
- **API:** 新增 Emby API 方法；移除 AppContainer 样例播放契约。
- **数据:** 新增真实媒体库、继续观看、最近入库和播放详情领域模型；不保存私密媒体明细到本地。

## 真实接口验证摘要
2026-05-27 已使用测试服务器 `http://10.10.10.100:60096/` 和用户 `wm` 完成只输出结构的探测，未记录 token、密码和具体媒体标题。

- `POST /Users/AuthenticateByName`: 成功，响应包含 `AccessToken`、`ServerId`、`SessionInfo`、`User`。
- `GET /Users/{userId}/Views`: 成功，返回 8 个视图，字段包含 `Id`、`Name`、`Type`、`CollectionType`、`ChildCount`、`ImageTags`。
- `GET /Users/{userId}/Items?ParentId=...&Recursive=true&IncludeItemTypes=Movie,Episode&Limit=0`: 成功，`TotalRecordCount` 可用于媒体库数量。
- `GET /Users/{userId}/Items/Resume`: 成功，返回 44 条继续观看，字段包含 `UserData.PlaybackPositionTicks`、`UserData.PlayedPercentage`、`RunTimeTicks`。
- `GET /Users/{userId}/Items/Latest`: 成功，返回数组，非 `Items` 包装对象。
- `GET /Shows/NextUp`: 请求成功，但当前测试用户返回 0 条。
- `GET /Items/{itemId}/PlaybackInfo?UserId=...`: 成功，响应包含 `PlaySessionId`、`MediaSources`、Video/Audio/Subtitle `MediaStreams` 字段。

## 核心场景

### 需求: 首页全部使用 Emby 真实数据
**模块:** ui/home, data
登录成功后，首页展示真实媒体库、真实继续观看和真实最近入库。

#### 场景: 媒体库卡片
用户进入首页：
- 媒体库卡片来自 `/Users/{userId}/Views`。
- 卡片标题使用 `Name`，类型使用 `CollectionType`，数量来自按 `ParentId` 统计的 `TotalRecordCount`。
- 没有硬编码 `Movies`、`TV Shows`、`Anime`。

#### 场景: 继续观看
用户有未完成播放记录：
- `Continue Watching` 来自 `/Users/{userId}/Items/Resume`。
- 进度条来自 `UserData.PlaybackPositionTicks` 或 `PlayedPercentage`。
- 标题、副标题、海报来自 Emby item 字段。

#### 场景: 兜底列表
用户没有继续观看：
- 首页展示 `/Users/{userId}/Items/Latest` 的最近入库列表。
- 不展示“样例播放”按钮或 Big Buck Bunny。

### 需求: 播放器全部使用 Emby 真实数据
**模块:** ui/player, data
用户从首页选择媒体后，播放器 OSD 不再显示硬编码格式。

#### 场景: 播放前加载详情
用户点击媒体：
- Repository 调用 `/Items/{itemId}/PlaybackInfo?UserId=...`。
- 生成 `PlaybackSource` 时携带真实媒体源、音视频流、字幕流和播放会话 ID。

#### 场景: OSD 展示真实格式
播放器 OSD 可见时：
- 顶部副标题使用真实 DirectPlay/容器/视频编码/分辨率/HDR 信息。
- 右上角质量徽标来自视频流 `Width/Height/VideoRange`。
- Audio/Subtitles 状态基于真实流数量和默认流，若没有字幕则显示“无字幕”，若有字幕但切换未实现则显示可读禁用原因。

### 需求: 移除演示数据
**模块:** ui, core/di
生产页面不显示本地样例播放数据。

#### 场景: 初始化和首页
未连接或无媒体时：
- 不提供样例播放入口。
- 文案提示用户检查 Emby 媒体库或筛选条件。
- `AppContainer.samplePlaybackSource()` 和硬编码弹幕样例从生产入口移除。

## 风险评估
- **风险:** 全量真实接口可能请求量增加，尤其媒体库数量统计需要多个 View 逐一查询。
- **缓解:** 首版串行或有限并发加载，限制每个列表数量；后续可加缓存和分页。
- **风险:** 测试服务器数据量很大，`Recursive=true` 全量拉取会慢。
- **缓解:** 首页使用 `Limit`、`Resume`、`Latest` 和按库统计接口，避免一次拉取 4 万以上条目。
- **风险:** 部分 Emby 字段可能为空或不同版本返回不一致。
- **缓解:** DTO 字段全部可空，UI 提供真实但保守的空态，不回退为假数据。
- **风险:** 音轨/字幕切换真实生效涉及 Media3 track selection，超出本切片。
- **缓解:** 本次只展示真实流状态和禁用原因，切换能力拆后续方案。
