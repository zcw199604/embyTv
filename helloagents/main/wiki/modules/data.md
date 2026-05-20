# data

## 目的
封装 Emby API、DTO 与 Repository。

## 模块概述
- **职责:** Retrofit 接口定义、Emby 登录、媒体列表读取、播放地址构造。
- **状态:** 🚧开发中
- **最后更新:** 2026-05-20

## 规范

### 需求: Emby 基础接入
**模块:** data
Repository 对 UI 暴露 `Result`，避免 UI 层直接处理 Retrofit 异常。

#### 场景: 登录成功
Emby 返回用户 ID 和访问令牌后：
- 生成 `EmbySession`。
- 使用 session 加载 Movie 和 Episode 列表。

#### 场景: 构造播放地址
选择媒体后：
- 生成 `Videos/{itemId}/stream?Static=true&api_key=...`。
- 对 itemId 和 token 进行 URL 编码。

## API接口
见 [API 手册](../api.md)。

## 数据模型
见 [数据模型](../data.md)。

## 依赖
- core.network
- domain

## 变更历史
- [202605201342_emby_tv_init](../../history/2026-05/202605201342_emby_tv_init/) - 初始化 Emby API 与 Repository。
