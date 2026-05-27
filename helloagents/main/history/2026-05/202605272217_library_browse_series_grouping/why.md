# 变更提案: 媒体库封面、列表页与剧集维度聚合

目录: `helloagents/main/history/2026-05/202605272217_library_browse_series_grouping/`

---

## 背景

当前首页已接入 Emby 真实 Dashboard，但用户实际安装后暴露出三个核心问题：

1. 媒体库、剧集、电影没有展示出对应的资源封面。
2. 遥控器 OK/Enter 进入媒体库时，仍只显示“媒体库详情暂未支持”，不能进入媒体库资源列表。
3. 首页各媒体库横排资源当前按 Episode 维度展示，剧集库会出现多张同一剧的单集卡片；用户期望按剧集维度展示，并显示剩余播放集数角标。

这些问题会直接影响 TV 端浏览效率：电视用户通常先进入媒体库，再按海报/封面快速识别电影或剧集；如果剧集按单集散列展示，会破坏“剧集 -> 季/集”的自然认知路径。

## 官方 API 依据

- Emby 图片入口使用 `/Items/{itemId}/Images/{imageType}`，图片类型包括 Primary、Thumb、Backdrop 等，客户端应根据条目返回的图片 tag 或直接图片端点构造 URL。
- `Users/{userId}/Items` 支持 `ParentId`、`Recursive`、`IncludeItemTypes`、`SortBy`、`SortOrder`、`Limit`、`StartIndex` 等参数，适合构建媒体库资源列表页。
- `Users/{userId}/Items/Latest` 支持按库获取最近入库资源；对于剧集库应使用 `GroupItems=true`，让 Episode 按 Series 聚合返回，避免首页按单集维度散列。

## 目标

- 修复媒体库、电影、剧集卡片封面缺失问题，优先展示真实 Emby 图片。
- 点击媒体库卡片后进入新的媒体库资源列表页。
- 媒体库列表页按当前库类型展示资源:
  - 电影库显示 Movie。
  - 剧集库显示 Series。
  - 混合或未知类型按 Movie/Series 优先展示，必要时保守支持 Episode。
- 首页各媒体库横排“最新入库”按剧集维度展示；电影仍按电影维度展示。
- 剧集卡片显示剩余未播放集数角标。
- 保持 TV 遥控器方向键、OK/Enter 和 Back 可完整操作。

## 范围内

- 扩展 Emby DTO 图片字段，覆盖 `PrimaryImageTag`、`ParentThumbImageTag`、`ParentBackdropImageTags`、`SeriesPrimaryImageTag` 等常见父级图片字段。
- 扩展图片 URL 构造器，支持无 tag 图片 URL 和父级条目图片 URL 兜底。
- 调整首页按库最新资源获取策略:
  - `CollectionType=tvshows` 使用 `Items/Latest?ParentId=...&GroupItems=true&IncludeItemTypes=Episode` 或等价聚合策略，最终 UI 卡片为 Series。
  - `CollectionType=movies` 使用 Movie。
- 新增媒体库资源列表 View 状态、数据加载方法和 Compose 页面。
- 媒体库卡片 OK/Enter 进入列表页；列表页 Back 返回首页并恢复合理焦点。
- 新增“剩余播放集数”领域字段和卡片角标。
- 补充单元测试与知识库。

## 范围外

- 不实现媒体详情页、剧集详情页、季/集列表页。
- 不实现分页无限加载；本次列表页先按固定页大小加载首屏资源。
- 不实现搜索、筛选、排序菜单和收藏入口。
- 不实现电影/剧集播放入口之外的复杂动作菜单。
- 不更改播放器状态上报、弹幕和音轨字幕功能。

## 用户价值

- 用户能通过真实封面识别媒体库、电影和剧集。
- 用户可以从首页进入某个媒体库，完成“选择媒体库 -> 浏览资源 -> 播放”的 TV 主路径。
- 剧集库首页不再被单集刷屏，展示维度更符合 Emby 和 TV 用户习惯。
- 剩余播放集数角标能快速提示哪些剧还有未看内容。

## 成功标准

- 媒体库、电影、剧集卡片在有真实图片字段时能显示图片；缺图才显示占位。
- 点击媒体库卡片能进入该媒体库资源列表页，不再只弹出“暂未支持”。
- 资源列表页显示当前媒体库名称、资源数量/类型提示和媒体卡片网格或横向分区。
- Back 从媒体库资源列表返回首页。
- 首页剧集库最新资源按 Series 去重展示，不出现同一剧的多个单集卡片。
- 剧集卡片能展示剩余未播放集数角标，例如“剩 3 集”。
- `.\gradlew.bat :app:testDebugUnitTest` 和 `.\gradlew.bat :app:assembleDebug` 通过。

## TDD 适用性

强制启用。该需求涉及可观察 UI 行为、API 契约、数据聚合规则和遥控器导航行为变化，必须先补 Repository/Mapper/状态机测试，再实现生产代码。

## 风险

- **Emby 图片字段差异:** 不同资源可能返回 `ImageTags`、`PrimaryImageTag`、父级图片 tag 或只有 item id；需要多级兜底。
- **剧集维度聚合差异:** `GroupItems=true` 在不同 Emby 版本或库类型下返回结构可能有差异；需要保守 fallback 到 Series 查询或本地按 `SeriesId/SeriesName` 聚合。
- **列表页范围扩大:** 媒体详情、分页、筛选很容易扩张；本次必须只做可浏览首屏列表。
- **焦点回退:** 首页与列表页切换后需要明确 Back 行为和初始焦点，否则 TV 操作会卡住。
- **性能:** 首页按库查询数量不能失控，最新资源和列表页都必须有固定 Limit。
