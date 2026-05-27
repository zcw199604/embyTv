# ui

## 目的
提供 Android TV Compose 页面、输入与播放导航。

## 模块概述
- **职责:** Setup 页面负责 Emby 连接，Home 页面负责媒体中心展示和播放入口，Player 页面负责 Media3、Compose OSD 与 AkDanmaku 的组合展示。
- **状态:** 🚧开发中
- **最后更新:** 2026-05-27

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

### 需求: Cinematic Glass TV 体验
**模块:** ui
基于设计稿 `stitch_emby_tv_interface_redesign.zip` 落地深色玻璃拟态 TV 体验，覆盖配置、首页和播放主链路。

#### 场景: 服务器配置
未连接 Emby 时：
- 首屏展示快速配对占位码和手动服务器配置区。
- 手动配置区复用 `HomeViewModel.connect()` 完成 Emby 认证。
- 密码使用输入遮罩，不在错误文案或日志中输出敏感内容。

#### 场景: 首页媒体中心
认证成功后：
- 首页展示顶部栏、媒体库卡片、继续观看横向媒体行和迷你播放条。
- 媒体卡片通过 Coil Compose 加载 `MediaItemSummary.imageUrl`。
- Movies、TV Shows、Collections、Settings 等尚未实现入口保持禁用/占位。

#### 场景: 播放 OSD
播放页进入后：
- `PlayerView` 关闭默认控制器，由 Compose OSD 展示标题、格式信息、进度、快进/快退、播放/暂停、音轨/字幕/弹幕入口。
- OK/方向键唤起 OSD；Back 在 OSD 可见时先隐藏，再次 Back 退出播放页。
- 弹幕开关和播放暂停状态同步到 AkDanmaku。

### 需求: TV 遥控器完整操作
**模块:** ui
所有可见操作必须能通过方向键、OK/Enter 和 Back 完成，未实现能力必须给出禁用态或明确提示。

#### 场景: 全局按键契约
用户使用遥控器时：
- 方向键优先交给当前聚焦控件处理，根容器不抢占 OSD 内部控件的方向键和 OK/Enter。
- Back 在抽屉打开时关闭抽屉，在播放 OSD 可见时隐藏 OSD，OSD 隐藏时退出播放页。
- 禁用入口可显示原因，OK/Enter 不允许空响应。

#### 场景: 首页与抽屉
首页打开抽屉后：
- 抽屉请求初始焦点并形成焦点组。
- Back 或关闭按钮关闭抽屉，关闭后焦点返回菜单按钮。
- 未实现的导航项和媒体库二级页显示“暂未支持”提示。

#### 场景: 播放 OSD
OSD 显示后：
- 播放/暂停按钮获取初始焦点。
- 快退、快进、播放/暂停、弹幕开关支持 OK/Enter。
- 上一集、下一集、Audio、Subtitles 在真实功能未实现前显示禁用提示。

## API接口
无外部 API。

## 数据模型
使用 `HomeUiState`、`HomeDashboardUiModel`、`DrawerUiState`、`MediaCardUiModel`、`PlayerOsdState` 和 `PlaybackSource`。

## 依赖
- data
- player
- danmaku
- Coil Compose

## 变更历史
- [202605271434_remote_control_support](../../history/2026-05/202605271434_remote_control_support/) - 补齐 TV 遥控器焦点、Back、禁用反馈和播放 OSD 操作闭环。
- [202605271353_tv_ui_redesign_core](../../history/2026-05/202605271353_tv_ui_redesign_core/) - 落地 Cinematic Glass 配置页、首页媒体中心和播放 OSD。
- [202605201342_emby_tv_init](../../history/2026-05/202605201342_emby_tv_init/) - 初始化 TV UI 与播放页面。
