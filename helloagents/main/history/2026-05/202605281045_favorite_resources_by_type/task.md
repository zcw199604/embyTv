# 任务清单: 收藏资源按电影/电视剧分组展示

目录: `helloagents/main/plan/202605281045_favorite_resources_by_type/`

---

## 0. 方案边界确认
- [√] 0.1 确认本次只覆盖收藏资源展示界面，不实现收藏增删、批量管理、筛选排序和详情页。
- [√] 0.2 确认收藏数据采用 `Filters=IsFavorite` 单次查询后本地分组为电影/电视剧，不把多个弱相关查询混入本次方案。
- [√] 0.3 确认最小改动策略: 保持现有 HomeViewModel/HomeScreen 结构，不引入 Navigation 框架和本地数据库。

## 1. RED: 收藏查询与分组测试
- [√] 1.1 RED: 新增或更新 `app/src/test/java/com/embytv/data/repository/EmbyRepositoryFavoritesTest.kt`，断言收藏查询会携带 `Filters=IsFavorite`、`IncludeItemTypes=Movie,Series,Episode`、`EnableUserData=true`。
- [√] 1.2 RED: 新增或更新 Repository 测试，断言收藏电视剧侧会把 Episode 收藏聚合为 Series，且聚合后的 Series 保留图片 URL 和剧集名字，避免同剧重复卡片和空白标题。
- [√] 1.3 RED: 新增 `app/src/test/java/com/embytv/ui/home/HomeFavoritesMapperTest.kt`，断言电影/电视剧两个维度的分组标题、卡片图片、资源名字、卡片文案和空态文案正确；缺图时允许占位但名字不能为空。
- [√] 1.4 RED: 新增 `HomeFavoritesStateTest` 或 ViewModel 测试，断言进入收藏页、切换分组、Back 返回首页的状态机行为正确。

## 2. GREEN: 数据层与领域模型
- [√] 2.1 扩展 `app/src/main/java/com/embytv/data/remote/EmbyApi.kt`，支持收藏过滤查询参数。
- [√] 2.2 扩展 `app/src/main/java/com/embytv/data/repository/EmbyRepository.kt`，新增收藏聚合读取方法和电影/电视剧分组逻辑。
- [√] 2.3 扩展 `app/src/main/java/com/embytv/domain/model/MediaItemSummary.kt`，增加收藏展示聚合所需模型（如 `EmbyFavoriteDashboard` / 分组模型）。

## 3. GREEN: 首页与收藏页 UI
- [√] 3.1 扩展 `app/src/main/java/com/embytv/ui/home/HomeUiState.kt` 和 `HomeViewModel.kt`，增加收藏页状态、当前分组和打开/关闭方法。
- [√] 3.2 新增收藏页 Composable，支持电影/电视剧分组切换、加载/空/错误状态和遥控器焦点；每个媒体资源卡片必须显示图片区域和资源名字。
- [√] 3.3 改造 `app/src/main/java/com/embytv/ui/home/HomeScreen.kt` 和 `CinematicComponents.kt`，增加收藏入口并复用现有媒体卡片。
- [√] 3.4 保持播放入口: 电影收藏卡片可播放；电视剧收藏卡片在详情页未实现前显示明确反馈，不允许 OK 空响应。

## 4. 集成、安全与性能检查
- [√] 4.1 检查收藏页首屏加载不会影响首页启动性能，收藏查询保持固定上限，不做全量无限拉取。
- [√] 4.2 检查所有 API / 图片 / 错误文案不包含 token、密码或完整播放 URL；收藏卡片缺图时只显示占位图，不能拼接假图片地址。
- [√] 4.3 检查 TV 遥控器路径: 收藏入口可进入、分组可切换、Back 返回、焦点恢复正常。

## 5. 文档更新
- [√] 5.1 更新 `helloagents/main/wiki/api.md`，补充 `Filters=IsFavorite` 与收藏查询策略。
- [√] 5.2 更新 `helloagents/main/wiki/data.md`，补充收藏聚合模型与电影/电视剧分组说明。
- [√] 5.3 更新 `helloagents/main/wiki/modules/ui.md`，记录收藏页、分组切换和遥控器操作。
- [√] 5.4 更新 `helloagents/main/CHANGELOG.md`。

## 6. 验证
- [√] 6.1 GREEN: 运行 `.\gradlew.bat :app:testDebugUnitTest`，确认新增 Repository / Mapper / ViewModel 测试通过。
- [√] 6.2 VERIFY: 运行 `.\gradlew.bat :app:assembleDebug`，确认 Debug 构建通过。
- [√] 6.3 TDD-EXEMPT: 真实 TV + Emby 收藏数据手工验收，原因: 需要真实收藏状态和遥控器交互；替代验证: 安装 APK 后确认收藏页可打开、电影/电视剧切换正常、每个资源显示图片区域和名字、Back 返回、空态和错误态可见。
