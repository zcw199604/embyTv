# data

## 目的
封装 Emby API、DTO、Repository、凭证存储与播放地址构造。

## 模块概述
- **职责:** Retrofit 接口定义、Emby 登录、首页 Dashboard 聚合、播放详情读取、播放地址构造。
- **状态:** 🚧开发中
- **最后更新:** 2026-05-27

## 规范

### 需求: Emby 基础接入
**模块:** data
Repository 对 UI 暴露 `Result`，避免 UI 层直接处理 Retrofit 异常。

#### 场景: 登录成功
Emby 返回用户 ID 和访问令牌后：
- 生成 `EmbySession`。
- 保存 `serverUrl`、`userId`、`username`、`accessToken`、`serverId`、`deviceId` 和保存时间。
- 使用 session 加载首页 Dashboard 聚合数据。

#### 场景: 首页 Dashboard
登录成功或恢复凭证后：
- 调用 `Users/{userId}/Views` 读取用户可见媒体库。
- 按媒体库调用 `Users/{userId}/Items?ParentId=...&Limit=0` 读取真实视频数量。
- 调用 `Users/{userId}/Items/Resume` 读取继续观看。
- 调用 `Users/{userId}/Items/Latest` 作为继续观看为空时的最近入库兜底。
- 不在首页首屏全量拉取全部 Movie/Episode。

#### 场景: 播放详情
选择媒体后：
- 调用 `Items/{itemId}/PlaybackInfo?UserId=...` 读取真实媒体源、Video/Audio/Subtitle streams。
- 生成 `PlaybackSource.details`，供播放器 OSD 展示容器、编码、画质、音轨和字幕状态。

#### 场景: 构造播放地址
选择媒体后：
- 生成 `Videos/{itemId}/stream?Static=true&api_key=...`。
- 对 itemId 和 token 进行 URL 编码。
- 不在日志或错误文案中输出完整播放 URL。

## API接口
见 [API 手册](../api.md)。

## 数据模型
见 [数据模型](../data.md)。

## 依赖
- core.network
- domain

## 变更历史
- [202605271602_emby_real_data_replacement](../../history/2026-05/202605271602_emby_real_data_replacement/) - 首页和播放器可见数据替换为 Emby 真实 API 数据。
- [202605271514_emby_server_mobile_sync](../../history/2026-05/202605271514_emby_server_mobile_sync/) - 保存 Emby token 凭证和用户名展示字段，不保存密码。
- [202605201342_emby_tv_init](../../history/2026-05/202605201342_emby_tv_init/) - 初始化 Emby API 与 Repository。
