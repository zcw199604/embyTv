# danmaku

## 目的
将项目领域弹幕模型转换为 AkDanmaku 数据结构。

## 模块概述
- **职责:** 创建 `DanmakuPlayer`，将 `DanmakuCue` 转换为 `DanmakuItemData`。
- **状态:** 🚧开发中
- **最后更新:** 2026-05-20

## 规范

### 需求: AkDanmaku 弹幕初始化
**模块:** danmaku
UI 不直接依赖 AkDanmaku 数据构造细节，通过 `AkDanmakuBridge` 适配。

#### 场景: 播放页展示弹幕
播放页收到 `PlaybackSource.danmaku` 后：
- 转换为 AkDanmaku 列表。
- 绑定 `DanmakuView`。
- 按播放时间显示滚动、顶部或底部弹幕。

## API接口
无外部 API。

## 数据模型
使用 `DanmakuCue` 和 `DanmakuMode`。

## 依赖
- domain
- AkDanmaku

## 变更历史
- [202605201342_emby_tv_init](../../history/2026-05/202605201342_emby_tv_init/) - 初始化 AkDanmaku 桥接。
