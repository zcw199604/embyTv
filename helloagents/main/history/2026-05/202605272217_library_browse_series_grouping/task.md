# 任务清单: 媒体库封面、列表页与剧集维度聚合

目录: `helloagents/main/history/2026-05/202605272217_library_browse_series_grouping/`

---

## 并行子代理标注

- 并行组 A: 任务 [1.1, 2.1, 2.2, 2.3, 2.4]；允许写入: `app/src/main/java/com/embytv/data/remote/`, `app/src/main/java/com/embytv/data/repository/`, `app/src/main/java/com/embytv/domain/model/`, `app/src/test/java/com/embytv/data/`；冲突域: Emby API/DTO/Repository 聚合和图片 URL 规则；验证: `.\gradlew.bat :app:testDebugUnitTest`
- 并行组 B: 任务 [1.2, 3.1, 3.2, 3.3, 3.4]；允许写入: `app/src/main/java/com/embytv/ui/home/`, `app/src/main/java/com/embytv/ui/components/`, `app/src/test/java/com/embytv/ui/home/`；冲突域: 首页 UI 模型、媒体库列表页、遥控器返回和卡片角标；验证: `.\gradlew.bat :app:testDebugUnitTest`
- 不可并行任务: [4.1, 4.2, 4.3, 5.1, 5.2, 5.3, 5.4, 6.1, 6.2, 6.3]；原因: 集成验证、知识库同步和最终构建需在数据/UI 合并后执行。

---

## 0. 方案边界确认
- [√] 0.1 确认本次只解决封面显示、媒体库资源列表页、首页剧集维度聚合和剩余集数角标，不实现详情页、分页、搜索筛选。
- [√] 0.2 确认不做全库 Episode 扫描计算剩余集数，优先使用 Emby 返回的未播放计数字段，避免大库性能问题。
- [√] 0.3 确认最小改动策略: 保持现有 HomeViewModel/HomeScreen 结构，不引入 Navigation 框架和本地数据库。

## 1. RED: 数据聚合与 UI 行为测试
- [√] 1.1 RED: 更新 `app/src/test/java/com/embytv/data/repository/EmbyRepositoryDashboardTest.kt` 或新增测试，断言图片字段只有 `PrimaryImageTag`、父级 `SeriesPrimaryImageTag`、`ParentThumbImageTag` 时仍能构造真实图片 URL，验证 why.md#目标-修复封面。
- [√] 1.2 RED: 更新 `app/src/test/java/com/embytv/ui/home/HomeDashboardMapperTest.kt`，断言 Series 卡片显示剧集维度标题、真实封面和 `剩 n 集` 角标，Movie 不显示剩余集数角标。
- [√] 1.3 RED: 新增 `HomeLibraryContentTest` 或 ViewModel 测试，断言点击媒体库后进入媒体库资源列表状态，Back/close 返回首页。
- [√] 1.4 RED: 增加 Repository 请求契约测试，断言 tvshows 最新资源请求使用 `GroupItems=true` 或 fallback 聚合为 Series，movies 请求使用 Movie。

> RED 证据: 首次运行 `.\gradlew.bat :app:testDebugUnitTest` 失败，失败点为缺失 `loadLibraryContent`、无 tag 图片兜底参数、`cornerBadge` 和 `LibraryContentUiState`，与目标行为匹配。

## 2. GREEN: Emby API、DTO 与 Repository
- [√] 2.1 扩展 `app/src/main/java/com/embytv/data/remote/dto/EmbyItemDtos.kt`，新增 `PrimaryImageTag`、`SeriesId`、`SeriesPrimaryImageTag`、`ParentThumbItemId`、`ParentThumbImageTag`、`ParentBackdropItemId`、`ParentBackdropImageTags`、`UserData.UnplayedItemCount` 等可空字段。
- [√] 2.2 扩展 `app/src/main/java/com/embytv/data/remote/EmbyApi.kt`，让 `getLatestItems` 支持 `ParentId`、`GroupItems`，让 `getItemsByParent` 支持动态 IncludeItemTypes、StartIndex、Limit、SortBy/SortOrder。
- [√] 2.3 扩展 `app/src/main/java/com/embytv/data/repository/EmbyStreamUrlBuilder.kt`，支持无 tag 图片 URL 和父级条目图片 URL 兜底。
- [√] 2.4 扩展 `app/src/main/java/com/embytv/domain/model/MediaItemSummary.kt`，增加 Series 聚合字段、`unplayedItemCount`、`cornerBadge` 所需领域字段和 `EmbyLibraryContent`。
- [√] 2.5 改造 `app/src/main/java/com/embytv/data/repository/EmbyRepository.kt`，实现图片多级兜底、按库最新资源按 movies/tvshows 分流、tvshows 按 Series 聚合和 `loadLibraryContent()`。

## 3. GREEN: 首页与媒体库列表 UI
- [√] 3.1 扩展 `app/src/main/java/com/embytv/ui/home/HomeDashboardModels.kt`，支持 Series 卡片、剩余集数角标、媒体库卡片可进入列表页。
- [√] 3.2 扩展 `app/src/main/java/com/embytv/ui/components/CinematicComponents.kt`，让 `MediaPosterCard` 支持额外角标展示，并保证文字不溢出。
- [√] 3.3 扩展 `app/src/main/java/com/embytv/ui/home/HomeUiState.kt` 和 `HomeViewModel.kt`，增加 `selectedLibrary`、`libraryContent`、loading/error 状态，以及 `openLibrary`、`closeLibrary`、`retryLibrary`。
- [√] 3.4 改造 `app/src/main/java/com/embytv/ui/home/HomeScreen.kt`，媒体库卡片 OK/Enter 进入 `LibraryContentScreen`；列表页支持 Back 返回首页、加载/空/错误状态和遥控器焦点。
- [√] 3.5 保持播放入口: 列表页 Movie 卡片可播放；Series 卡片在详情页未实现前显示“剧集详情暂未支持”或进入后续方案，不允许 OK 空响应。

## 4. 集成、安全与性能检查
- [√] 4.1 检查首页每个媒体库最新资源仍固定 Limit=8，媒体库列表首屏固定 Limit=60，不做全量 Episode 扫描。
- [√] 4.2 检查所有图片 URL 不包含 token、密码或完整播放 URL，缺图时仅显示占位。
- [√] 4.3 检查 TV 遥控器路径: 首页媒体库 OK 进入列表、Back 返回首页、列表项 OK 有明确播放或禁用反馈。

## 5. 文档更新
- [√] 5.1 更新 `helloagents/main/wiki/api.md`，补充 `Items/Latest` 的 `ParentId/GroupItems`、媒体库列表查询和图片字段兜底策略。
- [√] 5.2 更新 `helloagents/main/wiki/data.md`，补充 Series 聚合字段、未播放集数、`EmbyLibraryContent` 和角标字段。
- [√] 5.3 更新 `helloagents/main/wiki/modules/ui.md`，记录媒体库列表页、封面兜底、剧集维度横排和剩余集数角标。
- [√] 5.4 更新 `helloagents/main/CHANGELOG.md`。

## 6. 验证
- [√] 6.1 GREEN: 运行 `.\gradlew.bat :app:testDebugUnitTest`，确认新增 Repository/Mapper/ViewModel 测试通过。
- [√] 6.2 VERIFY: 运行 `.\gradlew.bat :app:assembleDebug`，确认 Debug 构建通过。
- [-] 6.3 TDD-EXEMPT: 真实 TV + Emby 手工验收，原因: 需要真实媒体库图片字段和遥控器交互；替代验证: 安装 APK 后确认首页封面、媒体库进入列表、Back 返回、剧集库按 Series 展示和剩余集数角标。
> 备注: 当前执行环境无法操作真实 TV 遥控器和用户 Emby 媒体库，已用单元测试、Debug 构建和静态检查替代验证。
