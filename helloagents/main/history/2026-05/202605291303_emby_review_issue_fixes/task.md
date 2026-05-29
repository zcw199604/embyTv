# 任务清单: Emby TV 审查问题修复

目录: `helloagents/main/plan/202605291303_emby_review_issue_fixes/`

---

## 并行子代理标注

启用条件: 任务可按模块分组并行审查或实现，但涉及同一文件的任务不可并行写入。

- 并行组 A: 任务 [1.1, 1.2]；允许写入: `app/src/main/java/com/embytv/ui/player/PlayerScreen.kt`, `app/src/main/java/com/embytv/ui/player/PlayerOsdState.kt`, `app/src/test/java/com/embytv/ui/player/**`；冲突域: 播放器上报；验证: `.\gradlew.bat :app:testDebugUnitTest`
- 并行组 B: 任务 [2.1, 2.2]；允许写入: `app/src/main/java/com/embytv/ui/components/CinematicComponents.kt`, `app/src/main/java/com/embytv/data/repository/EmbyStreamUrlBuilder.kt`, `app/src/main/java/com/embytv/data/repository/EmbyRepository.kt`, 相关测试；冲突域: 图片认证与认证头；验证: `.\gradlew.bat :app:testDebugUnitTest`
- 并行组 C: 任务 [3.1, 5.1]；允许写入: `app/src/main/java/com/embytv/ui/home/HomeViewModel.kt`, `app/src/main/java/com/embytv/ui/home/HomeScreen.kt`, `app/src/main/java/com/embytv/ui/home/HomeUiState.kt`, 相关测试；冲突域: 首页状态；验证: `.\gradlew.bat :app:testDebugUnitTest`
- 不可并行任务: [6.1, 7.1, 8.1]；原因: 安全检查、文档同步和最终验证需在集成后执行。

---

## 0. 方案边界确认
- [√] 0.1 确认本次任务仅覆盖 why.md 的范围内切片，范围外内容不进入实现。
- [√] 0.2 确认 how.md 的设计边界完整，尤其是模块职责、接口契约、数据边界和依赖边界。
- [√] 0.3 确认最小改动策略: 不做无关重构、目录搬迁、依赖升级或公共 API 重命名。

---

## 1. 播放切集上报修复
- [√] 1.1 在 `app/src/main/java/com/embytv/ui/player/PlayerScreen.kt` 中修复 `LaunchedEffect` / `DisposableEffect` 对旧 `playbackSource` 的捕获问题，验证 why.md#需求-播放切集上报正确-场景-自动下一集。
- [√] 1.2 在 `app/src/main/java/com/embytv/ui/player/PlayerScreen.kt` 中为遥控器上一集/下一集切换前补充当前媒体 stopped 上报，验证 why.md#需求-播放切集上报正确-场景-遥控器手动切集，依赖任务 1.1。
- [√] 1.3 为播放上报切集行为补充测试或可替代验证记录，覆盖 started/progress/stopped 不错绑旧 itemId，依赖任务 1.2。
  > 备注: 播放器 Compose/Media3 切集路径以代码审查和 `PlaybackReportingCoordinator` 幂等测试作为替代验证；未新增仪器测试。

## 2. 认证图片加载修复
- [√] 2.1 在 `app/src/main/java/com/embytv/ui/components/CinematicComponents.kt` 或图片加载入口中引入可携带 Emby 认证 header 的图片模型，验证 why.md#需求-认证封面图片可显示-场景-认证服务器加载封面。
- [√] 2.2 在 `app/src/main/java/com/embytv/data/repository/EmbyStreamUrlBuilder.kt` / `EmbyRepository.kt` 中整理图片 URL 与认证信息边界，避免 Token 进入可见 URL 或日志，依赖任务 2.1。
- [√] 2.3 使用真实 Emby 或可替代 mock 验证媒体库、电影、电视剧、剧集封面请求包含认证信息，依赖任务 2.2。
  > 备注: 单元测试验证认证头构造；真实 TV/Emby 图片渲染仍需设备侧手工验收。

## 3. 搜索取消修复
- [√] 3.1 在 `app/src/main/java/com/embytv/ui/home/HomeViewModel.kt` 中将 debounce 与搜索请求放入同一可取消 `Job`，或在写回前校验当前 query，验证 why.md#需求-搜索结果与当前关键词一致-场景-快速连续输入。
- [√] 3.2 增加搜索并发/取消单元测试，覆盖旧查询慢返回不能覆盖新查询结果，依赖任务 3.1。

## 4. 危险操作二次确认
- [√] 4.1 在 `app/src/main/java/com/embytv/ui/home/HomeUiState.kt` 中增加确认弹窗或确认态模型，用于删除凭证与清除播放进度。
- [√] 4.2 在 `app/src/main/java/com/embytv/ui/home/HomeScreen.kt` 中实现 TV 遥控器友好的确认 UI，默认焦点放在取消或安全按钮，Back 取消，验证 why.md#需求-危险操作需要确认-场景-删除保存凭证。
- [√] 4.3 在 `app/src/main/java/com/embytv/ui/home/HomeViewModel.kt` 中把删除凭证和清除播放进度改为确认后执行，验证 why.md#需求-危险操作需要确认-场景-清除播放进度，依赖任务 4.1 和 4.2。
- [√] 4.4 补充确认/取消路径的单元测试或手工验证记录，依赖任务 4.3。
  > 备注: 单元测试覆盖确认状态不含 token；Compose 焦点和 Back 路径通过编译与代码审查验证，仍建议真机遥控器验收。

## 5. 版本号一致性
- [√] 5.1 在 `app/src/main/java/com/embytv/data/repository/EmbyRepository.kt` 中移除 `X-Emby-Authorization Version` 硬编码，改为 `BuildConfig.VERSION_NAME` 或集中版本常量。
- [√] 5.2 在 `app/src/main/java/com/embytv/ui/home/HomeScreen.kt` 中移除 UI 版本硬编码，展示同一版本来源，验证 why.md#需求-版本号一致-场景-版本显示与后台会话。
- [√] 5.3 补充版本号构造测试或静态校验，确保 `0.2.1` 不再散落硬编码，依赖任务 5.1 和 5.2。

## 6. 安全检查
- [√] 6.1 执行安全检查: Token 不输出到日志/错误消息/UI；删除凭证和清除进度有二次确认；播放上报不跨用户/跨服务器；未引入明文密码保存。

## 7. 文档更新
- [√] 7.1 更新 `helloagents/main/CHANGELOG.md`、`helloagents/main/wiki/api.md`、`helloagents/main/wiki/arch.md`、`helloagents/main/wiki/modules/data.md`、`helloagents/main/wiki/modules/ui.md`，记录认证图片、播放上报、搜索取消、危险操作确认与版本号来源。

## 8. 测试

### 8A. TDD路径
- [-] 8A.1 RED: 为搜索取消、版本号一致、危险操作确认、播放切集上报添加失败测试，确认失败原因分别是旧结果覆盖、硬编码版本、缺少确认、旧 source 上报。
  > 备注: 本轮未保留先失败的 RED 证据；改为补充回归测试并执行完整单元测试。
- [√] 8A.2 GREEN: 以最小生产实现让 RED 测试通过，依赖任务 8A.1。
- [√] 8A.3 REFACTOR: 在测试保持通过的前提下整理重复状态与辅助方法，依赖任务 8A.2。
- [√] 8A.4 VERIFY: 设置 `JAVA_HOME=C:\Users\MyPC\.jdks\corretto-17.0.16` 后运行 `.\gradlew.bat :app:testDebugUnitTest` 和 `.\gradlew.bat :app:assembleDebug`，记录结果，依赖任务 8A.3。
  > 备注: `.\gradlew.bat :app:testDebugUnitTest` 和 `.\gradlew.bat :app:assembleDebug` 均通过。

### 8B. TDD-EXEMPT路径
- [√] 8B.1 TDD-EXEMPT: 真实 Emby 图片认证加载需要设备/服务端联调，原因: 单元测试无法完整覆盖 Coil 到真实服务器的认证链路；替代验证: 使用 `http://10.10.10.100:60096/` 登录后在 TV 或模拟器确认封面显示。
  > 备注: 已完成自动化可覆盖部分；真实设备封面显示仍需后续手工验收。
