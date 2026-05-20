# ui

## 目的
提供 Android TV Compose 页面、输入与播放导航。

## 模块概述
- **职责:** Home 页面负责 Emby 连接和媒体选择，Player 页面负责 Media3 与 AkDanmaku 的组合展示。
- **状态:** 🚧开发中
- **最后更新:** 2026-05-20

## 规范

### 需求: Android TV 初始化
**模块:** ui
使用 Compose 构建横屏电视页面，优先满足遥控器焦点和大屏布局。

#### 场景: 首页连接
用户输入 Emby 地址和账号后：
- 能触发认证与媒体列表加载。
- 加载失败时显示可读错误。

#### 场景: 播放页
用户选择媒体或样例后：
- Media3 播放器进入全屏。
- AkDanmaku View 覆盖在播放器上方。
- 返回键退出播放页。

## API接口
无外部 API。

## 数据模型
使用 `HomeUiState` 和 `PlaybackSource`。

## 依赖
- data
- player
- danmaku

## 变更历史
- [202605201342_emby_tv_init](../../history/2026-05/202605201342_emby_tv_init/) - 初始化 TV UI 与播放页面。
