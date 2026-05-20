# API 手册

## 概述
当前客户端通过 Emby HTTP API 完成认证和媒体列表读取。请求头使用 `X-Emby-Authorization` 标识客户端和设备，认证后同时在请求头和查询参数中携带令牌。

## 认证方式
- 登录接口返回 `AccessToken`。
- 后续请求使用 `X-Emby-Authorization` 的 `Token` 字段，并通过 `api_key` 查询参数兼容 Emby 流媒体接口。

---

## 接口列表

### Emby 认证

#### POST Users/AuthenticateByName
**描述:** 使用用户名密码获取 Emby 访问令牌。

**请求参数:**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| Username | string | 是 | Emby 用户名 |
| Pw | string | 否 | Emby 密码 |

**响应:**
```json
{
  "AccessToken": "token",
  "ServerId": "server-id",
  "User": {
    "Id": "user-id",
    "Name": "name"
  }
}
```

### 媒体列表

#### GET Users/{userId}/Items
**描述:** 递归获取 Movie 和 Episode 条目。

**请求参数:**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | string | 是 | 当前用户 ID |
| Recursive | boolean | 是 | 当前固定为 true |
| IncludeItemTypes | string | 是 | 当前为 Movie,Episode |
| Fields | string | 是 | Overview,PrimaryImageAspectRatio,ImageTags |

### 播放流

#### GET Videos/{itemId}/stream
**描述:** Media3 使用该地址播放视频流。

**请求参数:**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| itemId | string | 是 | 媒体条目 ID |
| Static | boolean | 是 | 当前固定为 true |
| api_key | string | 是 | Emby AccessToken |
