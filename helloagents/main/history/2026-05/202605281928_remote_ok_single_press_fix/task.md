# 任务清单: 修复遥控器 OK 需要按两次才进入详情

目录: `helloagents/main/plan/202605281928_remote_ok_single_press_fix/`

---

## 0. 方案边界确认
- [√] 0.1 确认本次只修复 `FocusableGlassSurface` 的遥控器 OK/Enter 单次触发行为。
- [√] 0.2 确认不改 HomeScreen 业务导航、不改 Emby API、不改版本号。
- [√] 0.3 确认最小改动策略: 不做 UI 结构重构，不改播放器 OSD 根级按键体系。

## 1. RED: 根因与回归验证
- [√] 1.1 复核 `FocusableGlassSurface` 当前仅依赖 `focusable + clickable`，没有显式处理 `DirectionCenter/Enter/NumPadEnter`。
- [√] 1.2 复核 `HomeScreen` 中 `MediaPosterCard` 点击链路一次调用即可进入 `openMediaDetail(item)`，确认问题不在详情业务分支。
- [-] 1.3 评估现有测试能力；若能使用 Compose UI 测试，先补一个聚焦后单次 OK 触发 `onClick` 的失败测试；否则记录 TDD-EXEMPT。
> 备注: 当前项目已有 JVM 单元测试入口，但未配置 Compose UI instrumentation 测试运行环境；本次为 TV 遥控器焦点事件修复，采用源码复核、单元测试编译和 debug APK 构建作为替代验证。

## 2. GREEN: 通用组件修复
- [√] 2.1 在 `app/src/main/java/com/embytv/ui/components/CinematicComponents.kt` 为 `FocusableGlassSurface` 增加 `onKeyEvent`。
- [√] 2.2 在 KeyUp 阶段处理 `Key.DirectionCenter`、`Key.Enter`、`Key.NumPadEnter`，调用 enabled 状态的 `onClick` 并消费事件。
- [√] 2.3 对禁用但有 `disabledReason` 的入口，单次 OK 调用 `onDisabledClick(reason)` 并消费事件。
- [√] 2.4 保留现有 `Modifier.clickable`，确保触摸/鼠标点击路径不变。

## 3. 回归检查
- [√] 3.1 检查 `MediaPosterCard`、`LibraryCard`、`NavigationRow`、`RoundIconButton`、播放 OSD 复用按钮均能沿用新行为。
- [√] 3.2 检查不会在同一次遥控器 OK 中重复触发 `onClick`。
- [√] 3.3 检查 Back、方向键移动焦点不受影响。

## 4. 文档更新
- [√] 4.1 更新 `helloagents/main/wiki/modules/ui.md`，记录通用可聚焦面板显式处理 TV OK/Enter。
- [√] 4.2 更新 `helloagents/main/wiki/arch.md`，追加 ADR-010 索引。
- [√] 4.3 更新 `helloagents/main/CHANGELOG.md`，记录遥控器 OK 单次触发修复。

## 5. 验证
- [√] 5.1 VERIFY: 运行 `.\gradlew.bat :app:testDebugUnitTest`。
- [√] 5.2 VERIFY: 运行 `.\gradlew.bat :app:assembleDebug`。
- [-] 5.3 TDD-EXEMPT: 真机遥控器验收，原因: 需要 Android TV/模拟器实际遥控器焦点事件；替代验证: 安装 APK 后验证媒体卡片、媒体库卡片、收藏卡片、返回按钮和禁用入口一次 OK 生效。
> 备注: 当前环境未连接 Android TV 真机或可交互 TV 模拟器；APK 已构建通过，真机安装后按上述路径手工验收。
