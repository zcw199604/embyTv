# 数据模型

## 概述
当前不引入本地数据库，数据以内存状态和领域模型传递。持久化账号、服务器和播放历史属于后续切片。

---

## 领域模型

### ServerConfig
| 字段 | 类型 | 说明 |
|------|------|------|
| baseUrl | String | Emby 服务器地址 |
| username | String | 用户名 |
| password | String | 密码 |
| deviceId | String | 当前客户端设备 ID |

### EmbySession
| 字段 | 类型 | 说明 |
|------|------|------|
| serverUrl | String | 服务器地址 |
| userId | String | 用户 ID |
| accessToken | String | 访问令牌 |
| serverId | String? | 服务器 ID |

### MediaItemSummary
| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | Emby 条目 ID |
| name | String | 标题 |
| type | String | Movie 或 Episode |
| overview | String? | 简介 |
| imageUrl | String? | 主图地址 |

### PlaybackSource
| 字段 | 类型 | 说明 |
|------|------|------|
| itemId | String | Emby 条目 ID |
| title | String | 标题 |
| streamUrl | String | Media3 播放地址 |
| danmaku | List<DanmakuCue> | 弹幕列表 |

### DanmakuCue
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 弹幕 ID |
| timeMs | Long | 相对播放时间 |
| text | String | 弹幕文本 |
| color | Int | RGB 颜色 |
| mode | DanmakuMode | 滚动、顶部或底部 |
