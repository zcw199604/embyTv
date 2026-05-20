# player

## 目的
封装 Media3 播放器创建与扩展渲染器配置。

## 模块概述
- **职责:** 创建 ExoPlayer，配置 OkHttp 数据源，启用扩展渲染器优先策略。
- **状态:** 🚧开发中
- **最后更新:** 2026-05-20

## 规范

### 需求: Media3 播放初始化
**模块:** player
播放器由 `Media3PlayerFactory` 统一创建，不在 UI 中直接拼装底层依赖。

#### 场景: FFmpeg 扩展可用
当 `app/libs` 中存在 Media3 FFmpeg 扩展 AAR 且类可被反射加载时：
- Media3 优先使用扩展渲染器。

#### 场景: FFmpeg 扩展不可用
当本地 AAR 不存在时：
- 播放器仍使用 Media3 默认渲染器工作。

## API接口
无外部 API。

## 数据模型
使用 `PlaybackSource.streamUrl`。

## 依赖
- core.network
- Media3

## 变更历史
- [202605201342_emby_tv_init](../../history/2026-05/202605201342_emby_tv_init/) - 初始化 Media3 播放工厂。
