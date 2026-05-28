# 变更提案: 修复遥控器 OK 需要按两次才进入详情

## 需求背景
当前在 TV 端使用遥控器操作媒体卡片时，需要按两次确认键才会进入具体媒体详情。用户预期是焦点已经停留在媒体卡片上时，按一次 OK/Enter 就立即触发打开详情。

初步代码分析显示，媒体详情入口本身没有二次确认逻辑：`HomeScreen` 中 `MediaPosterCard` 的 `onClick` 会直接调用 `openMediaDetail(item)`。更可能的问题集中在通用可聚焦组件 `FocusableGlassSurface`：它同时叠加了 `Modifier.focusable()` 和 `Modifier.clickable()`，但没有显式处理 TV 遥控器的 `DirectionCenter`、`Enter`、`NumPadEnter` 按键。在 TV Compose/Compose 焦点体系下，这可能造成第一次 OK 只完成焦点/点击语义切换，第二次 OK 才触发实际 `clickable`。

## 变更内容
1. 为 `FocusableGlassSurface` 增加遥控器 OK/Enter 键的统一处理。
2. 在 KeyUp 阶段捕获 `DirectionCenter`、`Enter`、`NumPadEnter` 并直接触发当前 `onClick` 或禁用态提示。
3. 保持鼠标/触摸点击能力不变，避免影响桌面预览或非遥控器交互。
4. 增加最小回归测试或验证，确认媒体卡片一次 OK 即触发详情入口。

## 范围边界
- **范围内:** `FocusableGlassSurface` 遥控器 OK 事件处理、相关单元/组件级测试、知识库同步。
- **范围外:** 不重构 HomeScreen 导航结构，不引入 Navigation 框架，不改媒体详情 API，不改播放器 OSD 按键体系，不调整版本号。
- **拆分说明:** 本方案只解决“已聚焦卡片按 OK 需两次”的根因；后续如需全面焦点验收或 UI 自动化测试，可单独规划。

## 影响范围
- **模块:** ui/components、ui/home、知识库。
- **文件:** `app/src/main/java/com/embytv/ui/components/CinematicComponents.kt`，必要时新增 UI 行为测试文件，更新 `helloagents/main/wiki/modules/ui.md`、`helloagents/main/CHANGELOG.md`。
- **API:** 无 Emby API 变更。
- **数据:** 无数据模型变更。

## 核心场景

### 需求: 遥控器 OK 单次触发卡片操作
**模块:** ui
用户在 TV 端通过方向键把焦点移动到媒体卡片或其他可点击玻璃面板后，按一次 OK/Enter 即触发对应动作。

#### 场景: 媒体卡片进入详情
媒体卡片已获得焦点时：
- 按一次遥控器 OK/Enter。
- `MediaPosterCard` 触发 `onClick`。
- Movie/Series 调用 `openMediaDetail(item)` 并进入详情加载态。

#### 场景: 媒体库卡片进入资源列表
媒体库卡片已获得焦点时：
- 按一次遥控器 OK/Enter。
- `LibraryCard` 触发 `onClick`。
- 页面进入对应媒体库资源列表。

#### 场景: 禁用入口反馈
禁用但带 `disabledReason` 的入口获得焦点时：
- 按一次遥控器 OK/Enter。
- 触发 `onDisabledClick(reason)`。
- 页面显示明确提示，不出现空响应。

## 风险评估
- **风险:** 同时保留 `clickable` 和新增 key handler 可能导致触摸/鼠标点击与遥控器按键重复触发。
- **缓解:** 只在 `KeyEventType.KeyUp` 且 key 为 OK/Enter 时消费事件，触摸点击仍交给 `clickable`；必要时通过测试验证单次回调。
- **风险:** 修改通用组件会影响按钮、抽屉项、媒体库卡片、播放 OSD 等多个复用入口。
- **缓解:** 最小修改在 `FocusableGlassSurface` 内完成，并运行单元测试、Debug 构建；重点手工验收首页媒体卡片、媒体库卡片、收藏卡片、返回按钮和禁用入口。
