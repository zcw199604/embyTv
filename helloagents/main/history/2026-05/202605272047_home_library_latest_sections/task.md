# 任务清单: 首页媒体库与最新资源展示增强

目录: `helloagents/main/plan/202605272047_home_library_latest_sections/`

---

## 并行子代理标注

- 并行组 A: 任务 [1.1, 2.1, 2.2, 2.3]；允许写入: `app/src/main/java/com/embytv/data/remote/`, `app/src/main/java/com/embytv/data/repository/`, `app/src/main/java/com/embytv/domain/model/`, `app/src/test/java/com/embytv/data/`；冲突域: Dashboard 数据模型和 Repository 聚合；验证: `.\gradlew.bat :app:testDebugUnitTest`
- 并行组 B: 任务 [1.2, 3.1, 3.2, 3.3]；允许写入: `app/src/main/java/com/embytv/ui/home/`, `app/src/main/java/com/embytv/ui/components/`, `app/src/test/java/com/embytv/ui/home/`；冲突域: 首页 UI 模型和 Compose 布局；验证: `.\gradlew.bat :app:testDebugUnitTest`
- 不可并行任务: [4.1, 5.1, 6.1, 7.1, 7.2, 7.3]；原因: 安全检查、文档同步和最终集成验证需在数据/UI 合并后执行。

---

## 0. 方案边界确认
- [√] 0.1 确认本次只增强登录后首页展示，不实现媒体库详情页、分页浏览、搜索筛选和详情页。
- [√] 0.2 确认所有可见内容来自 Emby API，不新增假媒体库、假封面、假缩略图或假剧集信息。
- [√] 0.3 确认最小改动策略: 保持现有 MVVM、Repository、Compose 组件和播放入口，不做无关重构。

## 1. RED: 数据和 UI 映射测试
- [√] 1.1 RED: 更新 `app/src/test/java/com/embytv/data/repository/EmbyRepositoryDashboardTest.kt`，断言 `loadHomeDashboard()` 会按每个 View 的 `ParentId` 拉取库内最新资源，并映射到 `libraryLatestSections`，验证 why.md#需求-每个媒体库最新资源-场景-按库展示最新内容。
- [√] 1.2 RED: 更新 `app/src/test/java/com/embytv/ui/home/HomeDashboardMapperTest.kt`，断言媒体库卡片展示真实名称/封面，继续观看 Episode 显示缩略图和季集信息，库最新资源生成独立 section，验证 why.md#需求-继续观看卡片上下文增强-场景-episode-显示剧集信息。
- [√] 1.3 RED: 增加图片 URL 选择测试，断言 `Primary/Thumb/Backdrop` 字段缺失时按真实可用图片兜底，不生成假图，验证 why.md#需求-媒体库真实展示-场景-媒体库名称与封面。

## 2. GREEN: Emby 数据模型与 Repository
- [√] 2.1 扩展 `app/src/main/java/com/embytv/data/remote/dto/EmbyItemDtos.kt` 和 `app/src/main/java/com/embytv/domain/model/MediaItemSummary.kt`，新增 `ThumbImageTags`、`BackdropImageTags`、`ParentId`、`ParentIndexNumber`、`IndexNumber`、缩略图/背景图字段，依赖任务1.1。
- [√] 2.2 扩展 `app/src/main/java/com/embytv/data/repository/EmbyStreamUrlBuilder.kt`，支持 Primary、Thumb、Backdrop 图片 URL 构造和真实图片优先级选择，依赖任务2.1。
- [√] 2.3 扩展 `app/src/main/java/com/embytv/data/remote/EmbyApi.kt` 和 `app/src/main/java/com/embytv/data/repository/EmbyRepository.kt`，新增按 `ParentId + SortBy=DateCreated + Limit` 拉取每库最新资源，并写入 `EmbyHomeDashboard.libraryLatestSections`，依赖任务2.1、2.2。
- [√] 2.4 检查 Repository 仍避免首页全量拉取 Movie/Episode，按库最新每库限制固定条数，验证 why.md#需求-每个媒体库最新资源-场景-按库展示最新内容。

## 3. GREEN: 首页 UI 模型与布局
- [√] 3.1 扩展 `app/src/main/java/com/embytv/ui/home/HomeDashboardModels.kt`，新增库最新资源 UI section，Episode 副标题组合 `seriesName/seasonName/SxxExx/productionYear` 等真实字段，依赖任务2.1。
- [√] 3.2 调整 `app/src/main/java/com/embytv/ui/components/CinematicComponents.kt` 的媒体卡片图片选择能力，优先使用缩略图/背景图，缺失时显示占位，不使用假图，依赖任务3.1。
- [√] 3.3 改造 `app/src/main/java/com/embytv/ui/home/HomeScreen.kt`，使用纵向 LazyColumn 渲染媒体库、继续观看、每个媒体库最新资源横排，并保持遥控器焦点路径，依赖任务3.1、3.2。
- [√] 3.4 检查 `MiniPlayerBar` 和底部提示不遮挡新增分区；必要时调整底部 padding，但不扩大到整体视觉重构。

## 4. 安全与性能检查
- [√] 4.1 检查日志、错误消息、测试 fixture 和知识库不包含真实私有媒体标题、访问令牌、密码或完整播放 URL。
- [√] 4.2 检查每库最新资源请求数量受限，空媒体库不显示空 section，不回退为假数据。
- [√] 4.3 检查 DTO 字段可空，图片和季集字段缺失时 UI 仍稳定显示占位或保守文案。

## 5. 文档更新
- [√] 5.1 更新 `helloagents/main/wiki/api.md`，补充按库最新资源接口参数和图片类型 URL。
- [√] 5.2 更新 `helloagents/main/wiki/data.md`，补充 `EmbyLibraryLatestSection`、扩展后的 `MediaItemSummary` 图片/季集字段。
- [√] 5.3 更新 `helloagents/main/wiki/modules/ui.md`，记录首页媒体库封面、继续观看剧集信息、各媒体库最新资源分区。
- [√] 5.4 更新 `helloagents/main/CHANGELOG.md`。

## 6. 测试
- [√] 6.1 GREEN: 运行 `.\gradlew.bat :app:testDebugUnitTest`，确认新增 Repository/Mapper 测试通过。
- [√] 6.2 VERIFY: 运行 `.\gradlew.bat :app:assembleDebug`，确认 Debug 构建通过。
- [-] 6.3 TDD-EXEMPT: 真实 TV 视觉与遥控器手工验收，原因: 当前环境未连接真实 TV/模拟器交互；替代验证: 构建通过后安装到 TV，确认媒体库名称/封面、继续观看缩略图/集信息、各媒体库最新资源分区均显示并可横向移动焦点。

---

## 执行总结

- RED: Repository 与首页 Mapper 测试先补充目标行为断言，生产代码修改前缺失字段和 section 能触发失败。
- GREEN: 已实现 Emby 按库最新资源聚合、Thumb/Backdrop 图片映射、Episode 季集信息展示和首页纵向分区布局。
- VERIFY: `.\gradlew.bat :app:testDebugUnitTest` 与 `.\gradlew.bat :app:assembleDebug` 通过。
- 未执行: 真实 TV 视觉与遥控器手工验收，原因是当前环境未连接真实设备/模拟器。
