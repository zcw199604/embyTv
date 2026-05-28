# 任务清单: 媒体详情页与电视剧季列表

目录: `helloagents/main/plan/202605281300_media_detail_seasons/`

---

## 0. 方案边界确认
- [√] 0.1 确认本次只实现 Movie/Series 详情页、Series 季列表和季内 Episode 列表，不实现收藏管理、演员详情、推荐和分页。
- [√] 0.2 确认 Movie/Series 卡片 OK 行为改为进入详情页；Episode 仍可直接播放。
- [√] 0.3 确认最小改动策略: 继续沿用现有 HomeViewModel/HomeScreen，不引入 Navigation 框架或本地数据库。

## 1. RED: 数据层详情接口测试
- [√] 1.1 RED: 新增 `app/src/test/java/com/embytv/data/repository/EmbyRepositoryMediaDetailTest.kt`，断言 `loadMediaDetail()` 会调用 `GET Users/{userId}/Items/{itemId}` 并携带详情字段。
- [√] 1.2 RED: 在同一测试中断言 Movie 详情映射 `Overview`、`People`、`Genres`、`ProductionYear`、评分和图片 URL。
- [√] 1.3 RED: 在同一测试中断言 Series 详情会调用 `GET Shows/{seriesId}/Seasons`，并将 Season 的 `UserData.UnplayedItemCount` 映射为季角标来源。
- [√] 1.4 RED: 在同一测试中断言 `loadSeasonEpisodes()` 会调用 `GET Shows/{seriesId}/Episodes?SeasonId=...`，并映射 Episode 的 SxxExx、缩略图和播放进度。

## 2. RED: UI 映射与状态测试
- [√] 2.1 RED: 新增 `app/src/test/java/com/embytv/ui/home/HomeMediaDetailMapperTest.kt`，断言详情页 UI 展示标题、简介、演员、类型、年份、评分。
- [√] 2.2 RED: 断言 Series 详情 UI 季列表显示季名称、集数文案和“剩 n 集”角标；`unplayedItemCount <= 0` 时不显示角标。
- [√] 2.3 RED: 断言 `MediaDetailUiState` 支持打开详情、选择季、从季内剧集 Back 返回季列表、关闭详情返回上一级。

## 3. GREEN: API、DTO 与领域模型
- [√] 3.1 扩展 `app/src/main/java/com/embytv/data/remote/EmbyApi.kt`，新增详情、季列表和季内剧集接口。
- [√] 3.2 扩展 `app/src/main/java/com/embytv/data/remote/dto/EmbyItemDtos.kt`，新增 People、Genres、Studios、Rating、PremiereDate 等可空字段。
- [√] 3.3 扩展 `app/src/main/java/com/embytv/domain/model/MediaItemSummary.kt`，新增 `EmbyMediaDetail`、`EmbySeasonSummary`、`EmbySeasonEpisodes`、`EmbyPersonSummary`。
- [√] 3.4 扩展 `app/src/main/java/com/embytv/data/repository/EmbyRepository.kt`，实现详情聚合、季列表映射和季内剧集读取。

## 4. GREEN: ViewModel 状态机
- [√] 4.1 扩展 `app/src/main/java/com/embytv/ui/home/HomeDashboardModels.kt` 和 `HomeUiState.kt`，新增详情 UI 状态、详情 UI model、季 UI model。
- [√] 4.2 扩展 `app/src/main/java/com/embytv/ui/home/HomeViewModel.kt`，新增 `openMediaDetail()`、`closeMediaDetail()`、`openSeasonEpisodes()`、`backFromDetail()`、`retryMediaDetail()`。
- [√] 4.3 确保详情页打开时关闭抽屉或不与媒体库/收藏 overlay 冲突；Back 优先处理详情内部层级。

## 5. GREEN: Compose 详情页与遥控器路径
- [√] 5.1 在 `app/src/main/java/com/embytv/ui/home/HomeScreen.kt` 新增 `MediaDetailScreen`，支持加载、错误、空态、Movie 详情和 Series 详情。
- [√] 5.2 将首页横排、媒体库列表和收藏列表中的 Movie/Series 卡片 OK 改为进入详情页；Episode 保持直接播放。
- [√] 5.3 Movie 详情页提供可聚焦“播放”按钮，OK 后调用既有播放入口。
- [√] 5.4 Series 详情页展示可聚焦季列表，每季显示图片、名称、集数和剩余未播放角标。
- [√] 5.5 选择季后展示 Episode 列表，Episode 卡片 OK 播放；Back 从 Episode 列表返回季列表。

## 6. 集成、安全与性能检查
- [√] 6.1 检查详情页不会在首页/媒体库首屏预加载所有详情；Season/Episode 按需加载。
- [√] 6.2 检查错误文案、UI 文案和日志不包含 token、密码或完整播放 URL。
- [√] 6.3 检查所有新增 DTO 字段可空处理，People/Genres/Rating 缺失时 UI 不崩溃。
- [√] 6.4 检查 TV 遥控器路径: 卡片 OK 进详情、Movie 播放、Series 季切换、Episode 播放、Back 层级返回。

## 7. 文档更新
- [√] 7.1 更新 `helloagents/main/wiki/api.md`，补充详情、Seasons、Episodes API。
- [√] 7.2 更新 `helloagents/main/wiki/data.md`，补充媒体详情、人物、季、季内剧集模型。
- [√] 7.3 更新 `helloagents/main/wiki/modules/data.md`，记录详情聚合与按需加载策略。
- [√] 7.4 更新 `helloagents/main/wiki/modules/ui.md`，记录详情页、季列表和遥控器操作。
- [√] 7.5 更新 `helloagents/main/CHANGELOG.md`。

## 8. 验证
- [√] 8.1 GREEN: 运行 `.\gradlew.bat :app:testDebugUnitTest`，确认新增 Repository / Mapper / State 测试通过。
- [√] 8.2 VERIFY: 运行 `.\gradlew.bat :app:assembleDebug`，确认 Debug 构建通过。
- [-] 8.3 TDD-EXEMPT: 真实 TV + Emby 详情手工验收，原因: 需要真实 People/Season/Episode 数据和遥控器交互；替代验证: 安装 APK 后打开 Movie/Series 详情、切换季、播放 Episode、确认 Back 返回层级。
> 备注: 当前开发环境无法直接完成真实 TV 遥控器手工验收，本次已用 Repository/Mapper/State 单元测试和 Debug 构建替代验证。
