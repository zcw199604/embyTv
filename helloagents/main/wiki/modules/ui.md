# ui

## 目的
提供 Android TV Compose 页面、输入与播放导航。

## 模块概述
- **职责:** Setup 页面负责 Emby 连接，Home 页面负责媒体中心、媒体详情、季/集浏览和播放入口，Player 页面负责 Media3、Compose OSD 与 AkDanmaku 的组合展示。
- **状态:** 🚧开发中
- **最后更新:** 2026-05-28

## 规范

### 需求: 结构化 Emby 服务器配置与手机同步
**模块:** ui
服务器初始化配置使用结构化字段，并允许手机扫码填写后同步到 TV 表单。

#### 场景: TV 端配置表单
未连接 Emby 时：
- 设置页展示服务器地址、协议、端口、路径、用户名、密码六个配置项。
- TV 端布局必须在 1080p 安全区内完整展示用户名、密码和“确定连接”按钮；用户名和密码可并排以减少纵向高度。
- 协议支持 HTTPS/HTTP，HTTPS 默认端口 443，HTTP 默认端口 8096。
- 路径为可选项，连接前统一规范化为 Retrofit 可用的 `baseUrl`。
- 密码仅作为登录输入保留在内存，不落盘。

#### 场景: 手机扫码同步
设置页 Quick Setup 区域：
- 启动 TV 本机临时局域网同步页并展示二维码。
- 手机浏览器打开后填写同一组字段，点击“同步到电视”后只更新 TV 表单，不自动登录。
- 同步入口使用配对 token，离开设置页或连接成功后停止服务；同一设置页会话内允许重复提交以便用户修正表单。

#### 场景: 登录凭证
Emby 登录成功后：
- 保存 `serverUrl`、`userId`、`username`、`accessToken`、`serverId`、`deviceId` 和保存时间。
- `username` 仅用于后续多服务器/多用户列表展示和身份区分。
- 后续请求使用 `accessToken`，不保存密码。

### 需求: Android TV 初始化
**模块:** ui
使用 Compose 构建横屏电视页面，优先满足遥控器焦点和大屏布局。

#### 场景: 首页连接
用户输入 Emby 服务器配置和账号后：
- 能触发认证与首页 Dashboard 聚合加载。
- 加载失败时显示可读错误。

#### 场景: 播放页
用户选择媒体后：
- Media3 播放器进入全屏。
- AkDanmaku View 覆盖在播放器上方。
- 返回键退出播放页。

### 需求: Cinematic Glass TV 体验
**模块:** ui
基于设计稿 `stitch_emby_tv_interface_redesign.zip` 落地深色玻璃拟态 TV 体验，覆盖配置、首页和播放主链路。

#### 场景: 服务器配置
未连接 Emby 时：
- 首屏展示快速配对占位码和手动服务器配置区。
- 手动配置区复用 `HomeViewModel.connect()` 完成 Emby 认证。
- 密码使用输入遮罩，不在错误文案或日志中输出敏感内容。

#### 场景: 首页媒体中心
认证成功后：
- 首页展示顶部栏、媒体库卡片、继续观看或最近入库横向媒体行和迷你播放条。
- 媒体库卡片来自 `Users/{userId}/Views`，数量来自按 `ParentId` 统计的 `TotalRecordCount`。
- 媒体库卡片展示真实媒体库名称和 `ImageTags.Primary` 封面，媒体库数量较多时以横向可聚焦列表呈现。
- 继续观看来自 `Users/{userId}/Items/Resume`，进度来自 `UserData.PlayedPercentage` 或 `PlaybackPositionTicks / RunTimeTicks`。
- 继续观看卡片优先展示 `ImageTags.Thumb` 缩略图，缺失时按 `BackdropImageTags`、`ImageTags.Primary` 兜底；Episode 副标题展示真实剧名和 `SxxExx`。
- 继续观看为空时展示 `Users/{userId}/Items/Latest` 的最近入库条目。
- 继续观看下方按每个媒体库展示最新资源横排，数据来自 `Users/{userId}/Items/Latest?ParentId=...&Limit=8`；电影库按 Movie 展示，剧集库按 Series 维度展示。
- 剧集库最新资源如果 Emby 仍返回 Episode，Repository 会按 `SeriesId/SeriesName` 聚合为 Series 卡片。
- Series 卡片在 `unplayedItemCount > 0` 时显示“剩 n 集”角标；Movie 不显示剩余集数角标。
- 媒体卡片通过 Coil Compose 加载 `MediaItemSummary.imageUrl`、`thumbImageUrl` 或 `backdropImageUrl`，并支持 Emby 父级图片字段兜底。
- 首页不再展示本地硬编码 Movies、TV Shows、Anime 卡片、假进度或样例播放入口。

#### 场景: 媒体库资源列表
用户在首页或抽屉对媒体库按 OK/Enter 后：
- 进入 `LibraryContentScreen`，顶部展示返回按钮、媒体库名称和资源数量。
- 电影库列表展示 Movie，剧集库列表展示 Series，未知库优先展示 Movie/Series。
- 列表页提供加载、空状态、错误状态和遥控器可聚焦的重试按钮。
- Back 或顶部返回按钮返回首页。
- Movie/Series 卡片 OK 进入媒体详情页；Episode 卡片 OK 直接播放。

#### 场景: 媒体详情页
用户在首页、媒体库列表或收藏页对 Movie/Series 按 OK/Enter 后：
- 进入 `MediaDetailScreen`，显示真实标题、封面、简介、年份、类型、评分、分级和演员。
- Movie 详情页提供可聚焦“播放”按钮，OK 后读取 `PlaybackInfo` 并进入播放器。
- Series 详情页展示可聚焦季列表，每季显示封面、名称、集数和“剩 n 集”角标。
- 选中某季后展示该季 Episode 列表，Episode 卡片 OK 播放。
- Back 在季内 Episode 列表时返回季列表，再次 Back 关闭详情页并回到来源页面。

#### 场景: 收藏资源页
用户在抽屉选择“收藏”后：
- 进入 `FavoriteContentScreen`，顶部展示返回按钮、收藏分组标题和电影/剧集数量。
- 页面提供“电影”和“电视剧”两个可聚焦切换按钮。
- 电影分组展示收藏 Movie；电视剧分组展示收藏 Series，以及由收藏 Episode 聚合出的 Series。
- 每个收藏媒体卡片必须显示图片区域和资源名字；缺图片时使用占位图，名字缺失时使用剧名或条目 ID 兜底。
- Movie/Series 卡片 OK 进入媒体详情页；Episode 卡片 OK 直接播放。
- 页面提供加载、空状态、错误状态和遥控器可聚焦的重试按钮。
- Back 或顶部返回按钮返回首页。

#### 场景: 播放 OSD
播放页进入后：
- 点击媒体后先读取 `Items/{itemId}/PlaybackInfo`，`PlaybackSource.details` 携带真实媒体源和音视频/字幕流信息。
- `PlayerView` 关闭默认控制器，由 Compose OSD 展示标题、真实容器/编码/画质信息、进度、快进/快退、播放/暂停、音轨/字幕/弹幕入口。
- Audio/Subtitles 展示真实默认流或首个流标题；实际切换暂未实现时提供禁用原因。
- OK/方向键唤起 OSD；Back 在 OSD 可见时先隐藏，再次 Back 退出播放页。
- 弹幕开关和播放暂停状态同步到 AkDanmaku。
- 播放开始、暂停/恢复、快退/快进、播放进度和退出播放页会通过 Emby Playback Check-ins 同步到服务器后台。

### 需求: TV 遥控器完整操作
**模块:** ui
所有可见操作必须能通过方向键、OK/Enter 和 Back 完成，未实现能力必须给出禁用态或明确提示。

#### 场景: 全局按键契约
用户使用遥控器时：
- 方向键优先交给当前聚焦控件处理，根容器不抢占 OSD 内部控件的方向键和 OK/Enter。
- Back 在抽屉打开时关闭抽屉，在媒体库列表页返回首页，在播放 OSD 可见时隐藏 OSD，OSD 隐藏时退出播放页。
- 禁用入口可显示原因，OK/Enter 不允许空响应。

#### 场景: 首页与抽屉
首页打开抽屉后：
- 抽屉请求初始焦点并形成焦点组。
- Back 或关闭按钮关闭抽屉，关闭后焦点返回菜单按钮。
- 媒体库卡片和抽屉媒体库项可用 OK/Enter 进入媒体库列表页；未实现入口保留禁用态或明确提示。

#### 场景: 详情页遥控器路径
详情页打开后：
- Back 按层级处理：季内 Episode 列表返回季列表，详情页根层关闭详情。
- Movie 详情的播放按钮、Series 的季卡片、Episode 卡片和错误重试按钮均可聚焦并响应 OK/Enter。
- 详情页缺失图片、人物或评分时显示兜底文案或省略对应字段，不出现空焦点操作。

#### 场景: 播放 OSD
OSD 显示后：
- 播放/暂停按钮获取初始焦点。
- 快退、快进、播放/暂停、弹幕开关支持 OK/Enter。
- 上一集、下一集、Audio、Subtitles 在真实功能未实现前显示禁用提示。

## API接口
无外部 API。

## 数据模型
使用 `HomeUiState`、`EmbyHomeDashboard`、`EmbyLibraryContent`、`EmbyFavoriteDashboard`、`EmbyMediaDetail`、`EmbySeasonSummary`、`EmbySeasonEpisodes`、`HomeDashboardUiModel`、`FavoriteContentUiState`、`MediaDetailUiState`、`DrawerUiState`、`LibraryContentUiState`、`MediaCardUiModel`、`PlayerOsdState`、`PlaybackSource` 和 `PlaybackDetails`。

## 依赖
- data
- player
- danmaku
- Coil Compose
- ZXing Core
- NanoHTTPD

## 变更历史
- [202605281300_media_detail_seasons](../../history/2026-05/202605281300_media_detail_seasons/) - 新增 Movie/Series 详情页、Series 季列表、季内 Episode 列表和详情页遥控器返回层级。
- [202605281045_favorite_resources_by_type](../../history/2026-05/202605281045_favorite_resources_by_type/) - 新增收藏页入口、电影/电视剧切换和遥控器返回路径。
- [202605272217_library_browse_series_grouping](../../history/2026-05/202605272217_library_browse_series_grouping/) - 修复 Emby 图片兜底，新增媒体库列表页，剧集库按 Series 展示并显示剩余集数角标。
- [202605272133_emby_playback_reporting](../../history/2026-05/202605272133_emby_playback_reporting/) - 播放页接入 Emby 播放状态上报。
- [202605272047_home_library_latest_sections](../../history/2026-05/202605272047_home_library_latest_sections/) - 首页媒体库真实封面、继续观看剧集信息和按库最新资源分区。
- [202605271602_emby_real_data_replacement](../../history/2026-05/202605271602_emby_real_data_replacement/) - 首页和播放器可见数据替换为 Emby 真实 API 数据，移除样例播放入口。
- [202605271514_emby_server_mobile_sync](../../history/2026-05/202605271514_emby_server_mobile_sync/) - 拆分 Emby 服务器配置字段，支持手机扫码同步和 token 凭证保存。
- [202605271434_remote_control_support](../../history/2026-05/202605271434_remote_control_support/) - 补齐 TV 遥控器焦点、Back、禁用反馈和播放 OSD 操作闭环。
- [202605271353_tv_ui_redesign_core](../../history/2026-05/202605271353_tv_ui_redesign_core/) - 落地 Cinematic Glass 配置页、首页媒体中心和播放 OSD。
- [202605201342_emby_tv_init](../../history/2026-05/202605201342_emby_tv_init/) - 初始化 TV UI 与播放页面。
