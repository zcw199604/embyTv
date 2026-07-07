# Emby TV 组件库

本目录收敛 Android TV 端可复用 Compose 组件，优先服务遥控器焦点、横屏安全区、媒体图片加载、加载/空/错误状态和播放器 OSD。

## 组件分组

- `CinematicComponents.kt`: 玻璃面板、可聚焦容器、媒体卡、媒体库卡、顶部栏、抽屉和基础按钮。
- `panels/`: 错误和空状态面板。
- `loading/`: 媒体卡片、横向列表、网格和详情页骨架屏。
- `navigation/`: 长列表字母索引和滚动位置指示器。
- `preview/`: 组件预览入口。

## 使用约定

- 交互组件优先使用 `FocusableGlassSurface`，确保 TV OK/Enter 单次触发。
- 媒体图片统一使用 `NetworkBackdropImage`，认证 Header 由 `LocalEmbyImageAuthorizationHeader` 注入。
- 加载态优先使用 `loading/` 下的骨架屏，避免空白闪烁。
- 可访问组件需要提供清晰的 `contentDescription` 或 `Modifier.accessibilityLabel()`。
- 禁用入口需要提供 `disabledReason`，遥控器确认时应给出反馈；带提示的禁用入口仍需把 focused 状态传给内容层，用于显示禁用但可见的焦点反馈。

## 设计令牌

- 颜色通过 `EmbyTvTheme` 的 `ThemePreferences` 切换，旧组件继续通过 `CinematicGlassColors` 读取当前主题色。
- 间距和尺寸通过 `CinematicGlassSpacing` 统一管理。
- 焦点动画使用 `EmbyAnimationSpecs`，默认焦点过渡为 200ms。
