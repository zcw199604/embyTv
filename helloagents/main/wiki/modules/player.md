# player

## 目的
封装 Media3 播放器创建、播放状态机、OSD 交互和弹幕播放协同。

## 模块概述
- **职责:** 创建 ExoPlayer，配置 OkHttp 数据源，启用扩展渲染器优先策略；通过 `PlayerManager` 管理播放器交互状态，并通过 `PlayerPlaybackController` 映射 Media3 事件。
- **状态:** 🚧开发中
- **最后更新:** 2026-07-07

## 规范

### 需求: 官方级 OSD 与播放状态机
**模块:** player
播放器交互层需以可测试状态机驱动 Compose OSD，避免 UI 直接散落播放状态判断。

#### 场景: OSD 显示与自动隐藏
当用户点击 D-Pad 中心键、执行播放控制、切换面板或发生缓冲/错误时：
- `PlayerManager` 通过 `StateFlow<PlayerOsdState>` 发布最新 OSD 状态。
- `PlayerRemoteKeyPolicy` 负责将 TV 遥控器 KeyUp 事件转换为 `Dispatch`、`SeekBy` 或 `Ignore` 命令；KeyDown 事件不触发动作，避免长按或系统重复事件造成重复操作。
- OSD 隐藏时 D-Pad 中心键、Enter、NumPadEnter、上/下方向键呼出 OSD；OSD 可见时这些键不被播放器根节点截获，留给当前焦点控件处理。
- `PlayerOsdAutoHidePolicy` 负责判断是否调度自动隐藏；Playing / Paused 的可交互 OSD 显示后 5 秒无交互自动隐藏，但音轨、字幕、倍速或弹幕快捷面板打开时不得自动隐藏，避免遥控器选项操作被计时器打断。
- `PlayerScreen` 的自动隐藏 effect 必须监听 `selectedQuickPanel`，面板打开或关闭时取消旧的 5 秒隐藏协程并重新按 `toAutoHideSnapshot()` 计算，避免用户刚打开字幕/音轨/弹幕面板后被旧计时器隐藏。
- `PlayerScreenOsdInteractionContractTest` 必须保护 `PlayerScreen` 的 OSD wiring：自动隐藏 effect 依赖 `visible`、`interactionRevision`、`status` 和 `selectedQuickPanel`；`onPreviewKeyEvent` 只提前消费隐藏态 seek 和 Back，中心键、Enter、上/下等呼出或焦点操作必须留给普通 `onKeyEvent` 按当前 OSD 可见性处理。
- OSD 显示和隐藏通过 `AnimatedVisibility` 执行轻量 fade + 小幅竖向位移动画；动画规格必须来自 `PlayerOsdMotionPolicy.TvDefault`，进入时长不超过 180ms、退出时长不超过 150ms，1080p 高度下位移不超过 32px，避免弱 TV 设备转场掉帧。
- Loading / Buffering / Ended / Error 属于阻塞或结果提示态，不自动隐藏，避免错误信息、缓冲提示或播放结束状态被 5 秒定时器清掉。
- Back 键在快捷面板打开时优先关闭当前面板并保留 OSD；没有快捷面板时隐藏 OSD；OSD 已隐藏时退出播放页。
- OSD 自动隐藏或 Back 隐藏时必须同时清理快捷面板、seek 预览和瞬态反馈消息，避免下一次呼出 OSD 时继续显示旧的 seek 方向、禁用入口或字幕/倍速反馈。

#### 场景: 遥控器焦点高亮
OSD 所有可操作入口必须提供明确 TV 焦点反馈：
- 图标按钮、快捷设置胶囊、轨道/弹幕选项均通过 `FocusableGlassSurface` 接收 `onFocusChanged` 状态，获得统一边框、阴影和缩放反馈。
- `OsdFocusVisualResolver` 负责将 focused、selected、primary、enabled 转换为图标/文字色、选中指示、标签权重和禁用透明度。
- `PlayerOsdFocusController` 负责主焦点请求策略：OSD 首次显示、从隐藏重新呼出、切换播放条目或快捷面板关闭时，将焦点恢复到主播放按钮；OSD 已显示时的面板切换和普通交互不得反复抢走当前焦点。
- `PlayerScreen` 主焦点恢复 `LaunchedEffect` 必须显式监听 `state.selectedQuickPanel`，确保策略中“快捷面板关闭后恢复主播放按钮焦点”的分支一定会执行，即使关闭面板的状态变化没有改变其它 effect key。
- `PlayerQuickPanelFocusPolicy` 负责快捷面板焦点请求策略：音轨、字幕、倍速或弹幕面板打开、不同面板之间切换，以及 Media3 轨道延迟上报导致同一面板从无选项变为有选项时，将焦点请求到首个可选项；同一面板内选项点击、已有选项状态同步或普通交互不得重复抢焦点。
- `PlayerScreenTrackPanelContractTest` 必须保护 `PlayerScreen` 的私有 Compose wiring：主播放按钮使用 `playFocusRequester`，快捷面板首个选项使用 `quickPanelFocusRequester`，上一集/下一集按钮把 `disabledReason` 继续传入 `FocusableGlassSurface`，音轨/字幕选择和关闭字幕必须调用 `PlayerTrackSelections` helper 后再同步 OSD 状态。
- 主播放按钮保持主色强调；音轨、字幕、倍速、弹幕等选中入口即使未聚焦也显示主色选中指示；禁用但带提示的入口允许聚焦并显示降权内容。
- OSD 快捷胶囊的短标签大写属于播放器 UI 稳定格式，必须使用 `Locale.US`，避免土耳其语等系统 Locale 下英文资源出现异常大写字形。
- 倍速、音轨和字幕快捷面板布局由 `PlayerQuickPanelLayoutPolicy.TvDefault` 统一约束，单行最多 3 个选项，避免多轨媒体或长语言标签在 1080p TV 上挤成一行。
- 音轨和字幕快捷面板不得硬限制只展示前几条轨道；多语言媒体返回的全部可支持轨道都必须进入 OSD 选项和焦点计数，超出可视高度时由面板滚动承载。
- `FocusableGlassSurface` 对禁用但带 `disabledReason` 的控件仍需把 focused 状态传给内容层，让 OSD 上一集/下一集等按钮在遥控器停留时既保留外层焦点边框，也能通过 `OsdFocusVisualResolver` 显示降权后的内容焦点反馈；禁用状态不得触发缩放动画。
- OSD 的禁用入口、错误和操作反馈通过通用 `RemoteHint` 展示，提示出现/消失使用短时 fade/slide 动画，避免遥控器反馈突兀闪烁。

#### 场景: 播放状态
播放器状态使用 `PlaybackEngineStatus` 表达 `Loading`、`Playing`、`Paused`、`Buffering`、`Ended` 和 `Error`。
- Media3 `STATE_BUFFERING` 映射到 `Buffering` 并保持 OSD 可见。
- `Buffering` / `Loading` 不得把 `PlayerOsdState.isPlaying` 改成 false；该字段作为播放意图驱动 `PlayerScreen` 调用 `player.play()` / `player.pause()`，缓冲时只暂停弹幕，避免误触发 Media3 pause。
- Media3 `STATE_ENDED` 映射到 `Ended`，保持 OSD 可见，置为非播放状态并暂停弹幕，同时继续触发停止上报和可用时自动播放下一集。
- 播放自然结束进入 `Ended` 时必须关闭已打开的快捷面板、清理旧 seek 预览和旧反馈消息，让结束态控制层或自动下一集反馈不被上一轮操作状态干扰。
- 播放错误映射为 `Error(message)` 并通过 OSD 反馈提示；当 Media3 错误消息为 null 或空白字符串时必须使用 `PlayerScreen` 传入的本地化 fallback，避免错误态 OSD 空白。
- 播放错误进入 OSD 时必须关闭已打开的音轨/字幕/倍速/弹幕面板，并清理旧 seek 预览，让错误反馈和错误态控制层成为当前唯一瞬态焦点。
- 播放/暂停按钮更新 `Paused` / `Playing`，并联动弹幕暂停状态。
- Media3 播放状态、播放错误、位置跳变、播放速度和首帧渲染事件由 `PlayerPlaybackController` 转换为 OSD action 与播放副作用，`PlayerScreen` 只负责消费 action/effect。
- `PlaybackSource.startPositionMs` 承载 Emby 继续观看位置；播放页设置媒体源后必须先归一化该值，再让 Media3 seek、AkDanmaku seek 和 `Sessions/Playing` 的开始上报共享同一起点，避免续播时画面、弹幕和服务端播放会话从不同位置开始。
- Media3 `onIsPlayingChanged(false)` 必须结合 `player.playbackState` 解释；当底层状态仍为 `STATE_BUFFERING` 时保持 OSD `Buffering`，当底层状态为 `STATE_ENDED` 时保持 OSD `Ended`，不得覆盖为 `Paused`。
- Media3 首帧渲染事件必须收敛 OSD 加载态：首帧已渲染且 `isPlaying=true` 时映射为 `Playing`，首帧已渲染但 `isPlaying=false` 时映射为 `Paused`，避免画面已就绪但 OSD 仍停留在 Loading。
- 生命周期暂停/恢复决策必须由 `PlayerLifecyclePlaybackPolicy` 基于最新 `PlayerOsdState` 快照生成：`ON_PAUSE` 上报暂停并暂停 Media3/弹幕；`ON_RESUME` 只有在最新状态仍为播放意图时才恢复 Media3 播放和暂停恢复上报，用户暂停后切后台再回前台不得因为 observer 捕获旧状态而自动恢复播放或弹幕。

#### 场景: 顶部实时信息
OSD 顶部必须展示播放上下文和真实技术信息：
- 标题来自 `PlaybackSource.title`。
- Episode 上下文来自 `PlaybackSource.contextLabel`，例如 `真实剧集 · S01E01`。
- 播放模式、容器、视频编码由 `PlayerPlaybackDetailsLabelResolver` 从 `PlaybackDetails` 生成；播放模式标签（如“直接播放”/`Direct Play`）来自 `PlayerScreen` 的 `stringResource`。
- 分辨率、HDR 范围和码率同样由 `PlayerPlaybackDetailsLabelResolver` 生成，例如 `2160p · HDR10 · 4.0 Mbps`；缺失质量、音轨或字幕时使用 `PlayerScreen` 通过 `stringResource` 传入的本地化 fallback。
- OSD 顶部标签、轨道可用性和音轨/字幕快捷胶囊摘要优先使用 `state.detailOverlay.playbackDetails`，缺失时回退到 `PlaybackSource.details`；因此暂停或呼出 OSD 后并行 Overlay 返回的新 PlaybackInfo 能刷新当前媒体技术信息，而不是一直停留在播放页初始数据。
- 码率属于播放器技术标签，`PlaybackDetails` domain 派生标签和 UI 层 `PlayerPlaybackDetailsLabelResolver` 都固定使用英文小数点，例如系统 Locale 为法语时仍显示 `4.0 Mbps`，避免不同系统语言下出现 `4,0 Mbps` 等不稳定 OSD/Overlay 展示。
- 容器、视频编码、音频编码和外挂字幕未知格式属于播放器技术标签，大小写转换必须显式使用 `Locale.US`；`PlaybackDetailsTest`、`PlayerPlaybackDetailsLabelsTest` 和 `PlayerTechnicalLocaleContractTest` 需覆盖系统 Locale 变化时仍保持稳定 ASCII 展示，避免不同电视系统语言影响 OSD 技术信息。
- OSD 播放时间和剩余时间属于时间轴技术标签，必须使用稳定西文数字格式，例如系统 Locale 为阿拉伯语时仍显示 `01:05` 和 `1:01:05`，避免进度条在不同电视系统语言下抖动或难以对照遥控器操作；当上游当前位置超过总时长时，状态机必须把当前位置限制到总时长，避免当前时间标签显示大于视频时长。

#### 场景: 播放列表导航
OSD 必须提供上一集/下一集入口，并由 Emby 上下文驱动：
- `PlaybackSource.queue` 承载 `PlaybackQueue`，包含 previous/current/next 和自动下一集开关。
- 用户从详情页季集列表、搜索或发现列表播放时，当前列表会作为显式队列传入。
- 没有显式队列时，Repository 会基于当前 Episode 的 `SeriesId` 和 `ParentId` 调用 `Shows/{seriesId}/Episodes` 获取同季队列，并按 `ParentIndexNumber`、`IndexNumber` 排序后计算上一集/下一集。
- 当当前集是同季最后一集且没有 next 时，Repository 会调用 `Shows/NextUp?SeriesId=...` 获取 Emby 的下一集候选，作为 OSD 下一集按钮和自然结束自动播放的兜底。
- `PlayerQueueNavigationPolicy` 根据 `PlaybackQueue` 生成上一集/下一集按钮状态、目标媒体和禁用提示；`PlayerScreen` 的切集点击从该策略状态读取 target，避免按钮可用性与实际切集目标分散判断。
- `PlayerQueueNavigationPolicy` 同时派生 `autoPlayNextTarget` 供播放自然结束自动切集使用；`autoPlayNext=false` 时手动下一集按钮仍可用，但自然结束不得自动切到下一集。
- `PlayerQueueNavigationPolicy` 必须过滤与 `PlaybackQueue.current.id` 相同的 previous/next 目标；过滤后对应按钮视为无可用目标，`autoPlayNextTarget` 也必须为 null，避免异常 Emby 队列或 NextUp 返回当前条目时切集到自己。
- 无可用目标时按钮保持可聚焦但禁用，策略状态中的 target 必须为 null，按 OK 只显示本地化的无上一集/无下一集提示，不退出播放页。

#### 场景: 方向键 seek
当 OSD 隐藏时按遥控器左/右方向键：
- 左键快退 10 秒，右键快进 10 秒。
- `PlayerRemoteKeyPolicy` 只在 OSD 隐藏时将左/右方向键转换为 `SeekBy(-10000/+10000)`；OSD 可见时左右键必须保留给焦点移动和面板内导航。
- `SeekPreviewState` 记录原始位置、目标位置、累计方向标签和可选缩略图 URL；连续 seek 以当前预览目标为基准累加，OSD 标签显示从原始位置到当前目标的实际可达偏移量，并保留上一张缩略图。
- `PlaybackSource.seekThumbnails` 优先来自 Emby `Chapters` 章节图，`previewThumbnailFor(positionMs)` 选择目标位置之前最近的章节图。
- Repository 只把带有效 `ImageTag` 的章节映射为 `SeekThumbnail`；章节 `StartPositionTicks` 为负数或缺失时按 0ms 处理，极端大 ticks 通过除法安全换算为毫秒，避免 seek 预览时间线出现溢出或无图章节。
- `PlaybackSource.previewThumbnailUrl` 优先来自当前媒体 `Thumb`，其次为 Backdrop/Primary，用于 Emby 未提供章节图或 trickplay 图时的稳定兜底；章节图和兜底图 URL 进入 OSD 前必须 trim，空白时必须视为不可用，不得向 OSD 传递空白图片地址。
- `PlayerManager.requestSeekPreview()` 负责根据目标位置调用 thumbnail provider 生成缩略图 URL，`PlayerScreen` 不直接拼装 seek preview action；provider 返回值必须 trim，null 或空白字符串不得覆盖上一张有效缩略图。
- seek 目标计算必须使用饱和加法；当总时长未知且当前位置异常接近 `Long.MAX_VALUE` 时，继续右键 seek 应饱和到 `Long.MAX_VALUE`，不得因 Long 溢出回绕到 0 或负数。
- `PlayerManager.requestSeekPreview()` 传给 thumbnail provider 的目标位置必须与 reducer 最终写入 `SeekPreviewState.targetPositionMs` 的饱和目标一致，避免 OSD 显示 0ms 缩略图但实际 seek 到末尾。
- seek 累计标签必须使用目标位置相对原始位置的饱和差值生成；即使异常状态中的 `originPositionMs` 超出已归一化时间轴，也不得因 `target - origin` 的 Long 溢出把 `+` / `-` 方向翻转。
- 提交 seek 后同步更新 Media3、Emby 播放进度上报和 AkDanmaku 进度。
- 打开、关闭、切换或在音轨、字幕、倍速、弹幕快捷面板内执行用户选择时必须清理 `SeekPreviewState`，避免旧缩略图和累计 seek 标签与设置面板同时停留在 OSD 中。
- 播放页即时 seek 与 Media3 `onPositionDiscontinuity` 可能先后报告同一目标位置，`PlaybackReportingCoordinator` 必须按位置和暂停状态去重，避免一次遥控器 seek 产生重复 Emby Progress check-in。
- seek 提交后保留 `SeekPreviewState`，确保缩略图和方向标签有机会在 OSD 中渲染；OSD 自动隐藏、Back 隐藏或取消 seek 时再清理该瞬态预览。

#### 场景: 播放中详情 Overlay
当 OSD 显示、播放器暂停或播放自然结束时：
- 若 `PlaybackSource` 带有 `session` 和 `deviceId`，播放页通过 `EmbyRepository.loadPlaybackOverlayDetails` 并行加载完整媒体详情与 PlaybackInfo。
- Overlay 加载开始、成功和失败必须携带当前 `PlaybackSource.itemId`；加载成功后通过 `PlayerOsdAction.DetailOverlayLoaded(itemId, mediaDetail, playbackDetails)` 同时写入 `PlayerDetailOverlayState.itemId`、`detail` 和 `playbackDetails`，让 OSD 顶部技术标签、轨道入口和快捷胶囊能使用当前媒体的最新 PlaybackInfo。
- `EmbyRepositoryMediaDetailTest` 必须覆盖该并行契约：即使完整媒体详情请求尚未返回，PlaybackInfo 请求也应已启动，避免 OSD 呼出时详情 Overlay 串行等待。
- `PlayerDetailOverlayLoadPolicy` 负责详情加载门禁：仅在 OSD 需要展示、播放器暂停或播放自然结束且会话上下文可用时请求详情；同一 `itemId` 已加载、正在加载或已失败时不重复请求，但旧播放项的 loaded/error/loading 状态不得阻止新 `itemId` 重新加载详情。
- `PlayerOsdReducer` 接收 `DetailOverlayLoaded` 或 `DetailOverlayFailed` 时必须校验 action 的 `itemId` 与当前 `PlayerDetailOverlayState.itemId` 一致；如果当前 Overlay 已归属其它媒体项，应忽略旧请求晚到结果，避免切集后旧详情、旧 PlaybackInfo 或旧错误提示覆盖当前媒体 OSD。
- `PlayerDetailOverlayVisibilityPolicy` 负责详情 Overlay 可见性：OSD 可见时展示详情；播放器暂停或自然结束时即使 OSD 已隐藏，也要在播放画面根层继续展示已加载或正在加载的详情 Overlay。
- Compose 加载 effect 只依赖播放项、会话上下文和“是否需要展示 Overlay”的布尔值；OSD 可见期间 Loading/Buffering/Playing 等状态细跳不得取消并重启同一次详情请求。
- Overlay 加载失败后写入带 `itemId` 的详情 Overlay 错误状态，并同步通过 `RemoteHint` 展示同一条 OSD 反馈；同一播放项内再次呼出 OSD 不自动重复请求，避免弱网时反复加载和闪烁；切换播放项时即使旧状态仍保留，也必须允许当前媒体重新加载。
- Overlay 展示标题、年份、社区/IMDb 类评分、影评评分、家长/官方分级、类型、IMDb/Douban 外部 ID、简介和演员摘要。
- Overlay 宽度必须使用 `fillMaxWidth().widthIn(max = 420.dp)` 这类最大宽度约束，不得固定 `width(420.dp)`；右侧安全区、窄视口或不同 TV 缩放设置下应优先收缩而不是裁切。
- 家长/官方分级来自 Emby `OfficialRating`，展示时必须通过 `player_official_rating_label` 资源格式化为带语义前缀的本地化标签，例如中文“分级 PG-13”、英文 `Rated PG-13`；空白值不展示，避免 Details Overlay 裸显 `PG-13` 等原始值导致语义不清。
- IMDb/Douban 外部 ID 展示由 `PlayerDetailProviderIdsLabelResolver` 统一生成，固定 IMDb 在前、Douban 在后，忽略 provider key 大小写并跳过空值或未知 provider，避免 Compose 私有逻辑缺少回归测试。
- 演员摘要由 `PlayerDetailCastLabelResolver` 统一生成，只展示 Emby `People.Type=Actor` 的人物，保留角色名并限制最多 4 个，避免导演/编剧排在前面时被误放进“演员”行；角色名连接格式由 `PlayerScreen` 通过 `stringResource` 注入，中文使用“姓名 饰 角色”，英文使用“Name as Role”。
- Overlay 评分值属于技术信息，社区/IMDb 类评分固定保留 1 位小数、影评评分固定为整数，并使用英文小数点，例如系统 Locale 为法语时仍显示 `8.6` 而不是 `8,6`；家长/官方分级的可见文案由 UI 层资源提供，分级值本身 trim 后原样保留。
- 加载失败只显示 OSD 错误信息，不阻塞播放控制。

#### 场景: 弹幕实时设置
弹幕设置归入 `PlayerOsdState.danmakuSettings`：
- 弹幕快捷胶囊只负责打开 `PlayerQuickPanel.Danmaku` 设置面板，不直接开关，避免遥控器误触导致状态反转。
- 音轨、字幕、倍速和弹幕快捷胶囊支持同入口开关：重复按当前已选中的胶囊会关闭面板，按其他胶囊则切换到目标面板。
- 弹幕面板内提供显式“开/关”按钮，使用 `PlayerOsdAction.SetDanmakuEnabled` 保持面板打开并避免重复点击误切换。
- 支持透明度、字号比例和显示区域（顶部/全屏）。
- 透明度限制在 0.2..1.0，字号比例限制在 0.8..1.6；若上游传入 `NaN` 或其它非有限值，透明度回退到 1.0、字号比例回退到 1.15 后再限幅，避免 TV 端可读性、Compose alpha 和 AkDanmaku 配置污染问题。
- 弹幕面板布局由 `DanmakuQuickPanelLayoutPolicy.TvDefault` 统一约束，9 个快捷设置按开关、透明度、字号和显示区域拆成多行，单行最多 3 个选项，避免 1080p TV 上横向按钮过密。
- `DanmakuPlaybackPolicy` 将弹幕播放状态转换为 `Start(config)` / `Pause` 命令，并将 seek 同步转换为 `ClearAndSeek(positionMs)` 命令，避免 Compose UI 直接散落 akdanmaku 调用判断。
- `DanmakuOverlay` 必须监听 `danmakuEnabled`、`danmakuPaused` 和 `danmakuSettings.playbackConfigKey()`，持续把开关、暂停/恢复、字号和显示区域变化应用到 `DanmakuPlayer`；`PlayerScreen` 不得再对同一组状态建立重复监听，避免一次设置变化重复 `updateConfig/start`。
- `PlayerScreenDanmakuSyncContractTest` 必须保护弹幕同步 wiring：持续配置同步归 `DanmakuOverlay` 所有，`PlayerScreen.syncDanmakuTo` 在 seek 或 Media3 位置跳变后必须按停止旧帧、`seekTo` 目标毫秒位置、再按当前 OSD 弹幕状态恢复播放/暂停的顺序执行。
- 弹幕配置变化时先调用 `DanmakuPlayer.updateConfig(config)` 再 `start(config)`，确保字号和显示区域实时生效；透明度只通过 `DanmakuOverlay` 的 Compose alpha 应用，不改变 `DanmakuPlaybackConfigKey`，避免弱 TV 设备上单独调透明度时重启 akdanmaku 渲染。
- AkDanmaku 视图绑定、透明度和持续 `DanmakuConfig` 应用由 `DanmakuOverlay` 承担；`PlayerScreen` 只保留播放源切换、seek 同步和生命周期恢复这类播放器事件驱动命令。
- Seek 或 Media3 位置跳变时先停止当前弹幕帧、再 `seekTo` 到目标毫秒位置，并按当前 OSD 弹幕状态恢复播放或保持暂停，保证 seek 后弹幕按新时间轴重绘。
- Media3 `onPositionDiscontinuity` 必须把 `reason` 传入 `PlayerPlaybackController`；所有位置跳变都同步弹幕时间轴，但只有 `DISCONTINUITY_REASON_SEEK` 和 `DISCONTINUITY_REASON_SEEK_ADJUSTMENT` 触发 `ReportSeek` 和 Emby Progress，自动切集等非 seek 跳变不得伪造成用户 seek。
- 切换播放源时先停止旧弹幕、更新新媒体弹幕数据、seek 到当前播放源 `startPositionMs`，再按当前弹幕设置启动，避免续播时弹幕从 0 开始或短暂显示上一媒体的弹幕。
- 播放页以 250ms 的受控 UI 进度采样刷新 OSD 位置，Emby 后台进度上报仍由 `PlaybackReportingCoordinator` 按 10 秒节流，避免高频 UI 刷新放大网络压力。
- `PlayerManager` 必须按完整 `PlaybackSource` 和归一化续播起点作为 Compose `remember` key 重建；当同一媒体切换 MediaSource、流地址、播放会话、弹幕数据或其它播放源字段时，旧 OSD 的 seek 预览、详情 Overlay、轨道本地选中态和瞬态反馈不得泄漏到新媒体。

#### 场景: 轨道与外挂字幕标签
当 Emby `PlaybackInfo` 返回音轨或字幕流时：
- `PlaybackTrack.label` 统一生成轨道展示标签，并对 Emby 原始语言码做友好名称归一化；Emby `DisplayTitle` 进入标签前必须 trim 首尾空白，避免 OSD 顶部音轨/字幕摘要和快捷胶囊显示脏文本。
- 外挂字幕优先展示规范化语言标签、字幕格式和 `External` 标记，例如 `Chinese (Simplified) · SRT · External`；`zh_Hans`、`zh_CN`、`en_US` 等下划线或地区变体不得直接出现在 OSD，Emby 返回 `subrip` / `webvtt` codec 时也必须展示为 `SRT` / `VTT`。
- 外挂字幕读取 Emby `DeliveryUrl` 并规范化为带 `api_key` 的绝对地址；若绝对 URL 已带 `api_key` 参数，检测必须按 query 参数名精确匹配且大小写不敏感并保留原 URL，避免 CDN 或服务端返回 `API_KEY` 等变体时重复追加 token，也避免 `not_api_key` 等相似参数误阻止 token 追加；注入 Media3 前必须 trim URL 首尾空白，避免服务端或插件返回带空白的 `DeliveryUrl` 时 MIME 后缀推断失败或 `SubtitleConfiguration` 携带非法空白；SRT/SubRip、WebVTT、ASS/SSA 通过 `MediaItem.SubtitleConfiguration` 注入 Media3，随播放器轨道一起上报到 OSD。
- 当 Emby 外挂字幕 `Codec` 缺失或为空时，`PlayerMediaItemFactory` 必须回退读取 trim 后 `DeliveryUrl` 路径后缀推断 SRT/VTT/ASS/SSA MIME；`PlaybackTrack.label` 也使用同一后缀推断生成 `VTT`/`ASS` 等格式标签，避免插件字幕源只返回文件 URL 时被跳过或显示不清。
- 外挂字幕注入 Media3 时，`language` 必须从 Emby 原始语言码规范化为播放器可匹配的 BCP-47 风格语言标签，例如 `chi/zho/zh/zh_CN/zh_Hans/zh_Hans_CN` 映射为 `zh-CN`，`zh_Hant_HK` 等繁体脚本/地区组合映射为 `zh-TW`，`eng` 映射为 `en`，`en_US` 映射为 `en-US`；语言标签大小写规范化必须显式使用 `Locale.US`，OSD 展示仍使用 `PlaybackTrack.label` 的友好语言名。
- 外挂字幕注入 Media3 时必须保留 Emby `IsDefault` 与 `IsForced` 语义；当同一字幕同时是默认和强制字幕时，`SubtitleConfiguration.selectionFlags` 必须同时包含 `C.SELECTION_FLAG_DEFAULT` 与 `C.SELECTION_FLAG_FORCED`。
- 播放页必须通过 `PlayerMediaItemFactory.create(playbackSource)` 构造 Media3 `MediaItem`；外挂字幕筛选、MIME 映射和默认/强制标记转换集中在该工厂，避免 `PlayerScreen` 直接拼装字幕配置。
- `PlayerMediaItemFactory.create(playbackSource)` 必须把 `PlaybackSource.itemId` 写入 Media3 `MediaItem.mediaId`，并把 `PlaybackSource.title` 写入 `MediaMetadata.title`，确保 Media3 回调、日志、后续会话集成和调试信息能稳定对应到 Emby 条目。
- OSD 音轨/字幕快捷入口可用性由 `PlayerTrackAvailabilityResolver` 统一判断：Media3 `Tracks` 已返回可选轨道时优先使用真实可选项；Media3 尚未上报但 Emby `PlaybackInfo` 已提供音轨/字幕摘要时，入口仍保持可用，避免顶部显示有轨道而面板入口提示“无字幕/无音轨”。
- Media3 内嵌轨道菜单项由 `PlayerTrackOptionMapper` 统一生成；只展示 `group.isTrackSupported(trackIndex)` 为 true 的可选轨道，避免 OSD 暴露 Media3 无法切换的音轨或字幕；当 Media3 `Format.label` 存在时必须先 trim 首尾空白再显示，避免服务端或封装元数据脏值导致 OSD 按钮文本不齐；当 `Format.label` 缺失时，音轨用友好语言名、编码和声道兜底（如 `English AAC 5.1`），字幕用友好语言名和字幕格式兜底（如 `Chinese (Simplified) SRT`），并将 `en_US` / `en-US`、`zh_Hans` / `zh-Hans` 等常见地区或脚本变体折叠为友好语言名，相关大小写转换必须显式使用 `Locale.US`，避免 OSD 直接显示 `chi`、`application/x-subrip`、原始地区码或受系统 Locale 影响的底层值。
- OSD 音轨/字幕快捷胶囊的摘要值由 `PlayerTrackSummaryLabelResolver` 生成：优先显示 Media3 当前选中轨道；尚未有 Media3 选中态时回退到 PlaybackInfo 默认/首个轨道；`subtitleDisabled=true` 时必须显示本地化无字幕标签，避免关闭字幕后入口仍展示 PlaybackInfo 默认字幕。
- Media3 内嵌轨道仍通过 `TrackSelectionOverride` 切换，关闭字幕使用 `setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)` 并清理 text overrides，避免后续重新启用文字轨道时恢复旧字幕。
- OSD 执行关闭字幕后必须立即将本地 `subtitleTracks.selected` 清空，避免 Media3 轨道回调到达前同时显示“关闭字幕”和某条字幕已选中。
- OSD 处于 `subtitleDisabled` 时，即使 Media3 延迟上报的 `TracksChanged` 仍包含旧的 selected 字幕，状态机也必须保留关闭意图并清空本地字幕 selected；只有用户显式选择字幕轨道才恢复字幕。
- OSD 执行音轨/字幕选择后必须通过 `PlayerOsdAction.SelectTrack` 即时更新本地选中态；选择字幕时同步清除 `subtitleDisabled`，选择音轨时不得改变字幕关闭状态。
- Media3 轨道参数选择必须保持同类型唯一、跨类型互不干扰：选择新的音轨会替换旧音轨 override，选择新的字幕会替换旧字幕 override；选择音轨不得清理既有字幕 override，也不得在字幕已关闭时重新启用 `C.TRACK_TYPE_TEXT`；选择字幕不得清理既有音轨 override，关闭字幕只禁用并清理 text 类型 override，不得清理音轨 override。
- OSD 执行音轨/字幕选择、关闭字幕、用户倍速选择或弹幕设置时必须清理旧 seek 预览和过期反馈；Media3 无反馈倍速同步只更新 `playbackSpeed`，不得呼出 OSD 或清理用户正在查看的状态。
- OSD 倍速状态只能保存 `SupportedPlaybackSpeeds` 中的有限值；Media3 或测试桩回调 `NaN`、正无穷或负无穷时必须回退为 1.0x，避免速度胶囊显示非法标签或后续 `player.setPlaybackSpeed()` 接收非法值。
- 关闭字幕后再次选择字幕时必须先清除 `C.TRACK_TYPE_TEXT` 禁用状态，再写入字幕 `TrackSelectionOverride`，避免遥控器面板显示已选择但播放器仍不渲染字幕。
- Media3 轨道参数构造集中在 `PlayerTrackSelections`，播放页只调用 `selectTrack` 和 `disableSubtitles`。

#### 场景: 多语言文案
播放器 OSD 核心可见文案必须使用 Android `stringResource`：
- 默认 `values/strings.xml` 为简体中文。
- `values-en/strings.xml` 提供英文资源。
- `PlayerStringResourceParityTest` 必须保证 `player_` 和 `settings_language` 相关资源键在默认中文与英文资源中保持一致，新增播放器可见文案时不得只更新单一语言。
- `PlayerSourceI18nContractTest` 必须扫描 `ui/player` 生产源码，确保播放器 OSD、详情 Overlay、轨道面板和弹幕面板的可见中文文案不以硬编码形式进入 Kotlin 源码。
- 语言偏好通过 `ThemePreferences.language` 和 `ThemePreferenceStore.preferencesFlow` 预留监听入口，设置页可写入语言选择。
- `HomeViewModel` 通过 `HomeThemePreferenceObserver` 将语言偏好写入 `HomeUiState.themePreferences`，为 ViewModel 层语言变化响应保留可测试入口。
- `MainActivity` 使用 `LocalizedApp` 将语言偏好转换为 localized Context / Configuration，使 Compose `stringResource` 读取对应语言资源。
- 字幕关闭、倍速切换、播放失败和详情加载失败等反馈由 `PlayerScreen` 使用 `stringResource` 生成后传入状态机或控制器，`PlayerOsdReducer` 与 `PlayerPlaybackController` 不固定中文文案；Media3 底层倍速回调只同步 `playbackSpeed`，不得自动呼出 OSD 或打开速度面板。
- Details Overlay 演员角色格式和家长/官方分级前缀同样必须资源化，不得在纯 Kotlin resolver 中硬编码中文或英文连接词。
- OSD 顶部播放模式、质量、音轨和字幕的可见文案由 `PlayerPlaybackDetailsLabelResolver` 使用 UI 层资源文案生成，避免跨语言环境显示 domain 层默认值。

## 运行验收清单
以下清单用于补齐“官方级”播放器完成前必须获得的 Android TV / 真实 Emby Server 证据；未完成前不得将播放器目标标记为完成。

### 运行前置预检
- 执行 `.\scripts\player-runtime-preflight.ps1`，确认 JDK、Android SDK、播放器 JVM 测试、Debug APK 构建和 APK 文件检查为 OK。
- 若需要把“无设备/无 AVD”作为阻断条件，执行 `.\scripts\player-runtime-preflight.ps1 -RequireDevice` 或 `.\scripts\player-runtime-preflight.ps1 -RequireAvd`；默认模式只将设备和 AVD 缺失标记为 WARN，便于无设备环境继续完成静态验证。
- 连接 Android TV 或模拟器后，执行 `.\scripts\player-runtime-preflight.ps1 -Install -Launch -CaptureLogcat`，安装 Debug APK、启动 `com.embytv/.MainActivity` 并将短时 logcat 保存到 `build/player-runtime/`；脚本优先通过 `pidof com.embytv` 按应用进程过滤日志，并扫描启动崩溃、ANR、Media3/ExoPlayer 关键错误，发现问题时预检失败且写出 `logcat-issues-*.txt`。
- 多设备连接时必须追加 `-DeviceSerial <serial>`；若只想采集日志、不让已知日志问题阻断流程，可追加 `-AllowLogcatIssues`。
- Debug 构建中可通过 logcat 标签 `EmbyTvPlaybackReport` 辅助核对 Emby Playback Check-ins；日志只允许输出事件类型、`itemId`、`playlistItemId`/会话/设备上下文是否存在、毫秒位置和暂停状态，不得输出 `streamUrl`、`api_key`、token、密码、服务器地址或真实设备标识值。
- 真实 Emby 播放链路验收时执行 `.\scripts\player-runtime-preflight.ps1 -Install -Launch -CaptureLogcat -LogcatSeconds 90 -RequirePlaybackReports`，脚本必须在 logcat 中找到 Started、Progress 和 Stopped 的 `EmbyTvPlaybackReport` 成功诊断；若只验证部分事件，可用 `-RequiredPlaybackEvents Started,Progress` 缩小门禁。

### Android TV 设备或模拟器
- 使用遥控器 OK / Enter / D-Pad Center 从隐藏态呼出 OSD，确认焦点落到主播放按钮，OSD 5 秒无操作自动隐藏。
- OSD 可见时按方向键只移动焦点，不触发 seek；OSD 隐藏时左/右方向键分别快退/快进 10 秒，并显示累计 seek 标签和缩略图。
- 打开音轨、字幕、倍速或弹幕面板后等待 5 秒，确认 OSD 不自动隐藏；按 Back 先关闭面板，再按 Back 隐藏 OSD，再按 Back 退出播放页。
- 在 1080p 和 4K 显示尺寸下检查 OSD 顶部标题、剧集上下文、质量标签、详情 Overlay 和底部控制栏不重叠、不裁切。
- 在弱性能 TV 或模拟器低配置镜像上开启“显示 GPU 渲染条”或 Android Studio Profiler，确认 OSD fade/slide、RemoteHint 和焦点缩放没有明显掉帧。
- 暂停播放后等待 OSD 自动隐藏，确认 Details Overlay 仍可见；恢复播放后确认隐藏态不再保留详情 Overlay。

### 真实 Emby Server 播放链路
- 播放 Movie 和 Episode 各一条，确认 `Sessions/Playing`、`Sessions/Playing/Progress`、`Sessions/Playing/Stopped` 在 Emby 后台会话中可见，退出播放页后不会重复上报停止。
- Episode 从详情页季集列表进入播放时，确认上一集/下一集按钮目标正确；当前季最后一集时确认 `Shows/NextUp` 兜底的下一集可用。
- 使用含章节图的媒体执行多次连续 seek，确认缩略图选择目标位置之前最近章节图；无章节图时使用 Thumb/Backdrop/Primary 兜底图。
- 使用直接播放、转码或不同容器媒体，确认 OSD 顶部播放模式、容器、编码、分辨率、HDR 和码率与 Emby PlaybackInfo 一致。

### 轨道、字幕与弹幕
- 播放含多音轨媒体，切换音轨后确认 Media3 实际音频变化，OSD 选中态立即更新，字幕关闭状态不被音轨切换改写。
- 播放含内嵌字幕媒体，选择 SRT/ASS/SSA 字幕后确认渲染正常；关闭字幕后确认 OSD 本地选中态立即清空且 Media3 不恢复旧字幕。
- 播放含外挂字幕 `DeliveryUrl` 的媒体，确认 SRT/WebVTT/ASS/SSA 被注入 Media3，语言标签显示为友好名称，例如 `Chinese (Simplified) · SRT · External`。
- 打开弹幕后执行暂停、恢复、快进、快退和切集，确认 AkDanmaku 按毫秒时间轴同步，seek 后不会残留旧时间点弹幕。
- 调整弹幕透明度、字号和显示区域，确认透明度变化不触发明显重启，字号和区域实时生效且 1080p 下不遮挡 OSD 主控制。

### 多语言与错误态
- 在设置页切换为英文，重进播放器确认 OSD 控制、反馈、详情加载、播放失败、无上一集/无下一集等文案读取英文资源。
- 触发网络断开、服务端 401/404、解码失败或空白 Media3 错误消息，确认 OSD 显示本地化 fallback，而不是空白错误提示。
- 详情 Overlay 加载失败后再次呼出 OSD，确认同一播放项内不重复请求；切换到新播放项后允许重新请求详情。

### 需求: Emby 播放状态上报
**模块:** player
播放器需要向 Emby 后台同步开始播放、播放进度、暂停/恢复和停止播放状态。

#### 场景: 开始播放
当 `PlayerScreen` 设置媒体源并准备播放后：
- 调用 `POST Sessions/Playing`。
- 上报 `ItemId`、`MediaSourceId`、`PlaySessionId`、`PlaylistItemId` 和 `PositionTicks`；非播放列表来源时 `PlaylistItemId` 可为空。
- 同一播放页生命周期内只发送一次开始事件。

#### 场景: 进度、暂停与 seek
播放过程中：
- `PlaybackReportingCoordinator` 必须先收到 `onStarted` 才允许发送 Progress、暂停/恢复、seek 或 Stopped 上报，避免 Compose effect 初始化/释放顺序导致进度型或停止事件早于 `Sessions/Playing`。
- 默认每 10 秒调用 `POST Sessions/Playing/Progress`。
- `PlaybackReportingCoordinator` 接收到非正数 Progress 节流间隔时必须回退到默认 10 秒，避免测试桩或未来配置把播放器 250ms UI 轮询放大为高频 Emby 后台 Progress check-in。
- 周期 Progress Tick 中检测到 `isPaused` 与上次上报状态不一致时必须立即发送 Progress，不等待位置达到 10 秒节流阈值，确保 Emby 后台会话暂停/恢复状态及时更新。
- 播放/暂停按钮切换、生命周期暂停、快退和快进后立即强制上报。
- Media3 毫秒位置统一转换为 Emby ticks；负数位置归零，超过 `Long.MAX_VALUE / 10000` 的极端毫秒值饱和为 `Long.MAX_VALUE`，避免 Check-ins DTO 写入溢出的负 ticks。
- `PlayerScreen` 必须在 Debug 构建中围绕 Emby 上报调用输出安全诊断日志：queued、succeeded 和 failed；失败日志只记录非敏感上下文和异常对象，不得阻塞播放控制、Back 退出或播放器释放。
- OSD 每 250ms 读取 Media3 当前播放位置、总时长和 `bufferedPosition`，`PlayerOsdState.positionMs` 在总时长有效时必须保持在 `0..durationMs`，`PlayerOsdState.bufferedFraction` 只保存 0..1 的缓冲比例；若上游传入 `NaN` 或无穷值，状态机必须回退到当前播放进度，避免 Compose 进度条拿到非有限数或让缓冲层短于播放层。
- 状态机必须确保缓冲层不小于播放层，弱网时可直观看到已缓冲范围且不会出现绿色播放层超出灰色缓冲层的 OSD 抖动。

#### 场景: 停止播放
当播放自然结束、退出播放页、播放器释放或同一 `PlayerScreen` 实例切换到新的 `PlaybackSource` 时：
- 调用 `POST Sessions/Playing/Stopped`。
- 同一播放页生命周期内最多发送一次停止事件。
- 若开始播放事件尚未发出，则忽略停止事件，避免向 Emby 发送没有对应播放会话的 Stopped。
- `PlayerScreen` 必须在 `playbackSource` / `reportingCoordinator` 生命周期结束时调用旧 coordinator 的 `onStopped(player.currentPosition)`；手动上一集/下一集或自然结束已提前停止时，`PlaybackReportingCoordinator` 负责去重，避免重复 Stopped。
- 上报失败不阻塞 Back 退出和播放器释放。

### 需求: Media3 播放初始化
**模块:** player
播放器由 `Media3PlayerFactory` 统一创建，不在 UI 中直接拼装底层依赖。

#### 场景: FFmpeg 扩展可用
当 `app/libs` 中存在 Media3 FFmpeg 扩展 AAR 且类可被反射加载时：
- Media3 优先使用扩展渲染器。

#### 场景: FFmpeg 扩展不可用
当本地 AAR 不存在时：
- 播放器仍使用 Media3 默认渲染器工作。

## API接口
- `POST Sessions/Playing`
- `POST Sessions/Playing/Progress`
- `POST Sessions/Playing/Stopped`

## 数据模型
使用 `PlaybackSource.streamUrl`、`PlaybackSource.session`、`PlaybackSource.deviceId`、`PlaybackSource.previewThumbnailUrl`、`PlaybackSource.seekThumbnails`、`PlaybackSource.startPositionMs`、`SeekThumbnail`、`PlaybackSource.contextLabel`、`PlaybackReportingCoordinator`、`PlayerPlaybackDiagnostics`、`PlayerPlaybackController`、`PlayerPlaybackEffect`、`PlayerLifecyclePlaybackPolicy`、`PlayerLifecyclePlaybackSnapshot`、`PlayerLifecyclePlaybackEffect`、`PlayerManager`、`PlayerOsdState`、`PlaybackEngineStatus`、`PlayerStartupPositionPolicy`、`PlayerRemoteKeyPolicy`、`PlayerRemoteKeyCommand`、`PlayerQueueNavigationPolicy`、`PlayerQueueNavigationState`、`PlayerQueueNavigationItemState`、`PlayerOsdAutoHidePolicy`、`PlayerOsdAutoHideSnapshot`、`PlayerQuickPanelLayoutPolicy`、`PlayerQuickPanelFocusPolicy`、`PlayerQuickPanelFocusSnapshot`、`OsdFocusVisualResolver`、`FocusableGlassSurfacePolicy`、`PlayerOsdFocusController`、`PlayerOsdFocusSnapshot`、`PlayerDetailOverlayLoadPolicy`、`PlayerDetailOverlayLoadSnapshot`、`PlayerDetailOverlayVisibilityPolicy`、`PlayerDetailOverlayVisibilitySnapshot`、`PlayerDetailProviderIdsLabelResolver`、`PlayerDetailCastLabelResolver`、`DanmakuOverlaySettings`、`DanmakuQuickPanelLayoutPolicy`、`DanmakuPlaybackPolicy`、`DanmakuPlaybackCommand`、`DanmakuPlaybackConfigKey`、`DanmakuSyncCommand`、`SeekPreviewState`、`PlaybackTrack.externalUrl`、`PlayerExternalSubtitle`、`PlayerTrackOptionMapper`、`PlayerTrackSummaryLabelResolver`、`PlaybackOverlayDetails` 和 Emby Playback Check-ins DTO；`PlayerManager` 的 Compose 生命周期以完整 `PlaybackSource` 为重建边界，避免播放源字段变化后复用旧 OSD 状态。

## 依赖
- core.network
- Media3

## 变更历史
- Unreleased - 播放器新增 `PlayerManager` StateFlow 状态机、`PlayerPlaybackDiagnostics` Debug 安全诊断日志、`PlayerPlaybackController` Media3 事件映射层、`PlaybackEngineStatus.Ended` 播放结束态、`PlayerStartupPositionPolicy` 续播起点策略、`PlayerRemoteKeyPolicy` 遥控器按键命令策略、`PlayerQueueNavigationPolicy` 队列导航按钮状态/目标媒体策略、`PlayerOsdAutoHidePolicy` 自动隐藏调度策略、`PlayerOsdMotionPolicy` 轻量 OSD 转场策略、`PlayerQuickPanelLayoutPolicy` 快捷面板 TV 分行策略、`PlayerQuickPanelFocusPolicy` 快捷面板首选项焦点策略、`OsdFocusVisualResolver` 焦点/选中视觉策略、`FocusableGlassSurfacePolicy` 禁用焦点内容反馈策略、`PlayerOsdFocusController` 主焦点恢复策略、`PlayerDetailOverlayLoadPolicy` 详情加载门禁、`PlayerDetailOverlayVisibilityPolicy` 暂停/OSD 详情可见性策略、`PlayerDetailProviderIdsLabelResolver` IMDb/Douban 标识展示策略、`PlayerDetailCastLabelResolver` 演员摘要策略、Emby `Chapters` 章节缩略图时间线、`PlaybackSource.startPositionMs` 续播起点、`PlaybackQueue` 播放列表导航、5 秒 OSD 自动隐藏且快捷面板打开时保持可见、自动隐藏监听快捷面板变化、方向键 seek、播放详情 Overlay、弹幕透明度/字号/区域设置、弹幕面板 TV 多行布局、倍速/音轨/字幕面板 TV 多行布局、多轨音频/字幕滚动面板、`DanmakuOverlay`、`DanmakuPlaybackPolicy` 弹幕命令策略、`DanmakuQuickPanelLayoutPolicy` 弹幕快捷面板布局策略、弹幕开关/暂停/字号/显示区域由 `DanmakuOverlay` 单点持续同步到 AkDanmaku、透明度调整不重启 akdanmaku 配置、250ms OSD 进度刷新、Buffering/Ended 保留播放意图并防止 `onIsPlayingChanged(false)` 覆盖为 Paused、播放结束关闭快捷面板并清理 seek 预览/旧反馈、首帧渲染后按真实播放态收敛 Loading 到 Playing/Paused、播放器错误空白消息 fallback、播放错误关闭快捷面板并清理 seek 预览、详情 Overlay 加载失败同步 RemoteHint 反馈、生命周期恢复读取最新 OSD 快照、seek 缩略图预览保留到 OSD 隐藏且 Back 隐藏时同步清理、OSD 隐藏和快捷面板用户选择时同步清理瞬态反馈/seek 预览、OSD 轻量 fade/slide 进入退出动画、seek 后弹幕停止旧帧并按新时间轴重绘、Media3 非 seek 位置跳变只同步弹幕且不触发 Emby seek Progress、seek Progress 重复上报去重、OSD 时间轴稳定西文数字格式、Overlay 评分稳定英文小数点格式、`PlayerMediaItemFactory` 播放源/外挂字幕配置构造、外挂字幕语言标签与 SRT/VTT/ASS/SSA DeliveryUrl 注入 Media3、外挂字幕下划线/脚本/地区语言码和 `subrip` / `webvtt` codec 规范化、`PlaybackTrack.label` 地区/脚本语言码友好展示、OSD 文案资源化、播放器资源键中英文一致性测试、运行时语言应用、用户操作反馈与错误 fallback 文案由 Compose 资源传入 reducer/控制器、Media3 底层倍速同步不打断 OSD、`PlayerTrackSelections` 轨道参数契约测试与同类型 override 替换/跨类型 override 保留验证、`PlayerTrackAvailabilityResolver` 轨道入口可用性策略、`PlayerTrackOptionMapper` 内嵌轨道友好标签映射、轨道选择即时更新 OSD 本地选中态、字幕关闭即时清理 OSD 本地选中态、顶部 Episode 上下文/码率稳定格式展示和并行 Overlay 详情加载。
- [202605291416_ui_interaction_optimization_phase1](../../history/2026-05/202605291416_ui_interaction_optimization_phase1/) - 播放器 OSD 新增缓冲进度状态和双层进度条展示。
- [202605272133_emby_playback_reporting](../../history/2026-05/202605272133_emby_playback_reporting/) - 接入 Emby Playback Check-ins，播放、暂停、进度和退出时同步后台状态。
- [202605201342_emby_tv_init](../../history/2026-05/202605201342_emby_tv_init/) - 初始化 Media3 播放工厂。
