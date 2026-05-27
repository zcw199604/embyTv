# 任务清单: 全量替换为 Emby 真实数据

目录: `helloagents/main/history/2026-05/202605271602_emby_real_data_replacement/`

---

## 并行子代理标注

- 并行组 A: 任务 [1.1, 2.1, 2.2]；允许写入: `app/src/main/java/com/embytv/data/remote/`, `app/src/main/java/com/embytv/data/repository/`, `app/src/test/java/com/embytv/data/`；冲突域: Emby DTO/API/Repository；验证: `.\gradlew.bat :app:testDebugUnitTest`
- 并行组 B: 任务 [1.2, 3.1, 3.2]；允许写入: `app/src/main/java/com/embytv/domain/model/`, `app/src/main/java/com/embytv/ui/home/`, `app/src/test/java/com/embytv/ui/home/`；冲突域: Home dashboard 模型；验证: `.\gradlew.bat :app:testDebugUnitTest`
- 不可并行任务: [4.1, 4.2, 5.1, 7.1]；原因: 播放入口依赖 Repository 和领域模型，最终验证需集成后执行。

---

## 0. 方案边界确认
- [√] 0.1 确认本次只替换当前页面可见假数据，不实现分页详情页、音轨/字幕真实切换和播放进度回写。
- [√] 0.2 确认知识库已记录真实 Emby API 探测结构，且不包含 token、密码或具体私有媒体标题。
- [√] 0.3 确认最小改动策略: 保持现有 MVVM、Media3 播放和 AkDanmaku 层，不做无关重构。

## 1. RED: 真实数据映射测试
- [√] 1.1 RED: 新增 Repository/Mapper 测试，覆盖 `Views`、库数量、`Resume`、`Latest` 和 `PlaybackInfo` DTO 到领域模型映射，先确认当前缺失模型导致失败。
- [√] 1.2 RED: 更新 `HomeDashboardMapperTest`，断言媒体库来自 Emby views、进度来自 `UserData`，不再有 `seededProgress()` 和固定 Anime 卡片。
- [√] 1.3 RED: 新增播放器详情格式测试，断言 OSD 标签由 `PlaybackInfo.MediaStreams` 生成，不再硬编码 `HEVC · 4K HDR`。
- [√] 1.4 RED: 增加样例入口移除测试或结构检查，确认生产入口不再暴露 `samplePlaybackSource`。

## 2. GREEN: Emby API 与 Repository
- [√] 2.1 扩展 `EmbyApi.kt` 和 DTO，新增 `getViews`、`getItemsByParent`、`getResumeItems`、`getLatestItems`、`getPlaybackInfo`。
- [√] 2.2 扩展 `EmbyRepository`，新增 `loadHomeDashboard()` 和 `createPlaybackSourceWithDetails()`，避免启动时全量拉取 45661 条媒体。
- [√] 2.3 更新 `EmbyStreamUrlBuilder` 或播放源构造，保留真实 stream URL，但错误日志不得输出完整 `api_key` URL。

## 3. GREEN: 首页真实数据
- [√] 3.1 扩展领域模型 `MediaItemSummary`、新增 `EmbyLibrarySummary`、`EmbyHomeDashboard`。
- [√] 3.2 改造 `HomeDashboardMapper`，媒体库、继续观看、最近入库全部来自 Repository 聚合结果。
- [√] 3.3 改造 `HomeUiState` 和 `HomeViewModel`，登录成功后加载 dashboard，而不是单一 `items` 列表。
- [√] 3.4 改造 `HomeScreen`，移除 `样例播放` 按钮、MiniPlayer 假文案和空态里的样例提示。

## 4. GREEN: 播放器真实数据
- [√] 4.1 扩展 `PlaybackSource`，携带真实 `PlaybackDetails`。
- [√] 4.2 改造播放入口，点击媒体时先加载 `PlaybackInfo` 再进入播放器。
- [√] 4.3 改造 `PlayerScreen`，顶部副标题、右上角质量标签、Audio/Subtitles 状态来自 `PlaybackDetails`。
- [√] 4.4 移除 `AppContainer.samplePlaybackSource()`、Big Buck Bunny 和硬编码样例弹幕。

## 5. 安全与性能检查
- [√] 5.1 检查首页没有全量拉取 Movie/Episode 作为首屏数据源。
- [√] 5.2 检查日志、错误消息和知识库不包含 token、密码、完整播放 URL 或具体私有媒体标题。
- [√] 5.3 检查 DTO 字段可空，接口为空时 UI 显示真实空态而不是假数据。

## 6. 文档更新
- [√] 6.1 更新 `helloagents/main/wiki/api.md`，补充最终落地接口和参数。
- [√] 6.2 更新 `helloagents/main/wiki/data.md`，补充最终领域模型。
- [√] 6.3 更新 `helloagents/main/wiki/modules/ui.md`，记录首页和播放器真实数据约定。
- [√] 6.4 更新 `helloagents/main/CHANGELOG.md`。

## 7. 验证
- [√] 7.1 REFACTOR: 相关测试通过后整理命名和职责边界，不扩大到分页/字幕切换。
- [√] 7.2 VERIFY: 使用 JDK 17 运行 `.\gradlew.bat :app:testDebugUnitTest`。
- [√] 7.3 VERIFY: 使用 JDK 17 运行 `.\gradlew.bat :app:assembleDebug`。
- [-] 7.4 手工验收: 使用测试服务器登录，确认首页媒体库、继续观看、最近入库、播放器 OSD 均来自 Emby。
  > 备注: 当前执行环境完成了真实接口结构探测、单元测试和 Debug 构建；未在 Android TV 真机/模拟器上做交互手工验收。

## 执行总结

- RED 证据: `HomeDashboardMapperTest`、`PlaybackDetailsTest`、`AppContainerContractTest` 和 `EmbyRepositoryDashboardTest` 覆盖真实数据映射、播放详情标签和样例入口移除。
- GREEN/VERIFY 证据: `.\gradlew.bat :app:testDebugUnitTest` 与 `.\gradlew.bat :app:assembleDebug` 均通过。
- TDD-EXEMPT: 真实 TV 遥控器/播放链路手工验收依赖设备或模拟器交互，本轮未执行，保留为后续验证项。
