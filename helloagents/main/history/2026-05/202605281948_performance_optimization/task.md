# 任务清单: TV 客户端性能优化

目录: `helloagents/main/plan/202605281948_performance_optimization/`

---

## 并行子代理标注（可选）

- 并行组 A: 任务 [2.1, 2.2, 2.3]；允许写入: `app/src/main/java/com/embytv/data/remote/EmbyApiFactory.kt`, `app/src/test/java/com/embytv/data/remote/*`；冲突域: API service 创建；验证: `.\gradlew.bat :app:testDebugUnitTest --tests *EmbyApiFactory*`
- 并行组 B: 任务 [3.1, 3.2, 3.3]；允许写入: `app/src/main/java/com/embytv/data/repository/EmbyStreamUrlBuilder.kt`, `app/src/test/java/com/embytv/data/repository/EmbyStreamUrlBuilderTest.kt`；冲突域: 图片 URL 构造；验证: `.\gradlew.bat :app:testDebugUnitTest --tests *EmbyStreamUrlBuilderTest`
- 不可并行任务: [1.*, 4.*, 5.*, 6.*, 7.*]；原因: 启动顺序和 Dashboard 聚合影响主 UI 状态，最终验证需在集成后执行。

---

## 0. 方案边界确认
- [√] 0.1 确认本次任务仅覆盖 why.md 的范围内切片，范围外内容不进入实现。
- [√] 0.2 确认不变更 Emby 登录协议、不保存密码、不新增持久化缓存数据库。
- [√] 0.3 确认最小改动策略: 不做无关 UI 重构、目录搬迁、依赖升级或公共模型重命名。

---

## 1. RED: 启动恢复顺序
- [-] 1.1 为 `HomeViewModel` 或可抽取的启动控制逻辑补测试，验证有保存凭证时不启动 `MobileSetupSyncServer`。
> 备注: 当前 `HomeViewModel` 直接依赖 Android ViewModel 与 `MobileSetupSyncServer`，缺少 JVM 单测友好的同步服务接口；本次采用最小代码复核和全量构建验证，后续如继续增强可先抽象 sync service 接口。
- [-] 1.2 为凭证失效路径补测试或记录 TDD-EXEMPT，验证 token 加载 Dashboard 失败后会清除凭证并启动同步服务。
> 备注: 凭证失效路径保留原有清凭证、重置 deviceId、启动同步服务逻辑；受限原因同 1.1。

## 2. GREEN: 启动恢复顺序优化
- [√] 2.1 修改 `app/src/main/java/com/embytv/ui/home/HomeViewModel.kt`，启动时先 `loadSavedCredential()`，无凭证时再 `startMobileSetupSync()`。
- [√] 2.2 保持凭证有效路径使用保存的 `accessToken` 构造 `EmbySession`，不使用用户名密码重新登录。
- [√] 2.3 保持凭证失效路径: 清除本地凭证、重置 deviceId、启动手机同步服务并提示重新登录。

## 3. RED/GREEN: EmbyApi service 复用
- [√] 3.1 RED: 为 `EmbyApiFactory` 增加测试或 fake 统计，验证相同 `baseUrl + accessToken` 复用 service，不同 token 或 baseUrl 不复用。
- [√] 3.2 GREEN: 在 `app/src/main/java/com/embytv/data/remote/EmbyApiFactory.kt` 中按 normalized baseUrl 与 accessToken 缓存 `EmbyApi`。
- [√] 3.3 检查登出/切服务器/token 变化不会误用旧 token；必要时提供 cache clear 或 key 隔离。

## 4. RED/GREEN: 图片 URL 尺寸化
- [√] 4.1 RED: 扩展 `EmbyStreamUrlBuilderTest`，验证 poster/backdrop/detail 图片 URL 包含 `MaxWidth`、`MaxHeight` 和 `Quality`。
- [√] 4.2 GREEN: 在 `app/src/main/java/com/embytv/data/repository/EmbyStreamUrlBuilder.kt` 增加图片 profile 或尺寸参数。
- [√] 4.3 更新媒体库、媒体卡片和详情图 URL 构造调用，首页使用较小尺寸，详情页使用较大尺寸；缺图兜底保持原逻辑。

## 5. RED/GREEN: 首页 Dashboard 加载优化
- [√] 5.1 RED: 为 `EmbyRepository.loadHomeDashboard()` 增加测试，覆盖多媒体库下库计数和按库 latest 的请求聚合结果。
- [√] 5.2 GREEN: 将库计数与按库 latest 改为受控并发加载；优先限制并发数，避免媒体库数量过多时无限并发。
- [√] 5.3 如果分阶段加载改动过大，则先实现受控并发；将分阶段 UI 状态作为后续方案记录。
- [√] 5.4 保持核心失败仍返回错误；非核心分区失败是否降级展示需在代码中保持一致策略并写入测试。
> 备注: 本次先实现受控并发，未引入分阶段 UI 状态，避免扩大 HomeUiState 和 UI loading 语义。

## 6. 播放上报队列化评估
- [√] 6.1 复核 `PlaybackReportingCoordinator` 当前 10 秒节流、seek/pause/stop 即时上报逻辑。
- [-] 6.2 若实现复杂度可控，新增播放上报队列/actor，保证事件顺序、合并过期 progress、stop 优先发送。
> 备注: 当前已有 10 秒进度节流；actor 会牵涉播放器退出 flush、stop 优先级和生命周期测试，风险高于本轮收益。
- [√] 6.3 若实现会扩大播放器生命周期风险，标记为 [-] 并创建后续独立方案建议；本次只依赖 API service 复用降低上报开销。

## 7. 安全检查
- [√] 7.1 检查 token 不进入日志、错误文案或明文新增存储。
- [√] 7.2 检查 API service 缓存 key 不导致旧 token 泄漏到新服务器或新用户。
- [√] 7.3 检查图片 URL 仍只包含 Emby 必需查询参数，不输出完整带 `api_key` 的播放 URL。

## 8. 文档更新
- [√] 8.1 更新 `helloagents/main/wiki/modules/data.md`，记录 Dashboard 受控并发、API service 复用和图片尺寸化。
- [√] 8.2 更新 `helloagents/main/wiki/modules/ui.md`，记录启动恢复顺序和首页加载体验。
- [√] 8.3 更新 `helloagents/main/wiki/arch.md`，追加 ADR-011 和 ADR-012 索引。
- [√] 8.4 更新 `helloagents/main/CHANGELOG.md`。

## 9. 验证
- [√] 9.1 VERIFY: 运行 `.\gradlew.bat :app:testDebugUnitTest`。
- [√] 9.2 VERIFY: 运行 `.\gradlew.bat :app:assembleDebug`。
- [-] 9.3 TDD-EXEMPT: 真机性能体感验收，原因: 需要 Android TV/Emby 真实网络环境；替代验证: 安装 APK 后验证有 token 冷启动、首页加载、图片清晰度和播放上报后台状态。
> 备注: 当前环境未连接 Android TV 真机；已完成 JVM 单元测试和 debug 构建验证。
