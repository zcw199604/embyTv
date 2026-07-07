# Changelog

本文件记录项目所有重要变更。
格式基于 Keep a Changelog，版本号遵循语义化版本。

## [Unreleased]

### 计划中

### 新增
- 新增 `PlayerManager`，通过 `StateFlow<PlayerOsdState>` 承载播放器 OSD、播放状态、seek 预览、弹幕设置和详情 Overlay 状态。
- 新增 `PlaybackEngineStatus.Ended` 播放结束态，Media3 自然结束时同步 OSD、暂停弹幕并保留停止上报/自动下一集副作用。
- 播放器 OSD 新增 5 秒无操作自动隐藏、隐藏态方向键快进/快退、播放中媒体详情 Overlay。
- 弹幕 OSD 面板新增透明度、字号和显示区域设置，并在 seek / Media3 位置跳变时同步 AkDanmaku 进度。
- 新增 `DanmakuOverlay`，将 AkDanmaku 视图绑定、透明度和配置应用从 `PlayerScreen` 中拆出。
- 新增播放器 OSD 中英文资源键，核心播放控制、轨道、弹幕和详情 Overlay 文案改为 `stringResource`。
- 新增语言偏好模型 `AppLanguage`，设置页可选择跟随系统、简体中文或英文，并通过 `ThemePreferenceStore.preferencesFlow` 被 ViewModel 监听。
- 新增 `PlaybackSource.previewThumbnailUrl`，seek 预览可复用当前媒体缩略图；Emby 外挂字幕可显示规范化语言标签。
- 新增 MainActivity 级 `LocalizedApp`，语言偏好变化时为 Compose 提供对应语言的 localized Context 和 Configuration。
- 新增 `PlayerTrackSelections`，集中封装 Media3 音轨/字幕选择和关闭字幕参数构造。
- 新增 `PlaybackSource.contextLabel`，播放器 OSD 顶部可显示 Episode 上下文（如 `S01E01`）。
- 新增 `PlaybackOverlayDetails` 和 `EmbyRepository.loadPlaybackOverlayDetails`，播放页详情 Overlay 可并行加载媒体元数据与 PlaybackInfo。
- 新增 `PlayerDetailOverlayState.playbackDetails`，详情 Overlay 加载成功后会把并行返回的 PlaybackInfo 播放详情保留到 OSD 状态中。
- 新增 `PlayerDetailOverlayLoadPolicy`，集中判断播放中详情 Overlay 的加载门禁，避免 OSD 可见期间播放状态细跳触发重复详情请求。
- 新增 `PlayerDetailOverlayVisibilityPolicy`，集中判断播放中详情 Overlay 在 OSD 可见或播放器暂停时的展示条件。
- 新增 `EmbyMediaDetail.criticRating` 与 `providerIds` 映射，详情 Overlay 可同时展示社区/IMDb 类评分、影评评分和 IMDb/Douban 外部标识。
- 新增 `PlayerPlaybackController` 与 `PlayerPlaybackEffect`，将 Media3 状态、错误、位置跳变、倍速和首帧事件映射为可测试的 OSD action 与播放副作用。
- 新增 `PlayerLifecyclePlaybackPolicy`，将播放页生命周期暂停/恢复时的 Media3、弹幕和 Emby pause check-in 决策抽成可测试策略。
- 新增 `PlayerPlaybackDetailsLabelResolver`，播放器 OSD 顶部播放模式、质量、音轨和字幕使用 UI 层本地化文案，避免跨语言环境显示 domain 层默认值。
- 新增 `OsdFocusVisualResolver`，统一播放器 OSD 控件 focused、selected、primary 和 disabled 视觉策略。
- 新增 `PlayerOsdFocusController`，让 OSD 首次显示、重新呼出和切换播放项时恢复主播放按钮焦点，同时避免面板内交互反复抢焦点。
- 新增 `PlayerQuickPanelFocusPolicy`，控制快捷面板打开或切换时把焦点送入首个可选项，避免遥控器用户停留在入口胶囊后再额外找焦点。
- 新增 `SeekThumbnail` 和 `PlaybackSource.seekThumbnails`，支持将 Emby `Chapters` 章节图片用于 seek 缩略图预览。
- 新增 `HomeThemePreferenceObserver`，让 ViewModel 层语言偏好监听有可测试的状态更新规则。
- 新增 `PlayerTrackSelectionsTest`，使用真实 Media3 `TrackSelectionParameters` 覆盖音轨选择、同类型轨道替换、关闭字幕、关闭后重新选择字幕、关闭字幕后切音轨不重新启用文字轨道，以及音轨/字幕 override 跨类型互不清理的参数契约。
- 新增 `PlayerTrackAvailabilityResolver`，统一 OSD 音轨/字幕入口可用性判断，避免 Emby PlaybackInfo 已有外挂字幕但 Media3 tracks 尚未上报时入口被误禁用。
- 新增 Emby 外挂字幕 `DeliveryUrl` 贯通，SRT/SubRip、WebVTT、ASS/SSA 外挂字幕会作为 Media3 `SubtitleConfiguration` 注入播放源。
- 新增 `PlayerMediaItemFactory` 与单元测试，集中构造 Media3 `MediaItem` 并验证外挂字幕筛选、MIME 映射、语言码规范化和默认/强制标记转换。
- 新增播放器外挂字幕防回归测试，覆盖默认与强制字幕标记组合，以及系统语言变化时技术编码标签仍保持稳定 ASCII 展示。
- 新增播放中详情 Overlay 并行加载契约测试，确保完整媒体详情未返回时 PlaybackInfo 请求已经启动。
- 新增播放队列上下文兜底：Episode 播放源可从同季 `Shows/{seriesId}/Episodes` 构建上一集/下一集，季末通过 `Shows/NextUp` 补下一集。
- 新增 `DanmakuPlaybackPolicy`、`DanmakuPlaybackCommand` 和 `DanmakuSyncCommand`，集中描述弹幕播放、暂停、配置应用和 seek 同步命令。
- 新增 `DanmakuPlaybackConfigKey`，让透明度单独变化不触发 akdanmaku 配置重启。
- 新增 `PlayerRemoteKeyPolicy` 和 `PlayerRemoteKeyCommand`，集中描述 TV 遥控器 Back、D-Pad、Enter 到 OSD action / seek / ignore 的映射。
- 新增 `PlayerQueueNavigationPolicy`，集中从 `PlaybackQueue` 派生上一集/下一集按钮状态和本地化禁用提示。
- 新增 `PlayerOsdAutoHidePolicy`，集中判断 OSD 5 秒自动隐藏是否应调度。
- 新增 `PlayerScreenOsdInteractionContractTest`，保护播放页自动隐藏 effect、预览按键层和普通按键层的 Compose wiring，避免快捷面板打开时旧计时器隐藏 OSD 或预览层吞掉焦点控件按键。
- 新增 `PlayerOsdMotionPolicy`，集中约束 OSD 显示/隐藏动画时长和位移，避免弱 TV 设备上使用过重转场。
- 新增 `PlayerQuickPanelLayoutPolicy`，集中约束 OSD 倍速、音轨和字幕快捷面板在 TV 上按短行展示。
- 新增 `DanmakuQuickPanelLayoutPolicy`，集中约束弹幕快捷设置在 TV OSD 中按开关、透明度、字号和显示区域分行展示。
- 新增 `PlayerTrackOptionMapper`，将 Media3 内嵌音轨/字幕转换为 OSD 可选菜单项，过滤不支持的单条轨道并补齐友好标签兜底。
- 新增 `PlayerTrackSummaryLabelResolver`，让 OSD 音轨/字幕快捷胶囊优先显示当前 Media3 选中轨道，关闭字幕时显示本地化无字幕标签。
- 新增 `PlayerDetailProviderIdsLabelResolver`，集中生成详情 Overlay 的 IMDb/Douban provider 标识文案并提供顺序、大小写和空值规则测试。
- 新增 `PlayerDetailCastLabelResolver`，集中生成详情 Overlay 的演员摘要并过滤非 Actor 人员。
- 新增 `PlayerScreenDetailOverlayContractTest`，保护详情 Overlay 加载、成功和失败 action 必须携带当前 `PlaybackSource.itemId`，避免切集后旧媒体详情或错误状态阻止新媒体元数据加载。
- 新增 `FocusableGlassSurfacePolicy`，集中描述 TV 可聚焦玻璃容器的可聚焦、内容聚焦和缩放聚焦规则。
- 新增 `RemoteHintMotionPolicy`，集中约束遥控器反馈提示的短时 fade/slide 动画，避免禁用入口和错误提示突兀闪现。
- 新增 `PlayerStringResourceParityTest`，校验播放器和语言设置相关资源键在默认中文与英文资源中保持一致。
- 新增 `PlayerSourceI18nContractTest`，扫描播放器生产源码，防止 OSD 可见文案重新引入硬编码中文。
- 新增 `PlayerTechnicalLocaleContractTest`，扫描播放器技术标签和字幕语言标签路径，防止重新引入未显式指定稳定 Locale 的大小写转换。
- 新增 `PlaybackSource.startPositionMs` 和 `PlayerStartupPositionPolicy`，播放器可从 Emby 继续观看位置启动。
- 新增播放器 OSD 焦点 wiring 契约测试，覆盖主播放按钮、快捷面板首项焦点请求器，以及上一集/下一集禁用提示传递。
- 新增 `scripts/player-runtime-preflight.ps1` 播放器运行预检脚本，自动检查 JDK、Android SDK、播放器 JVM 测试、Debug APK、ADB 设备和 AVD 就绪状态；连接 Android TV 或模拟器后可选安装 Debug APK、启动播放器应用并按应用进程采集短时 logcat，自动扫描启动崩溃、ANR、Media3/ExoPlayer 关键错误。
- 新增 `PlayerPlaybackDiagnostics`，在 Debug 构建中为 Emby Playback Check-ins 输出不含 URL、token、密码和设备标识值的 logcat 诊断日志，便于真机验证播放开始、进度、暂停/seek 和停止上报链路。
- 播放器运行预检脚本新增 `-RequirePlaybackReports` 和 `-RequiredPlaybackEvents`，可在真机 logcat 中强制校验 `EmbyTvPlaybackReport` 的 Started、Progress、Stopped 成功诊断。
- 新增 Repository 层边界回归测试，覆盖 Emby 续播位置和章节缩略图 `ticks -> ms` 转换的负数归零、超大值安全除法以及空白章节图过滤。

### 变更
- `PlayerMediaItemFactory.create` 构造的 Media3 `MediaItem` 现在写入 `PlaybackSource.itemId` 作为 `mediaId`，并把 `PlaybackSource.title` 写入 `MediaMetadata.title`，便于 Media3 回调、日志和后续会话集成稳定识别当前 Emby 条目。
- Emby 服务器配置默认值改为更适合局域网直连的 HTTP `8096`，HTTPS 切换默认端口改为 `8920`；TV 表单和手机扫码同步页保持一致，避免用户名密码正确但请求打到错误协议/端口导致登录失败。
- 首页 Dashboard 聚合在登录成功后对 `Resume`、`Latest`、`NextUp`、按库 latest 和媒体库数量统计做局部容错，单个扩展接口失败时对应分区降级为空，不再把已成功的 Emby 认证显示为连接失败。
- TV 手动登录失败提示现在会显示 HTTP 状态、实际请求地址和实际提交用户名，并将 HTTP 401 明确标记为用户名或密码错误；路径输入占位文案改为留空，避免误认为存在默认路径。
- Emby 媒体库、详情、播放信息和队列映射现在兼容服务端返回 `null` 集合字段，避免进入媒体库时因 `Items`、图片标签、人员、类型、章节或媒体流列表为空而显示 `Iterable.iterator()` 空指针错误。
- 首页默认布局重构为更接近 TV 媒体中心的密集浏览样式：顶部居中标题与右侧工具入口、媒体库横向宽卡、继续观看 16:9 横排卡片，以及按媒体库分组的海报 shelf 和“更多”入口。
- 播放中详情 Overlay 状态新增当前播放项归属，`PlayerDetailOverlayLoadPolicy` 只用同一 `itemId` 的 loading、loaded 或 error 阻止重复请求；切换上一集/下一集后会重新加载当前媒体详情与 PlaybackInfo。
- `PlayerOsdReducer` 现在会忽略不同 `itemId` 的详情 Overlay 成功/失败晚到结果，避免切集后旧请求覆盖当前媒体详情、技术标签或错误反馈。
- 播放页 Emby 上报失败现在由 `PlayerScreen` 捕获并记录为 Debug 诊断日志，避免上报异常阻塞 Back 退出或播放器释放。
- `PlaybackReportingCoordinator` 对非正数 Progress 节流间隔回退到默认 10 秒，避免测试桩或未来配置误把 250ms UI 轮询放大为高频 Emby Check-ins 请求。
- `PlaybackReportingCoordinator` 的周期 Progress Tick 现在会在暂停/恢复状态变化时立即上报，不再等待 10 秒位置节流，避免 Emby 后台会话暂停态滞后。
- `PlayerQueueNavigationPolicy` 现在会过滤与当前媒体 `id` 相同的上一集/下一集目标，避免异常队列或 NextUp 数据把 OSD 按钮、自动下一集指回当前条目。
- 播放器 OSD 快捷面板关闭后会把焦点恢复到主播放按钮，避免 Back 关闭音轨/字幕/弹幕面板后焦点停留在已移除的面板选项上。
- 播放页 `PlayerManager` 的 Compose 生命周期改为按完整 `PlaybackSource` 和续播起点重建，确保同一媒体切换播放源、流地址、播放会话或弹幕数据时清理旧 OSD 瞬态状态。
- 播放器知识库新增 Android TV / 真实 Emby Server 运行验收清单，覆盖 OSD 遥控器交互、seek 缩略图、上一集/下一集、轨道字幕、弹幕同步、Details Overlay、多语言和错误态验证。
- 播放页从本地 Compose `mutableStateOf` 切换为 `PlayerManager` 状态流驱动，Media3 状态变化映射为 `Loading`、`Playing`、`Paused`、`Buffering`、`Ended`、`Error`。
- `PlaybackDetails` 音轨/字幕摘要改为使用 `PlaybackTrack.label`，外部字幕优先展示语言、格式和 External 标记，并保留可交给 Media3 的 `externalUrl`。
- 连续方向键 seek 预览会从上一次预览目标继续累加，OSD 标签改为显示从本轮 seek 原始位置到当前目标的实际可达累计偏移，并保留上一张缩略图。
- 方向键 seek 预览的缩略图选择从 `PlayerScreen` 下沉到 `PlayerManager.requestSeekPreview()`，由可替换的 thumbnail provider 提供图片 URL。
- `PlayerManager.requestSeekPreview()` 现在会把 thumbnail provider 返回的空白字符串视为无缩略图，连续 seek 时保留上一张有效缩略图，避免 OSD 图片区域被空白 URL 覆盖。
- seek 预览章节图、兜底缩略图和 thumbnail provider 返回值进入 OSD 前会先 trim，避免服务端返回首尾空白 URL 时图片加载失败或缓存 key 不稳定。
- seek 提交后保留缩略图预览到 OSD 隐藏，避免方向键快进/快退时缩略图与 `+10s/-10s` 标签被同帧清除。
- seek 缩略图 provider 改为优先按目标时间选择最近章节图，缺失章节图时再使用当前媒体 Thumb/Backdrop/Primary 兜底图。
- `PlaybackDetails.qualityLabel` 增加码率展示，例如 `2160p · HDR10 · 4.0 Mbps`。
- 播放页 OSD 详情加载改为调用并行 Overlay 详情入口。
- 播放器 OSD 顶部技术标签、轨道可用性和音轨/字幕快捷胶囊摘要优先使用详情 Overlay 最新保留的 `PlaybackDetails`，再回退到初始 `PlaybackSource.details`。
- 播放器暂停后即使 OSD 因 5 秒无操作自动隐藏，播放中详情 Overlay 仍会在播放画面根层保持展示，符合暂停态查看媒体详情的 TV 体验。
- 播放自然结束后即使 OSD 被隐藏，播放中详情 Overlay 仍会保持可见，方便用户在结束态确认媒体信息或下一步操作。
- 播放页生命周期暂停/恢复现在通过 `PlayerLifecyclePlaybackPolicy` 消费最新 `PlayerOsdState` 快照，用户手动暂停后切后台再返回不会自动恢复 Media3 播放或弹幕。
- 播放中详情 Overlay 从固定 `420.dp` 宽度调整为填充可用宽度但最大 `420.dp`，降低不同 TV 安全区或窄视口下的裁切风险。
- 媒体详情接口字段扩展为请求 `ProviderIds`，并过滤空键值后传递给播放中详情 Overlay。
- 播放中详情 Overlay 的 IMDb/Douban provider 标识展示从 `PlayerScreen` 私有函数抽出为可测试策略，固定 IMDb 在前、Douban 在后并忽略 provider key 大小写。
- 播放中详情 Overlay 的演员摘要改为只展示 Emby `People.Type=Actor`，并保留角色名，避免导演或编剧被误放入演员行。
- 播放中详情 Overlay 的演员角色连接词改为由中英文 `stringResource` 注入，避免英文界面混入中文“饰”。
- 播放中详情 Overlay 的家长/官方分级改为通过本地化资源显示为带语义前缀的标签，例如中文“分级 PG-13”、英文 `Rated PG-13`，避免裸显 `OfficialRating` 值。
- 播放页进度刷新从 1 秒循环调整为 `PlayerPlaybackController.UI_PROGRESS_INTERVAL_MS` 250ms 受控采样，Emby progress 上报继续由 `PlaybackReportingCoordinator` 10 秒节流。
- 播放器技术标签、内嵌轨道兜底标签、外挂字幕 MIME/语言标签规范化的大小写转换统一显式使用 `Locale.US`，避免系统语言影响 OSD 与 Media3 字幕配置。
- 外挂字幕 Media3 注入和 OSD 标签在 Emby `Codec` 缺失或为空时会回退解析 `DeliveryUrl` 文件后缀，继续识别 SRT/VTT/ASS/SSA 格式。
- 外挂字幕注入 Media3 前会先 trim Emby `DeliveryUrl`，避免服务端或插件返回首尾空白时 MIME 后缀推断和 `SubtitleConfiguration` URL 携带非法空白。
- 播放页自动隐藏从硬编码 `delay(5000)` 调整为策略驱动；Playing / Paused 可交互态 5 秒无操作隐藏，Loading / Buffering / Ended / Error 不自动隐藏。
- 播放器 OSD 可见性从直接条件渲染调整为 `AnimatedVisibility`，使用短时 fade + 小幅竖向位移动画，降低突兀感同时控制 TV 端合成压力。
- 播放页自动隐藏策略新增快捷面板保护，音轨、字幕、倍速或弹幕设置面板打开时不调度 5 秒隐藏。
- 播放页自动隐藏 effect 现在监听快捷面板状态变化，打开面板会取消旧的隐藏计时并重新按策略判断。
- 播放器 OSD 图标按钮、快捷设置胶囊、轨道与弹幕选项按钮统一接入焦点视觉策略，聚焦、选中和禁用状态都有明确遥控器反馈。
- 播放器 OSD 倍速、音轨和字幕快捷面板从单行按钮调整为策略驱动多行布局，单行最多 3 个选项，避免长语言标签或多轨媒体在 1080p TV 上挤压。
- 播放器 OSD 快捷面板打开、在音轨/字幕/倍速/弹幕面板之间切换，或同一面板从无选项变为有选项时，会请求首个面板选项获得焦点；同一面板内普通交互不重复抢焦点。
- 播放器 OSD 主焦点恢复 effect 现在显式监听快捷面板状态，确保面板关闭后能按 `PlayerOsdFocusController` 策略把焦点恢复到主播放按钮。
- 播放器 OSD 音轨和字幕面板不再硬限制前 6 条轨道；多语言媒体的完整可支持轨道列表按短行展示，并在超出可视高度时滚动。
- 播放器 OSD 音轨/字幕快捷胶囊摘要从 PlaybackInfo 默认轨道改为优先读取 OSD 当前选中轨道，用户切换音轨或字幕后入口值会立即跟随当前选择。
- 播放器 OSD 打开快捷面板时按 Back 会先关闭当前面板并保留 OSD，可再次按 Back 隐藏 OSD，避免用户调整字幕/音轨时误退出控制层。
- 播放器 OSD 快捷胶囊支持同入口开关：重复按当前已选中的音轨、字幕、倍速或弹幕胶囊会关闭面板，切换到其他胶囊仍打开目标面板。
- 播放器 OSD 打开、关闭或切换快捷面板时会清理旧 seek 缩略图预览，避免 seek 预览与音轨/字幕/弹幕设置面板叠加。
- 播放器 OSD 在用户选择音轨/字幕、关闭字幕、选择倍速或调整弹幕设置时会同步清理旧 seek 预览和过期反馈，避免设置面板残留上一轮快进/快退提示。
- `FocusableGlassSurface` 的禁用但带提示状态现在会继续把 focused 传给内容层，用于 OSD 上一集/下一集等禁用按钮展示降权焦点反馈，同时禁用状态不触发缩放动画。
- `RemoteHint` 从直接条件渲染改为短时 `AnimatedVisibility`，保留上一条提示文本完成退出动画，用于播放器和首页遥控器反馈。
- 播放页遥控器按键处理从 Compose 事件分支抽取为策略层；OSD 可见时方向键和确认键保留给焦点控件，OSD 隐藏时中心键/Enter/上/下呼出菜单、左/右执行 10 秒 seek。
- 播放器上一集/下一集按钮无目标时改为禁用视觉但保持可聚焦，按 OK 显示“没有上一集/没有下一集”反馈而不是执行空切换。
- 播放器上一集/下一集按钮的可用性、禁用提示和目标媒体统一由 `PlayerQueueNavigationPolicy` 输出，`PlayerScreen` 切集点击从策略状态读取 target，避免 UI 重复读取队列造成状态分散。
- 自然结束自动下一集目标也改由 `PlayerQueueNavigationPolicy` 输出，`autoPlayNext=false` 时仍保留手动下一集按钮，但不会自动切到下一集。
- 弹幕快捷胶囊改为打开弹幕设置面板，开/关改由面板内显式按钮控制，避免进入设置时误切换弹幕状态。
- 弹幕快捷设置面板从单行 9 按钮调整为策略驱动的多行布局，单行最多 3 个选项，降低 1080p TV 上文本拥挤和遥控器横向导航压力。
- 弹幕设置变更时通过策略命令更新 AkDanmaku 配置并启动，Seek 或 Media3 位置跳变时先停止旧弹幕帧、再跳转到目标毫秒位置并按当前 OSD 状态恢复。
- 弹幕持续配置同步收敛到 `DanmakuOverlay`，`danmakuEnabled`、`danmakuPaused`、字号和显示区域变化由 Overlay 单点应用到 AkDanmaku；`PlayerScreen` 仅保留切源、seek 和生命周期恢复相关的事件驱动同步。
- 弹幕透明度变更改为只更新 Compose alpha；字号和显示区域变化才触发 akdanmaku `updateConfig/start`，降低弱 TV 设备上的重启开销。
- 播放源切换时弹幕加载顺序调整为停止旧弹幕、更新新媒体数据、seek 到 0、再按当前设置启动，避免短暂显示上一媒体弹幕。
- Repository 内部加载的 Episode 播放队列按季号、集号排序，避免 Emby 返回乱序时 OSD 上一集/下一集错位。
- app 模块 JVM 单测启用 Android 默认值返回，以支持 Media3 `TrackGroup` 等轻量 Android API 对象的参数契约测试。
- 播放器 OSD 字幕关闭和倍速切换反馈改为由 Compose `stringResource` 传入 `PlayerOsdAction`，状态机不再硬编码中文反馈文案。
- 播放器错误和详情 Overlay 加载失败 fallback 文案改为由 `PlayerScreen` 通过 `stringResource` 提供，播放控制器不再硬编码中文错误提示。
- Emby PlaybackInfo 返回的轨道 `DisplayTitle` 进入 `PlaybackTrack.label` 前会 trim 首尾空白，避免 OSD 顶部音轨/字幕摘要显示脏文本。
- Media3 轨道回调生成 OSD 菜单项时，缺少 `Format.label` 的音轨/字幕改为显示友好语言、编码、声道或字幕格式，避免显示原始语言码和 MIME 片段。
- Media3 轨道回调携带的 `Format.label` 进入 OSD 菜单前会 trim 首尾空白，避免服务端或封装元数据脏值导致音轨/字幕按钮文本不齐。
- 播放页启动媒体时改为共用 `startPositionMs` 驱动 Media3 seek、AkDanmaku seek 和 `Sessions/Playing` 开始上报，避免续播时画面、弹幕和 Emby 会话起点不一致。
- 播放列表来源媒体的 `PlaylistItemId` 现在会从 `MediaItemSummary` 贯通到 `PlaybackSource`，并随 `Sessions/Playing`、`Sessions/Playing/Progress` 和 `Sessions/Playing/Stopped` 上报。
- Emby Playback Check-ins 的毫秒到 ticks 转换改为饱和转换，极端大播放位置会写入 `Long.MAX_VALUE` 而不是溢出为负 ticks。

### 修复
- 修复未知总时长下极端大播放位置继续右键 seek 可能发生 Long 溢出并回绕到 0 的问题，seek 目标现在使用饱和加法。
- 修复异常 OSD seek origin 超出归一化时间轴时累计 seek 标签可能因 Long 减法溢出而翻转方向的问题，标签现在使用饱和差值生成。
- 修复 `PlayerManager.requestSeekPreview()` 为缩略图 provider 预计算目标位置时仍可能 Long 溢出的问题，缩略图选择现在与实际 seek 饱和目标一致。
- 修复 Media3 或测试桩回调 `NaN` / 无穷倍速时可能污染 OSD 倍速状态的问题，非有限倍速现在回退为 1.0x。
- 修复弹幕透明度或字号比例为 `NaN` 时可能污染 Compose alpha 或 AkDanmaku 配置的问题，非有限弹幕设置现在回退到默认值后再限幅。
- 修复 seek 缩略图兜底 URL 为空白字符串或带首尾空白时仍会传给 OSD 的问题，`PlaybackSource.previewThumbnailFor()` 现在只返回 trim 后的非空白章节图或非空白兜底图。
- 修复 OSD 进度刷新接收到超过总时长的位置、`NaN`、无穷或小于播放进度的缓冲比例时可能写入不稳定进度状态的问题，状态机现在会把当前位置限制到总时长，并让缓冲比例回退到当前播放进度以保持缓冲层不短于播放层。
- 修复同一 `PlayerScreen` 实例切换 `PlaybackSource` 时旧 Emby 播放会话可能没有发送 `Sessions/Playing/Stopped` 的问题，切源时会先结束旧 reporting coordinator。
- 修复播放器 OSD 关闭字幕后本地字幕轨道仍显示选中态的问题，避免 Media3 轨道回调到达前“关闭字幕”和具体字幕同时高亮。
- 修复 OSD 主焦点恢复 effect 未直接监听快捷面板状态的问题，避免未来出现只关闭面板但不触发交互版本变化时焦点停在已移除面板选项上。
- 修复关闭字幕后重新选择字幕时 OSD 仍短暂保持“字幕关闭”高亮的问题；轨道选择现通过状态机即时更新本地选中态。
- 修复 Back 隐藏 OSD 时未清理 seek 缩略图预览的问题，避免下次呼出 OSD 时短暂显示旧的 seek 目标。
- 修复 OSD 隐藏后再次呼出仍可能显示旧反馈消息的问题，自动隐藏和 Back 隐藏现在会清理瞬态反馈。
- 修复播放中进入 Buffering 时状态机把 `isPlaying` 置为 false 的问题，避免 Compose 副作用误调用 `player.pause()`；缓冲期间仅暂停弹幕。
- 修复播放器生命周期恢复时 observer 可能读取旧 OSD 状态的问题，避免用户暂停后切后台再返回时被误恢复播放或弹幕。
- 修复 Media3 `onIsPlayingChanged(false)` 在缓冲期间可能把 OSD 覆盖为 `Paused` 的问题，控制器现在结合 `player.playbackState` 保持 `Buffering`。
- 修复播放错误发生时仍保留快捷面板和旧 seek 缩略图预览的问题，错误态现在会关闭面板并清理 seek 预览，让错误反馈成为当前唯一瞬态焦点。
- 修复 Media3 自然结束后后续 `onIsPlayingChanged(false)` 可能把 OSD `Ended` 覆盖为 `Paused` 的问题，确保播放结束态、停止上报和自动下一集体验保持一致。
- 修复播放自然结束时仍保留快捷面板、旧 seek 缩略图预览和上一条反馈的问题，`Ended` 现在会收敛为干净的结束态控制层。
- 修复 Media3 首帧已渲染但 `isPlaying=false` 时 OSD 仍可能停留在 `Loading` 的问题，控制器现在会将该状态收敛为 `Paused`。
- 修复 OSD 顶部码率标签受系统 Locale 影响的问题，技术信息现在稳定显示 `4.0 Mbps` 这类小数点格式。
- 修复部分外部字幕源只返回 `DeliveryUrl` 后缀、不返回有效 `Codec` 时被跳过的问题，避免真实 Emby/插件字幕无法进入 Media3 字幕轨道。
- 修复 `PlaybackDetails.bitrateLabel` 和 `qualityLabel` 在非英文 Locale 下可能输出 `4,0 Mbps` 的问题，domain 派生技术标签现在同样固定使用英文小数点。
- 修复 OSD 时间轴在阿拉伯等 Locale 下可能显示本地数字的问题，播放时间和剩余时间现在固定使用西文数字格式。
- 修复 OSD 快捷胶囊标签大写转换受系统 Locale 影响的问题，音轨/字幕/倍速/弹幕入口标题现在固定使用 `Locale.US` 大写。
- 修复 Details Overlay 评分在非英文 Locale 下可能显示本地小数分隔符的问题，社区/IMDb 类评分和影评评分现在固定使用英文数字格式。
- 修复详情 Overlay 家长/官方分级裸显 `PG-13` 等原始值时语义不清的问题。
- 修复打开音轨、字幕、倍速或弹幕快捷面板后仍可能被旧 5 秒自动隐藏协程关闭的问题。
- 修复 Media3 底层倍速回调可能自动呼出 OSD 并打开速度面板的问题；无反馈文案的倍速同步现在只更新状态，不打断观看。
- 修复播放中详情 Overlay 加载失败后再次呼出 OSD 会自动重复请求的问题，失败信息保留在当前播放项内，避免弱网场景反复加载和闪烁。
- 修复播放中详情 Overlay 加载失败只保存在 Overlay 内部状态的问题，现在同一错误文案也会进入 OSD `RemoteHint` 反馈，弱网失败时用户无需移开焦点即可看到错误提示。
- 修复 Media3 返回空白错误消息时 OSD 错误态没有可见文案的问题，播放器控制器现在会回退到 Compose 传入的本地化错误提示。
- 修复关闭字幕时 Media3 text track override 未清理的问题，避免后续重新启用文字轨道时旧字幕被意外恢复。
- 修复关闭字幕后 Media3 延迟 `TracksChanged` 回调可能恢复旧字幕选中态的问题，OSD 会保留字幕关闭意图直到用户显式重新选择字幕。
- 修复关闭字幕后 OSD 字幕快捷胶囊仍可能显示 PlaybackInfo 默认字幕的问题，关闭态现在显示本地化无字幕标签。
- 修复 Emby 外挂字幕语言码带下划线时未规范化的问题，`zh_Hans`、`zh_CN`、`en_US` 等现在会转换为 Media3 更易匹配的语言标签。
- 修复 Emby 外挂字幕中文语言码同时包含脚本和地区时未归一的问题，`zh_Hans_CN` 现在会注入为 `zh-CN`，`zh_Hant_HK` 现在会注入为 `zh-TW`。
- 修复 Emby PlaybackInfo 轨道摘要中下划线语言码直接暴露到 OSD 的问题，`PlaybackTrack.label` 现在会将 `zh_Hans` 等归一为友好语言名。
- 修复 Emby PlaybackInfo 和 Media3 轨道语言码带地区变体时直接暴露到 OSD 的问题，`en_US` / `en-US` 现在会折叠显示为 `English`。
- 修复 Emby 外挂字幕绝对 `DeliveryUrl` 已带大小写不同的 `api_key` 参数时仍重复追加 token 的问题，token 检测现在大小写不敏感。
- 修复 Emby 外挂字幕 `DeliveryUrl` 带 `not_api_key` 等相似 query 参数时被误判为已有 token 的问题，现在只按 query 参数名精确识别 `api_key`。
- 修复 Emby 外挂字幕 codec 为 `subrip` 或 `webvtt` 时 OSD 直接显示 `SUBRIP` / `WEBVTT` 的问题，现在统一展示为 `SRT` / `VTT`。
- 修复固定语言偏好下系统配置变化后 Compose 可能继续持有旧 localized Context 的问题，`LocalizedApp` 现在会随 `LocalConfiguration` 变化重建本地化上下文。
- 修复一次遥控器 seek 可能由即时路径和 Media3 `onPositionDiscontinuity` 重复触发 Emby Progress check-in 的问题，相同位置和暂停状态的 seek 上报现在会去重。
- 修复 Media3 非 seek 位置跳变被误当成用户 seek 上报的问题；`onPositionDiscontinuity` 现在仅对 `SEEK` 和 `SEEK_ADJUSTMENT` 发送 Emby seek Progress，自动切集等非 seek 跳变只同步弹幕时间轴。
- 修复播放页初始化/释放阶段 Progress、暂停、seek 或 Stopped 上报可能早于 `Sessions/Playing` 的问题，`PlaybackReportingCoordinator` 现在在 Started 前忽略这些事件。
- 修复续播媒体开始播放时仍按 0ms 上报 Started 且弹幕从 0ms 初始化的问题。
- 修复多语言媒体中第 7 条及之后音轨/字幕无法通过 OSD 选择的问题。
- 修复详情 Overlay 可能把导演、编剧等非 Actor 人员展示到演员摘要中的问题。
- 修复详情 Overlay 英文界面演员角色摘要仍使用中文连接词的问题。
- 修复弹幕设置变化时 `PlayerScreen` 与 `DanmakuOverlay` 可能重复执行 AkDanmaku `updateConfig/start/pause` 的问题。

### 验证
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.PlayerMediaItemFactoryTest.createCarriesStableMediaIdAndTitleForMedia3Callbacks` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.PlayerOsdFocusControllerTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.PlayerScreenStartupPositionContractTest.playerManagerResetsForAnyPlaybackSourceChange` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.PlayerOsdReducerTest.resetsNonFiniteBufferedProgressForStableUi` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.PlayerOsdReducerTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.PlayerOsdReducerTest.seekPreviewLabelDoesNotWrapWhenOriginIsOutsideNormalizedTimeline --tests com.embytv.ui.player.PlayerOsdReducerTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.PlayerOsdReducerTest --tests "com.embytv.ui.player.*"` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.PlayerManagerTest --tests com.embytv.ui.player.PlayerOsdReducerTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.DanmakuPlaybackPolicyTest --tests com.embytv.ui.player.PlayerOsdReducerTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` 通过。
- `.\scripts\player-runtime-preflight.ps1` 通过；当前机器 ADB 设备与 AVD 仍为 WARN。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.domain.model.PlaybackDetailsTest --tests com.embytv.ui.player.PlayerTrackOptionMapperTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.domain.model.PlaybackDetailsTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.PlayerMediaItemFactoryTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.PlaybackReportingCoordinatorTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.PlayerPlaybackControllerTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.PlayerClockLabelTest --tests "com.embytv.ui.player.*"` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.PlayerOsdReducerTest --tests com.embytv.ui.player.PlayerDetailOverlayLoadPolicyTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.PlayerOsdReducerTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.PlayerDetailRatingLabelsTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.PlayerStringResourceParityTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.PlayerScreenTrackPanelContractTest.primaryFocusEffectObservesQuickPanelChangesForRestore` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.PlayerScreenTrackPanelContractTest.detailOverlayUsesResponsiveMaxWidthInsteadOfFixedWidth` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.PlayerScreenTrackPanelContractTest --tests com.embytv.ui.player.PlayerDetailOverlayLoadPolicyTest --tests com.embytv.ui.player.PlayerDetailProviderIdsLabelTest --tests com.embytv.ui.player.PlayerDetailCastLabelTest --tests com.embytv.ui.player.PlayerDetailRatingLabelsTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.PlayerOsdFocusControllerTest --tests com.embytv.ui.player.PlayerQuickPanelFocusPolicyTest --tests com.embytv.ui.player.PlayerScreenTrackPanelContractTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.PlayerOsdReducerTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.PlayerTrackOptionMapperTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.PlayerMediaItemFactoryTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.data.repository.EmbyRepositoryMediaDetailTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.DanmakuPlaybackPolicyTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.PlayerScreenDanmakuSyncContractTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.PlayerScreenDanmakuSyncContractTest --tests com.embytv.ui.player.DanmakuPlaybackPolicyTest --tests com.embytv.ui.player.PlayerPlaybackControllerTest` 通过，覆盖弹幕持续配置同步、seek 后 stop/seekTo/恢复顺序和 Media3 位置跳变同步弹幕路径。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.PlayerStartupPositionPolicyTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.PlayerScreenStartupPositionContractTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.data.repository.EmbyRepositoryMediaDetailTest.createPlaybackSourceCarriesResumePositionFromUserData` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.PlayerQuickPanelFocusPolicyTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.PlayerScreenTrackPanelContractTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.PlayerDetailCastLabelTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.PlayerRemoteKeyPolicyTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.PlayerQueueNavigationPolicyTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.PlayerScreenOsdInteractionContractTest --tests com.embytv.ui.player.PlayerRemoteKeyPolicyTest --tests com.embytv.ui.player.PlayerOsdAutoHidePolicyTest` 通过，覆盖自动隐藏 effect 监听快捷面板状态、预览按键层只抢 seek/Back、普通按键层按当前 OSD 可见性走遥控器策略。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.PlayerDetailProviderIdsLabelTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.components.FocusableGlassSurfacePolicyTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.components.RemoteHintMotionPolicyTest --tests com.embytv.ui.components.FocusableGlassSurfacePolicyTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.DanmakuQuickPanelLayoutPolicyTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.PlayerQuickPanelLayoutPolicyTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.PlayerQuickPanelFocusPolicyTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.PlayerOsdMotionPolicyTest --tests "com.embytv.ui.player.*"` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.PlayerTrackSelectionsTest --tests com.embytv.ui.player.PlayerScreenTrackPanelContractTest` 通过，覆盖同类型音轨/字幕切换会替换旧 override、关闭字幕后切音轨不重新启用 text track，以及播放页轨道选择/关闭字幕必须走 Media3 helper 并同步 OSD 状态。
- `.\gradlew.bat :app:testDebugUnitTest --tests "com.embytv.ui.player.*"` 通过。
- `.\gradlew.bat :app:testDebugUnitTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` 通过。
- `powershell -ExecutionPolicy Bypass -File .\scripts\player-runtime-preflight.ps1` 通过；当前机器播放器 JVM 测试、Debug 构建和 APK 检查为 OK，ADB 设备与 AVD 为 WARN。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.MainActivityLocalizationContractTest --tests com.embytv.ui.theme.ThemePreferenceRulesTest --tests com.embytv.ui.player.PlayerStringResourceParityTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.data.repository.EmbyStreamUrlBuilderTest --tests com.embytv.ui.player.PlayerMediaItemFactoryTest --tests com.embytv.data.repository.EmbyRepositoryDashboardTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.PlayerSourceI18nContractTest --tests com.embytv.ui.player.PlayerStringResourceParityTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.PlayerScreenTrackPanelContractTest --tests com.embytv.ui.player.PlayerSourceI18nContractTest --tests com.embytv.ui.player.PlayerStringResourceParityTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.domain.model.PlaybackDetailsTest.playbackTrackDisplayTitlesAreTrimmedBeforeOsdLabels` 先失败后通过，覆盖 Emby PlaybackInfo 轨道 `DisplayTitle` 首尾空白进入 OSD 前会被清理。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.domain.model.PlaybackDetailsTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests "com.embytv.ui.player.*" --tests com.embytv.domain.model.PlaybackDetailsTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` 通过。
- `powershell -ExecutionPolicy Bypass -File .\scripts\player-runtime-preflight.ps1` 通过；当前机器播放器 JVM 测试、Debug 构建和 APK 检查为 OK，ADB 设备与 AVD 为 WARN。
- `git diff --check -- app/src/main/java/com/embytv/domain/model/PlaybackSource.kt app/src/test/java/com/embytv/domain/model/PlaybackDetailsTest.kt helloagents/main/CHANGELOG.md helloagents/main/wiki/modules/player.md helloagents/main/wiki/data.md` 通过（仅输出 LF/CRLF 工作区换行提示）。
- `rg -n 'api_key\s*=\s*"|accessToken\s*=\s*"|password\s*=\s*"|eval\(|exec\(' app\src\main\java\com\embytv\ui app\src\main\java\com\embytv\data scripts` 未发现新增明文密钥或危险执行模式。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.PlayerTrackOptionMapperTest.media3TrackLabelsAreTrimmedBeforeOsdDisplay` 先失败后通过，覆盖 Media3 原始轨道标签首尾空白进入 OSD 前会被清理。
- `.\gradlew.bat :app:testDebugUnitTest --tests com.embytv.ui.player.PlayerTrackOptionMapperTest` 通过。
- `.\gradlew.bat :app:testDebugUnitTest --tests "com.embytv.ui.player.*"` 通过。
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` 通过。
- `powershell -ExecutionPolicy Bypass -File .\scripts\player-runtime-preflight.ps1` 通过；当前机器播放器 JVM 测试、Debug 构建和 APK 检查为 OK，ADB 设备与 AVD 为 WARN。
- `git diff --check -- app/src/main/java/com/embytv/ui/player/PlayerTrackOptionMapper.kt app/src/test/java/com/embytv/ui/player/PlayerTrackOptionMapperTest.kt helloagents/main/CHANGELOG.md helloagents/main/wiki/modules/player.md helloagents/main/wiki/data.md` 通过（仅输出 LF/CRLF 工作区换行提示）。
- `rg -n 'api_key\s*=\s*"|accessToken\s*=\s*"|password\s*=\s*"|eval\(|exec\(' app\src\main\java\com\embytv\ui app\src\main\java\com\embytv\data scripts` 未发现新增明文密钥或危险执行模式。
- `.\gradlew.bat :app:assembleDebug` 通过。
- `git diff --check` 通过（仅输出 LF/CRLF 工作区换行提示）。
- `rg -n 'api_key\s*=\s*"|accessToken\s*=\s*"|password\s*=\s*"|eval\(|exec\(' app\src\main\java\com\embytv\ui app\src\main\java\com\embytv\data` 未发现新增明文密钥或危险执行模式；唯一命中为 `EmbyStreamUrlBuilder` 对 URL 中 `api_key=` 的存在性判断。
- `.\gradlew.bat :app:testDebugUnitTest --tests "com.embytv.ui.player.*"` 通过。
- `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug` 通过。
- `powershell -ExecutionPolicy Bypass -File .\scripts\player-runtime-preflight.ps1` 通过；当前机器播放器 JVM 测试、Debug 构建和 APK 检查为 OK，ADB 设备与 AVD 为 WARN。
- `git diff --check -- app/src/main/java/com/embytv/ui/player/PlayerMediaItemFactory.kt app/src/test/java/com/embytv/ui/player/PlayerMediaItemFactoryTest.kt helloagents/main/CHANGELOG.md helloagents/main/wiki/modules/player.md helloagents/main/wiki/data.md` 通过（仅输出 LF/CRLF 工作区换行提示）。
- `rg -n 'api_key\s*=\s*"|accessToken\s*=\s*"|password\s*=\s*"|eval\(|exec\(' app\src\main\java\com\embytv\ui app\src\main\java\com\embytv\data scripts` 未发现新增明文密钥或危险执行模式。

## [0.5.0] - 2026-05-29

### 新增
- 新增兼容式主题系统，支持 Cinematic Glass、Dark Minimal、Emby Classic 和高对比度配色。
- 新增显示与辅助设置页，可通过抽屉进入并持久化主题、高对比度和字体大小偏好。
- 新增播放器速度控制，支持 0.5x、0.75x、1x、1.25x、1.5x 和 2x。
- 新增播放历史本地存储规则和 DataStore 持久化基础，最多保留最近 50 条媒体记录。
- 新增组件库 README 和核心组件 Compose Preview，记录组件分组、使用约定和设计令牌。
- 新增可访问性语义工具，并为核心媒体卡片、媒体库卡片补充屏幕阅读器描述。

### 变更
- `CinematicGlassColors` 改为读取 `EmbyTvTheme` 的当前主题颜色，保留既有调用方式。
- 应用版本提升至 `0.5.0`，`versionCode` 提升至 `6`。

### 验证
- `.\gradlew.bat :app:testDebugUnitTest` 通过。
- `.\gradlew.bat :app:assembleDebug` 通过。

## [0.4.0] - 2026-05-29

### 新增
- 新增搜索历史持久化，使用 DataStore Preferences 保存最近 20 条搜索记录，支持去重、点击复搜、单条删除和清空全部。
- 新增加载骨架屏组件，覆盖首页 Dashboard、媒体库列表、搜索、收藏、发现、发现入口、详情页和季内 Episode 列表等主要加载态。
- 新增媒体库长列表字母索引侧边栏和滚动位置指示器，媒体库资源数量达到阈值时可按首字母快速定位。

### 变更
- 搜索成功后自动记录关键词和结果数量；清空搜索或关闭搜索页时保留历史记录。
- `Modifier.shimmerEffect()` 从空实现改为 1.2 秒循环渐变动画，用于统一骨架屏视觉反馈。
- 应用版本提升至 `0.4.0`，`versionCode` 提升至 `5`。

### 验证
- `.\gradlew.bat :app:testDebugUnitTest` 通过。
- `.\gradlew.bat :app:assembleDebug` 通过。
- `git diff --check` 通过（仅存在仓库既有 LF/CRLF 提示）。
- 安全扫描未发现新增明文 token、secret、password 或危险 `eval/exec` 模式。

### 新增
- 新增搜索页，支持按关键词查询 Movie、Series、Episode、BoxSet 和 Playlist，并以 `Items` 为准展示结果。
- 新增合集、播放列表、类型、演员发现页，入口详情可展示真实 Emby 媒体资源。
- 新增 Emby 用户态写操作: 收藏/取消收藏、标记已播放/未播放、清除继续观看进度。
- 新增剧集播放队列，播放器 OSD 上一集/下一集可切换队列资源，自然结束可自动播放下一集。
- 新增播放器 Media3 音轨/字幕轨道面板，支持选择音轨、选择字幕和关闭字幕。
- 新增加密凭证列表模型，兼容旧单凭证并按服务器+用户去重保存。
- 新增 Movie/Series 媒体详情页，Movie 显示简介、演员、类型、年份、评分和播放按钮，Series 显示多季列表。
- 新增 Emby 详情、季列表和季内 Episode 接口聚合: `Users/{userId}/Items/{itemId}`、`Shows/{seriesId}/Seasons`、`Shows/{seriesId}/Episodes`。
- 新增季内 Episode 列表，Episode 卡片可继续进入既有播放详情读取和播放状态上报路径。

### 变更
- 图片加载改为通过 Coil `ImageRequest` 注入 `X-Emby-Authorization`，避免认证 Emby 服务器封面请求缺少 token。
- 搜索请求改为单一可取消协程链路，旧关键词慢返回不会覆盖当前关键词结果。
- 删除保存身份和清除继续观看进度改为二次确认，Back/取消不会执行危险操作。
- Emby 客户端认证头和首页显示版本统一使用 Gradle `versionName`。
- 播放器上一集/下一集切换前会停止当前媒体上报，播放进度轮询跟随当前 `PlaybackSource`。
- 抽屉导航整合搜索、收藏、合集、播放列表、类型、演员和媒体库入口，均支持 TV OK/Enter 操作。
- 详情页增加收藏、播放状态和清除进度操作按钮，成功后刷新 Emby 真实状态。
- 首页 Dashboard 额外读取 `Shows/NextUp`，空结果不影响原有首页展示。
- 首页、媒体库列表和收藏页中 Movie/Series 卡片 OK 行为改为进入详情页；Episode 保持直接播放。
- Series 季卡片使用 `UserData.UnplayedItemCount` 显示”剩 n 集”角标，缺失或为 0 时不显示。
- 媒体详情页补齐媒体信息和演员信息独立展示区，电影详情提供更明显的”播放”按钮，电视剧详情提供”查看季列表”入口。
- 已保存 token 冷启动时优先恢复凭证，只有无凭证或凭证失效时才启动手机扫码同步服务。
- 首页 Dashboard 聚合改为受控并发加载，Emby API service 按服务器和 token 复用，图片 URL 按用途追加尺寸参数。

### 修复
- 修复自动/手动切集后进度、暂停或退出可能继续上报到旧媒体的问题。
- 修复认证服务器图片接口可能因 Coil 图片请求未带 Emby 认证头而显示占位图的问题。
- 修复快速输入搜索关键词时旧搜索结果可能反写覆盖新结果的问题。
- 修复首页和 Emby 后台会话版本仍显示 `0.2.0` 的硬编码问题。
- 补齐 Coil 3 `coil-network-okhttp` 依赖，修复 Emby 媒体库、媒体卡片和详情页网络封面全部显示占位的问题。
- 修复 TV 遥控器在媒体卡片、媒体库卡片和通用图标按钮上需要按两次 OK/Enter 才触发点击的问题。

### 验证
- `.\gradlew.bat :app:testDebugUnitTest` 通过。
- `.\gradlew.bat :app:assembleDebug` 通过。

## [0.3.0] - 2026-05-29

### 新增
- 新增 UI 状态面板组件，错误状态支持网络、认证、404、服务器和未知类型图标，并可展示遥控器可聚焦的重试按钮。
- 新增空状态面板、通用动画规范和 UI Modifier 扩展，沉淀 Cinematic Glass 组件尺寸、语义色和进度条尺寸常量。
- 播放器 OSD 新增缓冲进度状态，进度条改为灰色缓冲层 + 绿色播放层的双层展示。

### 变更
- 网络图片统一构建 Coil `ImageRequest`，保留 Emby 图片认证 Header，增加 300ms crossfade、加载占位和失败占位。
- 通用 `GlassPanel` 焦点态升级为 200ms 动画、3dp 渐变边框和 8dp 阴影，增强 TV 遥控器焦点可见性。
- 收藏页、媒体库页、凭证页等原有状态提示通过新状态面板展示，错误场景可直接提供重试操作。
- 应用版本提升至 `0.3.0`。

### 验证
- `.\gradlew.bat :app:testDebugUnitTest` 通过。
- `.\gradlew.bat :app:assembleDebug` 通过。

## [0.2.0] - 2026-05-28

### 新增
- 落地 Cinematic Glass TV 核心体验: 服务器配置页、首页媒体中心、播放 Compose OSD 与弹幕开关。
- 新增 Coil Compose 3.4.0 用于 Emby 媒体图片加载。
- 新增首页 Dashboard 映射和播放器 OSD reducer 单元测试。
- 新增 Emby 首页真实 Dashboard 聚合: `Views`、按库统计、`Resume`、`Latest`。
- 新增 `PlaybackDetails`，播放器 OSD 可展示 Emby `PlaybackInfo` 返回的真实容器、编码、画质、音轨和字幕状态。
- 新增首页按媒体库展示最新入库资源分区，数据来自每个媒体库的 `ParentId + DateCreated` 查询。
- 新增 Emby Playback Check-ins 上报，播放开始、进度、暂停/恢复、快退/快进和停止播放会同步到服务器后台。
- 新增媒体库资源列表页，首页或抽屉对媒体库按 OK/Enter 可进入该库首屏资源列表。
- 新增收藏资源页，抽屉进入后按电影和电视剧两个维度展示收藏资源，并为每个卡片显示图片区域和资源名字。

### 变更
- 验证 `C:\Users\MyPC\.jdks\corretto-17.0.16` 可用于 Gradle，记录当前 Android SDK 路径仍缺失。
- 配置本机 Android SDK 路径 `C:\Users\MyPC\AppData\Local\Android\Sdk`，并将 `compileSdk` 调整为 36 + `compileSdkMinor = 1` 以匹配已安装的 `android-36.1`。
- 播放页关闭 Media3 默认控制器，改用 Compose OSD 管理播放、进度、返回键和弹幕快捷入口。
- 完善 TV 遥控器操作闭环: 抽屉 Back/焦点管理、首页禁用入口提示、播放 OSD 焦点与未实现入口反馈。
- 拆分 Emby 服务器配置字段，新增手机扫码同步到 TV 表单，并保存用户名展示字段与 Emby token 凭证，不保存密码。
- 首页首屏不再全量拉取 Movie/Episode 列表，改为按 Emby 真实聚合接口加载媒体库、继续观看和最近入库。
- 首页媒体库卡片显示真实媒体库名称与封面；继续观看和按库最新资源卡片优先展示 Thumb/Backdrop 缩略图，并为 Episode 展示真实剧名和 SxxExx 信息。
- 压缩 TV 端 Emby 配置页布局，用户名和密码改为同一行显示，连接按钮文案改为“确定连接”，避免 1080p 电视上底部字段被裁剪。
- 首页按库最新资源改为使用 `Items/Latest?ParentId=...`；电影库按 Movie 展示，剧集库按 Series 维度展示并显示剩余未播放集数角标。
- 收藏页使用 `Filters=IsFavorite` 拉取 Movie、Series 和 Episode，Episode 收藏会聚合为 Series 卡片，避免电视剧重复刷屏。

### 修复
- 移除 AGP 9 下不再需要的 `org.jetbrains.kotlin.android` 插件配置，避免 Gradle 构建在插件应用阶段失败。
- 修复 Android SDK 36.1 的 Gradle 配置方式，避免误用 `compileSdk = "android-36.1"` 或 `compileSdkExtension = 20`。
- 移除生产入口中的样例播放、Big Buck Bunny 和播放器硬编码 `HEVC / 4K HDR` 展示。
- 修复手机扫码同步提交时 NanoHTTPD 表单参数读取不稳定导致的“配对令牌无效”，同步成功后 token 在设置页生命周期内继续有效。
- 修复媒体库、电影和剧集封面字段兼容不足的问题，支持 `PrimaryImageTag`、父级图片字段、`SeriesPrimaryImageTag` 和无 tag 图片端点兜底。

### 验证
- `.\gradlew.bat :app:testDebugUnitTest` 通过。
- `.\gradlew.bat :app:assembleDebug` 通过。

## [0.1.0] - 2026-05-20

### 新增
- 初始化 Android TV 工程，接入 Jetpack Compose、TV Compose、Media3、Retrofit、OkHttp、AkDanmaku。
- 建立 MVVM + Coroutines + Flow 的基础分层。
- 增加 Emby 登录、媒体列表、播放 URL 构造与样例播放入口。
- 增加 Media3 FFmpeg 扩展 AAR 的本地接入预留。

### 变更
- README 更新为工程初始化说明与环境要求。

### 修复
- 无。

### 移除
- 无。
