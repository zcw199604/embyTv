# 任务清单：Emby TV UI/交互体验优化 - Phase 1 (v0.3.0)

## 📋 任务概览

**方案包ID**：ui-interaction-optimization-phase1  
**目标版本**：0.3.0  
**预计工期**：5-6天  
**优先级**：高  
**依赖项**：无

---

## 🧾 执行结果摘要

> **状态:** 已执行  
> **执行时间:** 2026-05-29  
> **验证命令:** `.\gradlew.bat :app:testDebugUnitTest`、`.\gradlew.bat :app:assembleDebug` 均通过。  
> **TDD-EXEMPT:** 本次新增 Compose 组件没有引入 Android Instrumentation/Compose UI 测试依赖，未补充组件级 UI 测试；通过现有 reducer 单元测试、Kotlin 编译和 Debug APK 构建验证可编译性与播放器状态行为。  
> **跳过项:** 1080p/4K 实机手测、弱网手测、Android Profiler、GPU Rendering、Git tag 与 Release APK 构建需要设备或发布授权，当前未静默执行。

---

## ✅ 任务列表

### 🎨 Task 1: 图片加载优化
**优先级**：P0（最高）  
**预计时间**：0.5天  
**负责模块**：`ui/components/CinematicComponents.kt`

#### 子任务
- [√] 1.1 优化 `NetworkBackdropImage` 组件
  - [√] 移除不必要的 `context` 依赖
  - [√] 使用 `remember` 缓存 `ImageRequest`
  - [√] 添加 `crossfade(300)` 淡入动画
  - [√] 添加 `placeholder` 和 `error` 占位符
  
- [√] 1.2 更新 `ImagePlaceholder` 组件
  - [√] 确保占位符颜色与主题一致
  - [-] 验证在不同尺寸下的显示效果
    > 备注: 已通过编译验证尺寸常量接入；1080p/4K 实机视觉验证需设备环境。
  
- [-] 1.3 编写单元测试
  - [-] 测试 `ImageRequest` 不重复构建
  - [-] 测试占位符正确显示
  - [-] 测试动画正常工作
    > 备注: 当前项目未配置 Compose UI 单元测试环境；以 `testDebugUnitTest`、`assembleDebug` 和现有 UI 组件编译验证替代。

#### 验收标准
- ✅ 图片加载有300ms淡入动画
- ✅ 加载中显示灰色占位符，无白屏闪烁
- ✅ 加载失败显示深灰色占位符
- ✅ `ImageRequest` 在重组时不重复构建
- ✅ 单元测试覆盖率 > 80%

#### 技术要点
```kotlin
// 关键代码片段
val model = remember(imageUrl, authorizationHeader) {
    ImageRequest.Builder(context)
        .data(imageUrl)
        .crossfade(300)
        .build()
}
```

---

### 🎯 Task 2: 焦点指示增强
**优先级**：P0（最高）  
**预计时间**：1天  
**负责模块**：`ui/components/CinematicComponents.kt`

#### 子任务
- [√] 2.1 优化 `GlassPanel` 组件
  - [√] 添加 `animateDpAsState` 实现边框宽度动画
  - [√] 添加 `animateDpAsState` 实现阴影动画
  - [√] 实现渐变边框（`Brush.linearGradient`）
  - [√] 调整动画时长为200ms
  
- [√] 2.2 创建动画规范文件
  - [√] 新建 `ui/utils/AnimationSpecs.kt`
  - [√] 定义标准动画时长和缓动函数
  - [√] 导出可复用的动画配置
  
- [√] 2.3 更新主题颜色
  - [√] 在 `ui/theme/EmbyTvTheme.kt` 中添加新颜色
  - [√] 定义 `OnSurfaceMedium` 等中间层次颜色
  
- [-] 2.4 测试焦点效果
  - [-] 在1080p电视上验证可见性
  - [-] 在4K电视上验证比例正确
  - [-] 验证快速切换时无抖动
    > 备注: 需要 Android TV 或模拟器手动验证；本次仅完成编译和 APK 构建验证。

#### 验收标准
- ✅ 焦点状态有渐变边框（绿色到黄色）
- ✅ 焦点状态有8dp阴影效果
- ✅ 边框宽度从1dp平滑过渡到3dp
- ✅ 动画时长200ms，流畅无卡顿
- ✅ 在1080p和4K电视上显示清晰

#### 技术要点
```kotlin
// 关键代码片段
val borderWidth by animateDpAsState(
    targetValue = if (focused) 3.dp else 1.dp,
    animationSpec = tween(durationMillis = 200)
)

val elevation by animateDpAsState(
    targetValue = if (focused) 8.dp else 0.dp,
    animationSpec = tween(durationMillis = 200)
)
```

---

### ❌ Task 3: 错误状态优化
**优先级**：P1（高）  
**预计时间**：1天  
**负责模块**：新建 `ui/components/panels/ErrorStatePanel.kt`

#### 子任务
- [√] 3.1 创建 `ErrorStatePanel` 组件
  - [√] 定义 `ErrorType` 枚举
  - [√] 实现组件布局（图标+标题+描述+按钮）
  - [√] 为不同错误类型配置图标
  - [√] 添加重试按钮（可选）
  
- [√] 3.2 创建 `EmptyStatePanel` 组件
  - [√] 实现空状态布局
  - [√] 支持自定义图标和操作按钮
  
- [√] 3.3 迁移现有错误展示
  - [√] 在 `HomeScreen.kt` 中替换 `LibraryStatePanel`
  - [√] 更新搜索、收藏、媒体库等页面
  - [-] 删除旧的错误展示代码
    > 备注: 保留 `LibraryStatePanel` 作为兼容包装函数，内部已委派给新状态面板，避免一次性扩大 `HomeScreen.kt` 改动面。
  
- [-] 3.4 编写单元测试
  - [-] 测试不同错误类型显示正确图标
  - [-] 测试重试按钮点击事件
  - [-] 测试布局在不同文本长度下正常
    > 备注: 当前项目未配置 Compose UI 测试依赖；以编译和 Debug APK 构建验证组件可用性。

#### 验收标准
- ✅ 错误状态显示对应的图标（网络/认证/404/服务器/未知）
- ✅ 错误信息层次清晰（标题+描述）
- ✅ 有重试按钮时可以正常点击
- ✅ 所有错误场景已迁移到新组件
- ✅ 单元测试覆盖率 > 85%

#### 技术要点
```kotlin
// 关键代码片段
enum class ErrorType {
    Network, Auth, NotFound, Server, Unknown
}

@Composable
fun ErrorStatePanel(
    title: String,
    subtitle: String,
    errorType: ErrorType = ErrorType.Unknown,
    onRetry: (() -> Unit)? = null
)
```

---

### 📊 Task 4: 播放器缓冲进度显示
**优先级**：P1（高）  
**预计时间**：1.5天  
**负责模块**：`ui/player/PlayerScreen.kt`, `ui/player/PlayerOsdState.kt`

#### 子任务
- [√] 4.1 扩展 `PlayerOsdState`
  - [√] 添加 `bufferedFraction: Float` 字段
  - [√] 更新 `PlayerOsdAction.ProgressChanged` 包含缓冲进度
  - [√] 更新 `PlayerOsdReducer` 处理缓冲进度
  
- [√] 4.2 优化 `ProgressRail` 组件
  - [√] 实现双层进度条（缓冲层+播放层）
  - [√] 调整进度条高度为8dp
  - [√] 优化颜色对比度
  
- [√] 4.3 更新播放器状态监听
  - [√] 从 `player.bufferedPosition` 获取缓冲位置
  - [√] 计算缓冲进度百分比
  - [√] 每秒更新一次缓冲进度
  
- [√] 4.4 测试缓冲进度
  - [-] 模拟不同网络环境
  - [√] 验证缓冲进度准确性
  - [-] 验证UI显示正确
    > 备注: reducer 已覆盖缓冲比例更新与边界钳制；不同网络和 UI 视觉显示需设备/模拟器手测。

#### 验收标准
- ✅ 播放器显示双进度条（灰色缓冲+绿色播放）
- ✅ 缓冲进度实时更新
- ✅ 缓冲进度始终 >= 播放进度
- ✅ 进度条高度8dp，清晰可见
- ✅ 在弱网环境下缓冲进度正确显示

#### 技术要点
```kotlin
// 关键代码片段
data class PlayerOsdState(
    // ...
    val bufferedFraction: Float = 0f
)

LaunchedEffect(player) {
    while (true) {
        val buffered = player.bufferedPosition.toFloat() / duration
        dispatch(PlayerOsdAction.ProgressChanged(
            positionMs = player.currentPosition,
            durationMs = duration,
            bufferedFraction = buffered.coerceIn(0f, 1f)
        ))
        delay(1_000)
    }
}
```

---

### 🎨 Task 5: 主题和工具类扩展
**优先级**：P2（中）  
**预计时间**：0.5天  
**负责模块**：`ui/theme/`, `ui/utils/`

#### 子任务
- [√] 5.1 扩展颜色定义
  - [√] 在 `CinematicGlassColors` 中添加语义化颜色
  - [√] 添加 `OnSurfaceMedium`, `InfoText`, `MetadataText`
  - [√] 添加 `ErrorContainer`, `OnErrorContainer`
  
- [√] 5.2 扩展尺寸规范
  - [√] 在 `CinematicGlassSpacing` 中添加组件尺寸
  - [√] 定义按钮尺寸、进度条高度、图标尺寸
  
- [√] 5.3 创建 Modifier 扩展
  - [√] 新建 `ui/utils/Modifiers.kt`
  - [√] 实现 `Modifier.focusScale()`
  - [√] 预留 `Modifier.shimmerEffect()` 接口（Phase 2实现）

#### 验收标准
- ✅ 所有新颜色已定义并文档化
- ✅ 所有新尺寸已定义并文档化
- ✅ Modifier扩展可正常使用
- ✅ 代码符合Kotlin编码规范

---

### 🧪 Task 6: 测试和验证
**优先级**：P0（最高）  
**预计时间**：1.5天  
**负责模块**：全部

#### 子任务
- [√] 6.1 单元测试
  - [-] 为所有新组件编写单元测试
  - [-] 确保测试覆盖率 > 80%
  - [√] 运行 `.\gradlew.bat :app:testDebugUnitTest`
    > 备注: 新增播放器 reducer 单测；Compose 组件测试和覆盖率门禁当前项目未配置。
  
- [-] 6.2 UI测试
  - [-] 编写关键路径的UI测试
  - [-] 测试焦点导航流程
  - [-] 测试错误状态显示
    > 备注: 当前项目未配置 androidTest Compose UI 测试实现，本次不新增测试栈。
  
- [-] 6.3 手动测试
  - [-] 在1080p电视上完整测试
  - [-] 在4K电视上完整测试（如有条件）
  - [-] 在弱网环境下测试图片加载
  - [-] 测试播放器缓冲进度
    > 备注: 需要 TV/模拟器和网络条件。
  
- [-] 6.4 性能测试
  - [-] 使用 Android Profiler 监控内存
  - [-] 使用 GPU Rendering 监控帧率
  - [-] 对比优化前后的性能数据
    > 备注: 需要设备运行环境和基线数据。
  
- [√] 6.5 回归测试
  - [√] 验证现有功能未受影响
  - [-] 测试所有主要用户流程
  - [-] 检查是否有新的crash或ANR
    > 备注: 已通过单元测试和 Debug APK 构建做基础回归；完整用户流程、crash/ANR 需设备手测。

#### 验收标准
- ✅ 所有单元测试通过
- ✅ 测试覆盖率 > 80%
- ✅ 关键路径UI测试通过
- ✅ 手动测试清单全部完成
- ✅ 性能指标达到目标
- ✅ 无新增crash或严重bug

---

### 📝 Task 7: 文档和发布
**优先级**：P2（中）  
**预计时间**：0.5天  
**负责模块**：文档

#### 子任务
- [√] 7.1 更新 CHANGELOG
  - [√] 记录所有新增功能
  - [√] 记录所有变更
  - [√] 记录性能改进
  
- [√] 7.2 更新项目文档
  - [√] 更新 `helloagents/main/project.md`
  - [√] 记录新的组件和工具类
  - [√] 更新架构说明
  
- [√] 7.3 编写发布说明
  - [√] 准备用户可读的更新说明
  - [√] 准备开发者技术文档
  
- [√] 7.4 版本发布
  - [√] 更新版本号为 0.3.0
  - [-] 创建 Git tag
  - [-] 构建 Release APK
  - [-] 验证 APK 可正常安装和运行
    > 备注: 已构建 Debug APK；Git tag、Release APK 和安装验证属于发布动作或设备动作，未在未授权情况下执行。

#### 验收标准
- ✅ CHANGELOG 完整准确
- ✅ 项目文档已更新
- ✅ 发布说明清晰易懂
- ✅ 版本号正确
- ✅ Release APK 构建成功

---

## 📊 任务依赖关系

```
Task 5 (主题扩展)
    ↓
Task 1 (图片优化) ← Task 2 (焦点增强) ← Task 3 (错误优化)
    ↓                    ↓                    ↓
Task 4 (播放器进度)
    ↓
Task 6 (测试验证)
    ↓
Task 7 (文档发布)
```

**关键路径**：Task 5 → Task 2 → Task 6 → Task 7

---

## 🎯 里程碑

### Milestone 1: 基础优化完成（Day 2）
- ✅ Task 1: 图片加载优化
- ✅ Task 5: 主题和工具类扩展

### Milestone 2: 视觉增强完成（Day 3）
- ✅ Task 2: 焦点指示增强
- ✅ Task 3: 错误状态优化

### Milestone 3: 功能完善（Day 4）
- ✅ Task 4: 播放器缓冲进度显示

### Milestone 4: 质量保证（Day 5-6）
- ✅ Task 6: 测试和验证
- ✅ Task 7: 文档和发布

---

## 🚨 风险和应对

### 风险1: 焦点动画性能问题
**概率**：中  
**影响**：高  
**应对**：
- 使用 `animateDpAsState` 而非手动动画
- 限制同时播放的动画数量
- 在低端设备上降级为无动画

### 风险2: 图片加载缓存策略冲突
**概率**：低  
**影响**：中  
**应对**：
- 充分测试不同场景下的缓存行为
- 保留原有的 `memoryCacheKey` 和 `diskCacheKey`
- 准备回滚方案

### 风险3: 播放器状态监听影响性能
**概率**：低  
**影响**：中  
**应对**：
- 使用1秒更新间隔，避免过于频繁
- 在后台时暂停状态更新
- 监控CPU和内存占用

---

## 📋 测试清单

### 功能测试
- [ ] 图片加载有淡入动画
- [ ] 图片加载失败显示占位符
- [ ] 焦点状态有渐变边框和阴影
- [ ] 焦点切换动画流畅
- [ ] 错误状态显示正确图标
- [ ] 错误状态重试按钮可用
- [ ] 播放器显示缓冲进度
- [ ] 缓冲进度实时更新

### 性能测试
- [ ] 图片加载性能提升 > 15%
- [ ] 列表滚动帧率稳定60fps
- [ ] 内存占用无明显增长
- [ ] 焦点切换响应时间 < 80ms

### 兼容性测试
- [ ] 1080p电视显示正常
- [ ] 4K电视显示正常
- [ ] 不同Android版本兼容
- [ ] 不同Emby服务器版本兼容

### 回归测试
- [ ] 首页加载正常
- [ ] 搜索功能正常
- [ ] 播放功能正常
- [ ] 媒体详情正常
- [ ] 收藏功能正常
- [ ] 用户登录正常

---

## 📈 进度跟踪

| 任务 | 状态 | 进度 | 负责人 | 备注 |
|------|------|------|--------|------|
| Task 1: 图片加载优化 | 已完成 | 80% | Codex | 自动化验证通过；组件 UI 测试跳过 |
| Task 2: 焦点指示增强 | 已完成 | 80% | Codex | 自动化验证通过；TV 视觉手测跳过 |
| Task 3: 错误状态优化 | 已完成 | 85% | Codex | 保留兼容包装函数 |
| Task 4: 播放器缓冲进度 | 已完成 | 90% | Codex | reducer 单测覆盖缓冲比例 |
| Task 5: 主题工具扩展 | 已完成 | 100% | Codex | - |
| Task 6: 测试验证 | 部分完成 | 45% | Codex | 单元测试和 Debug 构建通过；设备/Profiler 测试跳过 |
| Task 7: 文档发布 | 部分完成 | 70% | Codex | 文档和版本号完成；tag/Release APK 跳过 |

**整体进度**：78% (5/7 完成，2/7 部分完成)

---

## 🔗 相关文档

- [需求分析](./why.md)
- [技术方案](./how.md)
- [项目约定](../project.md)
- [变更日志](../CHANGELOG.md)

---

**任务清单创建时间**：2026-05-29  
**创建人员**：Kiro AI  
**最后更新**：2026-05-29
