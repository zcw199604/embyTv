# core

## 目的
提供基础设施、网络客户端与依赖装配。

## 模块概述
- **职责:** OkHttp 配置、Retrofit 工厂依赖、AppContainer 手动依赖注入。
- **状态:** 🚧开发中
- **最后更新:** 2026-05-20

## 规范

### 需求: 基础设施初始化
**模块:** core
依赖创建集中在 `DefaultAppContainer`，避免页面层直接构造网络和播放器对象。

#### 场景: 应用启动
应用启动后：
- `EmbyTvApplication` 初始化 `DefaultAppContainer`。
- `MainActivity` 将容器传入 Compose 根组件。

## API接口
无外部 API。

## 数据模型
无独立数据模型。

## 依赖
- data
- player
- danmaku

## 变更历史
- [202605201342_emby_tv_init](../../history/2026-05/202605201342_emby_tv_init/) - 初始化核心装配。
