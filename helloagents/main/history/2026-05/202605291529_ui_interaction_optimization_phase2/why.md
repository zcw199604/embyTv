# 需求分析：Emby TV UI/交互体验优化 - Phase 2

## 📋 需求概述

**需求来源**：Phase 1 完成后的持续优化计划
**需求类型**：功能增强 + 架构重构
**影响范围**：UI层 + 数据持久化层
**预期版本**：0.4.0
**前置依赖**：Phase 1 (v0.3.0) 已完成

---

## 🎯 核心问题

### 1. 搜索体验不够便捷
**现状**：
- 无搜索历史记录，用户需要重复输入相同的关键词
- 无搜索建议，用户不知道可以搜索什么
- 无热门搜索推荐，冷启动体验差

**影响**：
- 搜索效率低，用户体验不够流畅
- 重复输入增加操作成本
- 新用户不知道如何开始搜索

**用户场景**：
```
场景1：重复搜索
用户昨天搜索过"复仇者联盟"，今天想再看一遍，
需要重新输入完整关键词。

场景2：不知道搜什么
用户打开搜索页，面对空白输入框不知道该搜什么，
缺少引导和建议。

场景3：输入效率低
TV 遥控器输入慢，如果能从历史记录中选择，
可以节省大量时间。
```

### 2. 加载状态体验不够友好
**现状**：
- 加载时显示空白或简单的文字提示
- 用户不知道内容的大致结构
- 加载时间感知较长

**影响**：
- 用户感觉应用响应慢
- 加载过程缺少视觉反馈
- 用户体验不够流畅

**对比分析**：
```
当前实现：
[空白] → 3秒 → [内容显示]
用户感知：应用很慢

骨架屏实现：
[骨架屏] → 3秒 → [内容显示]
用户感知：内容正在加载，应用响应快
```

### 3. 长列表导航效率低
**现状**：
- 媒体库和搜索结果只能逐个卡片移动
- 浏览大量内容时操作疲劳
- 无快速跳转机制

**影响**：
- 浏览效率低，用户体验差
- 在大型媒体库中查找内容困难
- 遥控器操作次数过多

**数据支持**：
```
假设媒体库有 500 部电影，每行 5 个：
- 当前方式：需要按 100 次方向键才能到底部
- 快速导航：按 1 次字母键 + 5 次方向键即可定位
```

### 4. HomeScreen 文件过大
**现状**：
- `HomeScreen.kt` 文件 1685 行，维护困难
- 多个独立功能混在一个文件中
- 代码复用性差，职责不清晰

**影响**：
- 代码可读性差
- 修改风险高，容易引入 bug
- 团队协作困难
- 性能可能受影响（编译时间长）

**文件结构分析**：
```kotlin
HomeScreen.kt (1685行)
├── HomeScreen (主入口)
├── CredentialPickerScreen (200行)
├── HomeDashboardScreen (300行)
├── FavoriteContentScreen (150行)
├── LibraryContentScreen (120行)
├── SearchScreen (180行)
├── DiscoveryScreen (200行)
├── MediaDetailScreen (250行)
└── 各种辅助组件 (285行)
```

---

## 💡 解决方案

### 方案1：搜索历史功能

#### 技术方案
**数据持久化：** 使用 DataStore Preferences 存储搜索历史
```kotlin
// 数据结构
data class SearchHistory(
    val query: String,
    val timestamp: Long,
    val resultCount: Int
)

// 存储策略
- 最多保存 20 条历史记录
- 按时间倒序排列
- 重复搜索更新时间戳
- 支持删除单条或清空全部
```

**UI 设计：**
```
搜索页布局：
┌─────────────────────────────────┐
│ [返回] 搜索                      │
│ ┌─────────────────────────────┐ │
│ │ 🔍 [输入框]          [清空] │ │
│ └─────────────────────────────┘ │
│                                 │
│ 最近搜索                        │
│ [复仇者联盟] [钢铁侠] [蜘蛛侠]  │
│                                 │
│ 热门搜索（可选）                │
│ [漫威] [DC] [星球大战]          │
│                                 │
│ 搜索结果...                     │
└─────────────────────────────────┘
```

**交互流程：**
1. 用户打开搜索页 → 显示历史记录和热门搜索
2. 用户选择历史记录 → 自动填充并搜索
3. 用户输入新关键词 → 搜索完成后保存到历史
4. 用户长按历史记录 → 显示删除选项

#### 实现要点
- ✅ 使用 DataStore 而非 SharedPreferences（更现代、类型安全）
- ✅ 历史记录去重（相同关键词只保留最新的）
- ✅ 限制历史记录数量（避免无限增长）
- ✅ 提供清空历史功能
- ✅ 历史记录可点击快速搜索

---

### 方案2：骨架屏加载状态

#### 技术方案
**实现方式：** 自定义 Modifier.shimmerEffect()
```kotlin
fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition()
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    this.drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.3f),
                    Color.Transparent
                ),
                start = Offset(translateAnim - 1000f, 0f),
                end = Offset(translateAnim, size.height)
            )
        )
    }
}
```

**骨架屏组件：**
```kotlin
@Composable
fun MediaCardSkeleton() {
    Column(
        modifier = Modifier.shimmerEffect(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .background(CinematicGlassColors.SurfaceHigh)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(16.dp)
                .background(CinematicGlassColors.Surface)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(14.dp)
                .background(CinematicGlassColors.Surface)
        )
    }
}
```

**应用场景：**
- 首页 Dashboard 加载
- 媒体库列表加载
- 搜索结果加载
- 收藏页加载
- 媒体详情加载

#### 实现要点
- ✅ 骨架屏尺寸与实际内容一致
- ✅ 闪烁动画流畅自然（1.2秒周期）
- ✅ 颜色与主题一致
- ✅ 支持不同类型的骨架屏（卡片、列表、详情）

---

### 方案3：长列表快速导航

#### 技术方案A：字母索引侧边栏
```
布局设计：
┌─────────────────────────────┐
│ [返回] 电影库 (500部)       │ A
│                             │ B
│ [卡片] [卡片] [卡片] [卡片] │ C
│ [卡片] [卡片] [卡片] [卡片] │ ...
│ [卡片] [卡片] [卡片] [卡片] │ X
│                             │ Y
│                             │ Z
└─────────────────────────────┘
```

**实现方式：**
```kotlin
@Composable
fun AlphabetIndexBar(
    items: List<MediaItemSummary>,
    onIndexClick: (Char) -> Unit
) {
    val availableLetters = items
        .mapNotNull { it.name.firstOrNull()?.uppercaseChar() }
        .filter { it in 'A'..'Z' }
        .distinct()
        .sorted()

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(end = 20.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        ('A'..'Z').forEach { letter ->
            IndexButton(
                letter = letter,
                enabled = letter in availableLetters,
                onClick = { onIndexClick(letter) }
            )
        }
    }
}
```

#### 技术方案B：Page Up/Down 支持
```kotlin
// 监听遥控器按键
.onKeyEvent { event ->
    when (event.key) {
        Key.PageUp -> {
            // 向上跳转一屏（约 10 个卡片）
            scrollToIndex(currentIndex - 10)
            true
        }
        Key.PageDown -> {
            // 向下跳转一屏
            scrollToIndex(currentIndex + 10)
            true
        }
        else -> false
    }
}
```

#### 技术方案C：快速滚动指示器
```
滚动时显示：
┌─────────────────────────────┐
│                             │
│         ┌─────────┐         │
│         │   M     │         │ ← 当前位置指示器
│         │ 120/500 │         │
│         └─────────┘         │
│                             │
└─────────────────────────────┘
```

#### 实现要点
- ✅ 字母索引只显示有内容的字母
- ✅ 点击字母后平滑滚动到对应位置
- ✅ Page Up/Down 支持（如果遥控器有这些键）
- ✅ 快速滚动时显示位置指示器

---

### 方案4：HomeScreen 拆分重构

#### 目标架构
```
ui/screens/
├── home/
│   ├── HomeScreen.kt (300行以内，只负责路由)
│   ├── HomeViewModel.kt
│   ├── HomeUiState.kt
│   └── components/
│       ├── HomeDashboard.kt
│       └── CredentialPicker.kt
├── search/
│   ├── SearchScreen.kt
│   ├── SearchViewModel.kt
│   ├── SearchUiState.kt
│   └── components/
│       ├── SearchBar.kt
│       └── SearchHistory.kt
├── favorites/
│   ├── FavoritesScreen.kt
│   ├── FavoritesViewModel.kt
│   └── FavoritesUiState.kt
├── library/
│   ├── LibraryContentScreen.kt
│   ├── LibraryViewModel.kt
│   └── LibraryUiState.kt
├── detail/
│   ├── MediaDetailScreen.kt
│   ├── SeasonEpisodesScreen.kt
│   ├── DetailViewModel.kt
│   └── DetailUiState.kt
└── discovery/
    ├── DiscoveryScreen.kt
    ├── DiscoveryViewModel.kt
    └── DiscoveryUiState.kt
```

#### 拆分策略
**阶段1：提取独立 Screen**
1. 提取 `SearchScreen` → `ui/screens/search/SearchScreen.kt`
2. 提取 `FavoritesScreen` → `ui/screens/favorites/FavoritesScreen.kt`
3. 提取 `LibraryContentScreen` → `ui/screens/library/LibraryContentScreen.kt`
4. 提取 `MediaDetailScreen` → `ui/screens/detail/MediaDetailScreen.kt`
5. 提取 `DiscoveryScreen` → `ui/screens/discovery/DiscoveryScreen.kt`

**阶段2：提取可复用组件**
1. 提取 `MediaRow` → `ui/components/MediaRow.kt`
2. 提取 `MediaGridRow` → `ui/components/MediaGridRow.kt`
3. 提取 `DetailTopBar` → `ui/components/DetailTopBar.kt`
4. 提取 `SectionHeader` → `ui/components/SectionHeader.kt`

**阶段3：优化 HomeScreen**
1. 保留 `HomeScreen` 作为主路由
2. 保留 `HomeDashboardScreen` 作为首页内容
3. 删除已提取的 Screen 代码
4. 目标：`HomeScreen.kt` < 300 行

#### 实现要点
- ✅ 每个 Screen 独立文件，职责单一
- ✅ ViewModel 和 UiState 与 Screen 放在同一目录
- ✅ 可复用组件提取到 `ui/components/`
- ✅ 保持现有功能不变
- ✅ 充分测试，确保无回归

---

## 📊 需求完整性评分

| 维度 | 评分 | 说明 |
|------|------|------|
| 目标明确性 | 9/10 | 4个优化目标清晰明确 |
| 范围清晰度 | 9/10 | 功能范围和技术方案明确 |
| 技术可行性 | 9/10 | 基于成熟技术，可行性高 |
| 约束条件 | 8/10 | 需保持 Phase 1 的设计风格 |
| 验收标准 | 8/10 | 需在详细设计中补充 |

**综合评分：8.6/10** ✅ 可以进入方案设计阶段

---

## 🔍 风险评估

### 高风险项
- **HomeScreen 拆分**：涉及大量代码移动，可能引入 bug
  - **缓解措施**：分步拆分，每步都充分测试

### 中风险项
- **搜索历史持久化**：DataStore 使用不当可能导致数据丢失
  - **缓解措施**：充分测试异常情况，添加数据备份机制

- **骨架屏性能**：动画可能在低端设备上影响性能
  - **缓解措施**：使用硬件加速，在低端设备上降级

### 低风险项
- **字母索引**：独立功能，不影响现有逻辑
- **快速滚动**：可选功能，失败不影响基本使用

---

## 📈 预期收益

### 用户体验
- **搜索效率提升 50%**：历史记录快速选择
- **加载体验提升**：骨架屏减少等待焦虑
- **浏览效率提升 80%**：字母索引快速定位
- **整体流畅度提升**：代码优化带来的性能提升

### 开发效率
- **代码可维护性提升 60%**：文件拆分，职责清晰
- **开发效率提升 40%**：独立模块，并行开发
- **Bug 率降低 30%**：代码结构清晰，易于测试

### 技术债务
- **架构健康度大幅提升**：从单体到模块化
- **扩展性增强**：新功能更容易添加
- **测试覆盖率提升**：独立模块更容易测试

---

## ✅ 验收标准

### 功能验收
- [ ] 搜索历史保存和显示正常
- [ ] 历史记录可点击快速搜索
- [ ] 历史记录可删除和清空
- [ ] 骨架屏在所有加载场景显示
- [ ] 骨架屏动画流畅自然
- [ ] 字母索引可用且准确
- [ ] HomeScreen 拆分完成，功能正常

### 性能验收
- [ ] 搜索历史读写性能 < 50ms
- [ ] 骨架屏动画帧率 > 55fps
- [ ] 字母索引响应时间 < 100ms
- [ ] 拆分后编译时间减少 > 20%

### 代码质量验收
- [ ] HomeScreen.kt < 300 行
- [ ] 每个新 Screen 文件 < 400 行
- [ ] 单元测试覆盖率 > 75%
- [ ] 无代码重复（DRY 原则）

---

## 📅 时间估算

### 任务拆分
- **Task 1: 搜索历史功能** - 2天
  - DataStore 集成 - 0.5天
  - UI 实现 - 1天
  - 测试 - 0.5天

- **Task 2: 骨架屏实现** - 2天
  - Modifier 扩展 - 0.5天
  - 骨架屏组件 - 1天
  - 集成到各页面 - 0.5天

- **Task 3: 快速导航** - 2天
  - 字母索引 - 1天
  - Page Up/Down - 0.5天
  - 测试优化 - 0.5天

- **Task 4: HomeScreen 拆分** - 3天
  - 提取独立 Screen - 1.5天
  - 提取可复用组件 - 1天
  - 测试验证 - 0.5天

- **Task 5: 测试和文档** - 1天

**总计：10天**

---

## 🎯 成功指标

### 定量指标
- 搜索历史使用率 > 60%
- 用户平均搜索时间减少 40%
- 代码文件平均行数 < 350
- 单元测试覆盖率 > 75%

### 定性指标
- 用户反馈加载体验更流畅
- 开发团队认为代码更易维护
- 新功能开发速度加快

---

**需求分析完成时间**：2026-05-29
**分析人员**：Kiro AI
**审核状态**：待审核
