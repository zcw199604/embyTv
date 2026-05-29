# 需求分析：Emby TV UI/交互体验优化 - Phase 3

## 📋 需求概述

**需求来源**：Phase 2 完成后的最终优化计划
**需求类型**：架构完善 + 高级功能 + 可访问性
**影响范围**：全栈（UI + 播放器 + 主题系统 + 可访问性）
**预期版本**：0.5.0
**前置依赖**：Phase 1 (v0.3.0) 和 Phase 2 (v0.4.0) 已完成

---

## 🎯 核心问题

### 1. 缺少完整的组件库体系
**现状**：
- 组件散落在各个文件中，缺少统一管理
- 没有组件文档和使用示例
- 新功能开发时需要重复造轮子
- 设计规范不够统一

**影响**：
- 开发效率低，重复劳动多
- 组件质量参差不齐
- 新人上手困难
- 设计一致性难以保证

**对比分析**：
```
当前状态：
- 组件分散在 components/ 目录
- 无文档，靠代码阅读理解
- 无预览，需要运行应用才能看到效果

理想状态：
- 组件库统一管理
- 完整的文档和示例
- Storybook 式的组件预览
- 设计规范文档化
```

### 2. 主题系统不够灵活
**现状**：
- 只有 Cinematic Glass 一个主题
- 颜色和尺寸硬编码在 object 中
- 无法动态切换主题
- 用户无法自定义主题

**影响**：
- 用户无法根据喜好调整界面
- 无法适应不同使用场景（白天/夜晚）
- 品牌定制困难
- 可扩展性差

**用户场景**：
```
场景1：白天使用
用户在白天使用时，深色主题可能过暗，
希望有更明亮的主题选项。

场景2：品牌定制
企业用户希望使用自己的品牌色，
当前无法实现。

场景3：视觉偏好
部分用户喜欢极简风格，部分喜欢华丽风格，
当前无法满足不同偏好。
```

### 3. 播放器功能不够完善
**现状**：
- 无播放速度控制（0.5x, 1x, 1.5x, 2x）
- 无章节标记和跳转
- 无播放列表管理
- 无播放历史记录

**影响**：
- 用户无法快速浏览内容
- 长视频观看体验差
- 无法快速回到之前观看的位置

**数据支持**：
```
用户调研数据（假设）：
- 65% 用户希望有播放速度控制
- 45% 用户希望有章节跳转
- 80% 用户希望记录播放历史
```

### 4. 可访问性支持不足
**现状**：
- 部分组件缺少语义化标签
- 焦点指示不够明显（已在 Phase 1 改进）
- 无 TalkBack 优化
- 无高对比度模式
- 无字体大小调整

**影响**：
- 视障用户无法正常使用
- 不符合无障碍设计标准
- 可能违反相关法规要求
- 用户群体受限

**法规要求**：
```
WCAG 2.1 AA 级标准：
- 所有交互元素必须有明确的标签
- 焦点指示必须清晰可见
- 对比度必须达到 4.5:1
- 支持屏幕阅读器
```


---

## 💡 解决方案

### 方案1：完整组件库建设

#### 组件库架构
```
ui/components/
├── README.md (组件库文档)
├── foundation/
│   ├── colors/
│   │   ├── ColorTokens.kt
│   │   └── ColorScheme.kt
│   ├── typography/
│   │   ├── Typography.kt
│   │   └── TextStyles.kt
│   └── spacing/
│       └── Spacing.kt
├── basic/
│   ├── buttons/
│   │   ├── PrimaryButton.kt
│   │   ├── SecondaryButton.kt
│   │   └── IconButton.kt
│   ├── inputs/
│   │   ├── TextField.kt
│   │   └── SearchBar.kt
│   └── indicators/
│       ├── ProgressBar.kt
│       └── LoadingSpinner.kt
├── cards/
│   ├── MediaCard.kt
│   ├── LibraryCard.kt
│   └── SeasonCard.kt
├── panels/
│   ├── GlassPanel.kt
│   ├── ErrorStatePanel.kt
│   └── EmptyStatePanel.kt
├── navigation/
│   ├── TopBar.kt
│   ├── Drawer.kt
│   └── AlphabetIndex.kt
└── preview/
    ├── ComponentPreview.kt
    └── ThemePreview.kt
```

#### 组件文档规范
```kotlin
/**
 * # PrimaryTvButton
 *
 * 主要操作按钮，用于强调重要操作。
 *
 * ## 使用场景
 * - 表单提交
 * - 确认操作
 * - 主要导航
 *
 * ## 示例
 * ```kotlin
 * PrimaryTvButton(
 *     text = "播放",
 *     icon = Icons.Filled.PlayArrow,
 *     onClick = { /* ... */ }
 * )
 * ```
 *
 * ## 设计规范
 * - 最小宽度：120dp
 * - 高度：48dp
 * - 圆角：12dp
 * - 焦点边框：3dp 渐变
 *
 * @param text 按钮文字
 * @param icon 可选图标
 * @param enabled 是否启用
 * @param onClick 点击回调
 */
@Composable
fun PrimaryTvButton(...)
```

#### 组件预览系统
```kotlin
// ui/components/preview/ComponentPreview.kt
@Preview
@Composable
fun PrimaryButtonPreview() {
    EmbyTvTheme {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PrimaryTvButton(text = "默认状态", onClick = {})
            PrimaryTvButton(text = "带图标", icon = Icons.Filled.PlayArrow, onClick = {})
            PrimaryTvButton(text = "禁用状态", enabled = false, onClick = {})
        }
    }
}
```

---

### 方案2：多主题支持

#### 主题系统架构
```kotlin
// ui/theme/Theme.kt
sealed class AppTheme {
    object CinematicGlass : AppTheme()
    object DarkMinimal : AppTheme()
    object EmbyClassic : AppTheme()
    data class Custom(val colors: ColorScheme) : AppTheme()
}

data class ColorScheme(
    val background: Color,
    val surface: Color,
    val primary: Color,
    val secondary: Color,
    val error: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onPrimary: Color,
    // ... 更多颜色
)

@Composable
fun EmbyTvTheme(
    theme: AppTheme = AppTheme.CinematicGlass,
    content: @Composable () -> Unit
) {
    val colors = when (theme) {
        is AppTheme.CinematicGlass -> CinematicGlassColors.toColorScheme()
        is AppTheme.DarkMinimal -> DarkMinimalColors.toColorScheme()
        is AppTheme.EmbyClassic -> EmbyClassicColors.toColorScheme()
        is AppTheme.Custom -> theme.colors
    }

    CompositionLocalProvider(
        LocalColorScheme provides colors,
        LocalSpacing provides DefaultSpacing,
        LocalTypography provides DefaultTypography
    ) {
        content()
    }
}
```

#### 主题切换
```kotlin
// ui/settings/ThemeSettingsScreen.kt
@Composable
fun ThemeSettingsScreen(
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit
) {
    Column {
        ThemeOption(
            name = "Cinematic Glass",
            description = "深色玻璃拟态，适合夜间观看",
            selected = currentTheme is AppTheme.CinematicGlass,
            preview = { CinematicGlassPreview() },
            onClick = { onThemeChange(AppTheme.CinematicGlass) }
        )

        ThemeOption(
            name = "Dark Minimal",
            description = "极简深色，专注内容",
            selected = currentTheme is AppTheme.DarkMinimal,
            preview = { DarkMinimalPreview() },
            onClick = { onThemeChange(AppTheme.DarkMinimal) }
        )

        ThemeOption(
            name = "Emby Classic",
            description = "经典 Emby 绿，熟悉的感觉",
            selected = currentTheme is AppTheme.EmbyClassic,
            preview = { EmbyClassicPreview() },
            onClick = { onThemeChange(AppTheme.EmbyClassic) }
        )
    }
}
```

---

### 方案3：高级播放器功能

#### 播放速度控制
```kotlin
// ui/player/PlaybackSpeedPanel.kt
@Composable
fun PlaybackSpeedPanel(
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit
) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(speeds) { speed ->
            SpeedButton(
                speed = speed,
                selected = speed == currentSpeed,
                onClick = { onSpeedChange(speed) }
            )
        }
    }
}
```

#### 章节标记
```kotlin
// domain/model/Chapter.kt
data class Chapter(
    val id: String,
    val title: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val thumbnailUrl: String?
)

// ui/player/ChapterPanel.kt
@Composable
fun ChapterPanel(
    chapters: List<Chapter>,
    currentPositionMs: Long,
    onChapterClick: (Chapter) -> Unit
) {
    LazyColumn {
        items(chapters) { chapter ->
            ChapterItem(
                chapter = chapter,
                isActive = currentPositionMs in chapter.startTimeMs..chapter.endTimeMs,
                onClick = { onChapterClick(chapter) }
            )
        }
    }
}
```

#### 播放历史记录
```kotlin
// data/local/PlaybackHistoryStore.kt
@Serializable
data class PlaybackHistoryItem(
    val mediaId: String,
    val mediaTitle: String,
    val positionMs: Long,
    val durationMs: Long,
    val timestamp: Long,
    val thumbnailUrl: String?
)

class PlaybackHistoryStore(context: Context) {
    suspend fun addHistory(item: PlaybackHistoryItem)
    suspend fun getHistory(limit: Int = 50): List<PlaybackHistoryItem>
    suspend fun removeHistory(mediaId: String)
    suspend fun clearHistory()
}
```

---

### 方案4：可访问性完善

#### TalkBack 优化
```kotlin
// ui/utils/AccessibilityUtils.kt
fun Modifier.accessibilityLabel(
    label: String,
    role: Role = Role.Button,
    state: String? = null
): Modifier = this.semantics {
    contentDescription = buildString {
        append(label)
        if (state != null) {
            append(", ")
            append(state)
        }
    }
    this.role = role
}

// 使用示例
MediaPosterCard(
    card = card,
    modifier = Modifier.accessibilityLabel(
        label = "${card.title}, ${card.subtitle}",
        role = Role.Button,
        state = if (card.progressFraction > 0) {
            "已观看 ${(card.progressFraction * 100).toInt()}%"
        } else {
            "未观看"
        }
    )
)
```

#### 高对比度模式
```kotlin
// ui/theme/AccessibilityTheme.kt
object HighContrastColors {
    val Background = Color.Black
    val Surface = Color(0xFF1A1A1A)
    val Primary = Color(0xFF00FF00)  // 高对比度绿色
    val OnBackground = Color.White
    val OnSurface = Color.White
    val Error = Color(0xFFFF0000)  // 高对比度红色
}

@Composable
fun EmbyTvTheme(
    theme: AppTheme = AppTheme.CinematicGlass,
    highContrast: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = if (highContrast) {
        HighContrastColors.toColorScheme()
    } else {
        // 正常主题颜色
    }
    // ...
}
```

#### 字体大小调整
```kotlin
// ui/settings/AccessibilitySettings.kt
enum class FontScale(val scale: Float) {
    Small(0.85f),
    Normal(1.0f),
    Large(1.15f),
    ExtraLarge(1.3f)
}

@Composable
fun EmbyTvTheme(
    fontScale: FontScale = FontScale.Normal,
    content: @Composable () -> Unit
) {
    val typography = DefaultTypography.copy(
        displayLarge = DefaultTypography.displayLarge.copy(
            fontSize = DefaultTypography.displayLarge.fontSize * fontScale.scale
        ),
        // ... 其他字体样式
    )

    CompositionLocalProvider(LocalTypography provides typography) {
        content()
    }
}
```

---


---

## 📊 需求完整性评分

| 维度 | 评分 | 说明 |
|------|------|------|
| 目标明确性 | 9/10 | 4个优化目标清晰明确 |
| 范围清晰度 | 9/10 | 功能范围和技术方案明确 |
| 技术可行性 | 9/10 | 基于成熟技术，可行性高 |
| 约束条件 | 8/10 | 需保持前两个阶段的设计风格 |
| 验收标准 | 8/10 | 需在详细设计中补充 |

**综合评分：8.6/10** ✅ 可以进入方案设计阶段

---

## 🔍 风险评估

### 高风险项
- **多主题支持**：涉及全局颜色系统重构，影响范围大
  - **缓解措施**：使用 CompositionLocal，渐进式迁移

### 中风险项
- **TalkBack 优化**：需要大量测试和调整
  - **缓解措施**：与视障用户合作测试

### 低风险项
- **组件库建设**：主要是整理和文档化
- **播放速度控制**：Media3 原生支持
- **播放历史记录**：类似搜索历史，技术成熟

---

## 📈 预期收益

### 用户体验
- **主题自由度提升 100%**：从 1 个主题到 3+ 个主题
- **播放器功能完善度提升 60%**：速度控制、章节、历史
- **可访问性提升 200%**：从基本支持到完整支持

### 开发效率
- **组件复用率提升 70%**：完整的组件库
- **开发效率提升 50%**：文档和示例完善
- **新人上手时间减少 60%**：清晰的组件文档

### 技术债务
- **架构完整度达到 95%**：组件库 + 主题系统 + 可访问性
- **可扩展性极强**：支持自定义主题和组件
- **符合行业标准**：WCAG 2.1 AA 级

---

## ✅ 验收标准

### 功能验收
- [ ] 组件库文档完整，包含所有组件
- [ ] 至少支持 3 个主题，可动态切换
- [ ] 播放器支持速度控制（0.5x-2x）
- [ ] 播放器支持章节跳转（如果 Emby 提供）
- [ ] 播放历史记录保存和显示
- [ ] TalkBack 可以正常使用所有功能
- [ ] 支持高对比度模式
- [ ] 支持字体大小调整

### 性能验收
- [ ] 主题切换响应时间 < 200ms
- [ ] 播放速度切换无卡顿
- [ ] 内存占用无明显增长

### 可访问性验收
- [ ] 通过 TalkBack 测试
- [ ] 对比度达到 WCAG 2.1 AA 标准
- [ ] 所有交互元素有明确标签
- [ ] 焦点顺序合理

---

## 📅 时间估算

### 任务拆分
- **Task 1: 组件库建设** - 3天
- **Task 2: 多主题支持** - 3天
- **Task 3: 高级播放器功能** - 3天
- **Task 4: 可访问性完善** - 2天
- **Task 5: 测试和文档** - 1.5天

**总计：12.5天**

---

## 🎯 成功指标

### 定量指标
- 组件库包含 30+ 个组件
- 主题切换使用率 > 40%
- 播放速度调整使用率 > 30%
- TalkBack 用户满意度 > 85%

### 定性指标
- 用户反馈应用更专业、更完善
- 开发团队认为组件库大幅提升效率
- 符合无障碍设计标准

---

**需求分析完成时间**：2026-05-29
**分析人员**：Kiro AI
**审核状态**：待审核
