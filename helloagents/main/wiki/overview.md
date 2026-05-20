# embyTv

> 本文件包含项目级别的核心信息。详细的模块文档见 `modules/` 目录。

---

## 1. 项目概述

### 目标与背景
初始化一个支持 Emby 媒体播放和弹幕覆盖的 Android TV 客户端，面向电视遥控器和横屏播放场景。

### 范围
- **范围内:** Android TV 工程骨架、Emby 认证与媒体列表、Media3 播放入口、AkDanmaku 弹幕入口、MVVM 基础分层。
- **范围外:** 完整账号持久化、复杂媒体库导航、字幕搜索、真实第三方弹幕源解析、投屏、离线缓存。

### 干系人
- **负责人:** 项目开发者。

---

## 2. 模块索引

| 模块名称 | 职责 | 状态 | 文档 |
|---------|------|------|------|
| ui | Compose TV 页面与 ViewModel | 🚧开发中 | [modules/ui.md](modules/ui.md) |
| data | Emby API、DTO、Repository | 🚧开发中 | [modules/data.md](modules/data.md) |
| player | Media3 播放器工厂 | 🚧开发中 | [modules/player.md](modules/player.md) |
| danmaku | AkDanmaku 领域桥接 | 🚧开发中 | [modules/danmaku.md](modules/danmaku.md) |
| core | DI、网络与基础设施 | 🚧开发中 | [modules/core.md](modules/core.md) |

---

## 3. 快速链接
- [技术约定](../project.md)
- [架构设计](arch.md)
- [API 手册](api.md)
- [数据模型](data.md)
- [变更历史](../history/index.md)
