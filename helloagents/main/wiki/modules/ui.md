# ui

## 目的
提供 Android TV Compose 页面、输入与播放导航。

## 模块概述
- **职责:** Setup 页面负责 Emby 连接，Home 页面负责媒体中心、搜索、发现页、收藏、媒体详情、季/集浏览和播放入口，Player 页面负责 Media3、Compose OSD、音字幕切换、连续播放与 AkDanmaku 的组合展示。
- **状态:** 🚧开发中
- **最后更新:** 2026-05-29

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
- 存在多个已保存身份时，启动后进入身份选择页，用户可用遥控器选择进入、删除某个身份或添加新服务器。

### 需求: Android TV 初始化
**模块:** ui
使用 Compose 构建横屏电视页面，优先满足遥控器焦点和大屏布局。

#### 场景: 首页连接
用户输入 Emby 服务器配置和账号后：
- 能触发认证与首页 Dashboard 聚合加载。
- 加载失败时显示可读错误。
- App 启动时优先尝试恢复已保存 token；只有没有凭证或凭证失效时才启动手机扫码同步服务，避免已登录用户冷启动时出现多余同步服务开销。

#### 场景: 播放页
用户选择媒体后：
- Media3 播放器进入全屏。
- AkDanmaku View 覆盖在播放器上方。
- 返回键退出播放页。

### 需求: Cinematic Glass TV 体验
**模块:** ui
基于设计稿 `stitch_emby_tv_interface_redesign.zip` 落地深色玻璃拟态 TV 体验，覆盖配置、首页和播放主链路。

#### 场景: 通用视觉反馈
页面和卡片复用 Cinematic Glass 组件时：
- `GlassPanel` 在焦点态使用 200ms 动画，将边框从 1dp 过渡到 3dp，并显示 Emby Green 到强调黄的渐变边框和 8dp 阴影。
- `CinematicGlassColors` 从 `EmbyTvTheme` 的当前主题读取颜色，保持既有组件 API 不变。
- 网络图片通过 Coil `ImageRequest` 加载，统一保留 `X-Emby-Authorization` Header、内存/磁盘 cache key、300ms crossfade、加载占位和失败占位。
- 图片缺失、加载中或加载失败时必须保持稳定占位，不允许出现白屏闪烁。
- 加载失败和空数据场景优先使用 `ErrorStatePanel` / `EmptyStatePanel` 展示图标、标题、说明和可选重试按钮。

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
- Coil 图片请求通过 `LocalEmbyImageAuthorizationHeader` 注入当前 session 的 `X-Emby-Authorization`，认证服务器不需要将 token 暴露在图片 URL 中。
- 首页不再展示本地硬编码 Movies、TV Shows、Anime 卡片、假进度或样例播放入口。

#### 场景: 媒体库资源列表
用户在首页或抽屉对媒体库按 OK/Enter 后：
- 进入 `LibraryContentScreen`，顶部展示返回按钮、媒体库名称和资源数量。
- 电影库列表展示 Movie，剧集库列表展示 Series，未知库优先展示 Movie/Series。
- 列表页提供加载、空状态、错误状态和遥控器可聚焦的重试按钮。
- 当资源数量达到长列表阈值时，页面右侧显示字母索引侧边栏，只启用实际存在内容的首字母。
- 用户点击字母索引后列表平滑滚动到对应首字母资源，并显示滚动位置指示器。
- Back 或顶部返回按钮返回首页。
- Movie/Series 卡片 OK 进入媒体详情页；Episode 卡片 OK 直接播放。

#### 场景: 加载骨架屏
主要页面加载内容时：
- 首页媒体库、继续观看和按库最新资源加载时显示横向媒体卡片骨架。
- 媒体库、搜索、收藏、发现和发现入口资源列表加载时显示与实际卡片网格接近的骨架布局。
- 媒体详情页加载时显示详情页骨架，季内 Episode 加载时显示列表骨架。
- 骨架屏使用 `Modifier.shimmerEffect()` 的 1.2 秒循环渐变动画，颜色遵循 Cinematic Glass 深色主题。

#### 场景: 显示与辅助设置
用户从抽屉进入设置后：
- 页面展示主题、高对比度和字体大小设置项。
- 主题支持 Cinematic Glass、Dark Minimal 和 Emby Classic。
- 高对比度开启后覆盖当前主题颜色，使用黑底、白字和高亮绿色主色。
- 字体大小偏好支持 Small、Normal、Large、ExtraLarge 四档，并持久化到 DataStore。
- 语言偏好支持跟随系统、简体中文和英文；固定语言模式下，Compose localized Context 仍需随 Android `LocalConfiguration` 变化重建，避免系统字体、屏幕或区域配置变化后资源上下文陈旧。
- Back 或顶部返回按钮返回首页。

#### 场景: 媒体详情页
用户在首页、媒体库列表或收藏页对 Movie/Series 按 OK/Enter 后：
- 进入 `MediaDetailScreen`，显示真实标题、封面、简介、年份、类型、评分、分级和演员。
- 详情页需要以独立区域展示“媒体信息”和“演员信息”；媒体信息至少包含 Emby 返回的年份、时长、类型、评分、分级、首播日期、制片方和剧集数量等可用字段，演员信息展示演员名和饰演角色。
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
- 播放进度条采用双层进度显示：灰色缓冲进度层来自 Media3 `bufferedPosition`，绿色播放进度层来自当前播放位置。
- Audio/Subtitles 展示真实默认流或首个流标题；实际切换暂未实现时提供禁用原因。
- Audio/Subtitles 根据 Media3 当前 tracks 打开可聚焦轨道列表，字幕面板额外提供“关闭字幕”。
- `PlaybackSource.queue` 有上一集/下一集时，OSD 按钮可直接切换；自然播放结束后自动播放下一集。
- OK/方向键唤起 OSD；Back 在 OSD 可见时先隐藏，再次 Back 退出播放页。
- 弹幕开关和播放暂停状态同步到 AkDanmaku。
- 播放开始、暂停/恢复、快退/快进、播放进度和退出播放页会通过 Emby Playback Check-ins 同步到服务器后台。

### 需求: TV 遥控器完整操作
**模块:** ui
所有可见操作必须能通过方向键、OK/Enter 和 Back 完成，未实现能力必须给出禁用态或明确提示。

#### 场景: 全局按键契约
用户使用遥控器时：
- 方向键优先交给当前聚焦控件处理，根容器不抢占 OSD 内部控件的方向键和 OK/Enter。
- 通用 `FocusableGlassSurface` 显式处理 `DirectionCenter`、`Enter` 和 `NumPadEnter` 的 KeyUp，聚焦后单次 OK/Enter 即触发可用入口或禁用原因提示。
- Back 在抽屉打开时关闭抽屉，在媒体库列表页返回首页，在播放 OSD 可见时隐藏 OSD，OSD 隐藏时退出播放页。
- 禁用入口可显示原因，OK/Enter 不允许空响应。
- 通用 `RemoteHint` 用 `RemoteHintMotionPolicy.TvFeedback` 执行不超过 120ms 进入、100ms 退出、10px 竖向位移的短时反馈动画，并保留上一条提示文本完成退出转场，避免禁用入口和错误提示在 TV 上突兀闪现。

#### 场景: 首页与抽屉
首页打开抽屉后：
- 抽屉请求初始焦点并形成焦点组。
- Back 或关闭按钮关闭抽屉，关闭后焦点返回菜单按钮。
- 媒体库卡片和抽屉媒体库项可用 OK/Enter 进入媒体库列表页；搜索、收藏、合集、播放列表、类型、演员入口可通过遥控器进入对应页面。

#### 场景: 搜索页
搜索页打开后：
- 搜索输入框获得初始焦点。
- 当搜索关键词为空且存在历史记录时，显示“最近搜索”历史记录面板。
- 历史记录以可聚焦 chip 展示，OK/Enter 可直接用该关键词再次搜索。
- 历史记录 chip 聚焦时显示删除入口；搜索页同时提供清空全部历史入口。
- 输入关键词后延迟触发 Emby 搜索，结果卡片展示图片、名字、类型和进度/角标。
- Movie/Series 进入详情，Episode 直接播放，暂不支持打开的类型显示提示。
- Back 返回首页。

#### 场景: 发现页
用户从抽屉进入合集、播放列表、类型或演员页后：
- 页面显示入口卡片、加载、空状态、错误状态和重试按钮。
- OK/Enter 进入入口详情资源列表。
- 资源列表复用媒体卡片交互：Movie/Series 进详情，Episode 可直接播放。
- Back 在入口详情时返回入口列表，再次 Back 返回首页。

#### 场景: 详情页遥控器路径
详情页打开后：
- Back 按层级处理：季内 Episode 列表返回季列表，详情页根层关闭详情。
- Movie 详情的播放按钮、Series 的季卡片、Episode 卡片和错误重试按钮均可聚焦并响应 OK/Enter。
- 详情页收藏/取消收藏、标记已播放/未播放和清除继续观看进度按钮可聚焦并响应 OK/Enter。
- 清除继续观看进度属于危险操作，按 OK/Enter 后必须先显示确认弹窗；取消按钮默认获取焦点，Back 或取消不会调用 Emby 写接口。
- 详情页缺失图片、人物或评分时显示兜底文案或省略对应字段，不出现空焦点操作。

#### 场景: 凭证危险操作确认
身份选择页中：
- 删除保存身份属于危险操作，按删除后必须先显示确认弹窗。
- 确认弹窗默认焦点在取消按钮；Back 或取消不删除凭证。
- 确认后才调用凭证删除逻辑。

#### 场景: 播放 OSD
OSD 显示后：
- 播放/暂停按钮获取初始焦点。
- 快退、快进、播放/暂停、弹幕开关支持 OK/Enter。
- 上一集、下一集在 `PlaybackQueue` 有数据时切换剧集；无数据时显示“没有上一集/下一集”。
- Audio 和 Subtitles 根据 Media3 当前 tracks 打开可聚焦轨道列表，字幕面板额外提供“关闭字幕”。
- Speed 快捷项打开速度面板，可切换 0.5x、0.75x、1x、1.25x、1.5x 和 2x。

## API接口
无外部 API。

## 数据模型
使用 `HomeUiState`、`SettingsUiState`、`ThemePreferences`、`SearchUiState`、`SearchHistoryItem`、`DiscoveryContentUiState`、`EmbyHomeDashboard`、`EmbyLibraryContent`、`EmbyFavoriteDashboard`、`EmbyMediaDetail`、`EmbySeasonSummary`、`EmbySeasonEpisodes`、`HomeDashboardUiModel`、`FavoriteContentUiState`、`MediaDetailUiState`、`DrawerUiState`、`LibraryContentUiState`、`MediaCardUiModel`、`PlayerOsdState`、`PlaybackSource`、`PlaybackDetails`、`PlaybackQueue` 和 `PlayerTrackOption`。

## 依赖
- data
- player
- danmaku
- Coil Compose
- Coil Network OkHttp: Coil 3 加载 HTTP/HTTPS Emby 图片 URL 必须显式引入 `io.coil-kt.coil3:coil-network-okhttp`，否则 `AsyncImage` 无网络 fetcher，会统一显示占位图。
- ZXing Core
- NanoHTTPD
- DataStore Preferences
- Kotlinx Serialization JSON

## 变更历史
- [202605291553_ui_interaction_optimization_phase3](../../history/2026-05/202605291553_ui_interaction_optimization_phase3/) - 完成 Phase 3 核心增强：主题偏好、设置页、播放速度、播放历史规则、可访问性语义和组件库文档。
- [202605291529_ui_interaction_optimization_phase2](../../history/2026-05/202605291529_ui_interaction_optimization_phase2/) - 完成 Phase 2 UI 体验增强：搜索历史、加载骨架屏、媒体库字母索引和版本 0.4.0。
- [202605291416_ui_interaction_optimization_phase1](../../history/2026-05/202605291416_ui_interaction_optimization_phase1/) - 完成 Phase 1 UI/交互体验优化：图片 crossfade 与占位、焦点渐变边框、状态面板、播放器缓冲进度和版本 0.3.0。
- [202605291035_emby_tv_feature_completion](../../history/2026-05/202605291035_emby_tv_feature_completion/) - 新增 TV 搜索页、发现页、详情页用户态动作、播放器音字幕面板和连续播放。
- [202605291303_emby_review_issue_fixes](../../history/2026-05/202605291303_emby_review_issue_fixes/) - 修复审查发现的认证图片、搜索取消、危险操作确认、播放上报和版本号一致性问题。
- [202605281948_performance_optimization](../../history/2026-05/202605281948_performance_optimization/) - 优化已保存 token 冷启动路径，避免先启动手机扫码同步服务。
- [20260528_media_detail_rich_info](../../history/2026-05/20260528_media_detail_rich_info/) - 媒体详情页补齐媒体信息、演员信息和更明显的播放/季列表入口。
- [202605281928_remote_ok_single_press_fix](../../history/2026-05/202605281928_remote_ok_single_press_fix/) - 在通用可聚焦面板统一处理 TV OK/Enter KeyUp，修复进入详情需按两次确认键的问题。
- [202605281915_coil_network_images_fix](../../history/2026-05/202605281915_coil_network_images_fix/) - 补齐 Coil 3 OkHttp 网络图片加载依赖，修复 Emby 封面全不显示。
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
