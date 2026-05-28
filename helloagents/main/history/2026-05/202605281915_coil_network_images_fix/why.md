# 变更提案: 修复 Emby 封面图片不显示

## 需求背景
当前 TV 客户端中所有媒体库封面、媒体卡片封面和详情页图片都无法显示。代码已经从 Emby 返回数据中构造出图片 URL，Android 也已放开局域网 HTTP 明文流量；使用测试服务器直接请求图片端点可返回 `200 image/png`。进一步检查 Gradle 运行时依赖发现当前只引入了 `io.coil-kt.coil3:coil-compose:3.4.0`，没有引入 Coil 3 的网络图片加载模块。

Coil 3 默认不再内置 HTTP/HTTPS 网络加载能力，必须显式添加 `coil-network-okhttp` 或 Ktor 网络模块。当前 `AsyncImage(model = imageUrl)` 因缺少网络 fetcher，无法加载 Emby HTTP 图片 URL，导致所有封面统一显示占位图。

## 变更内容
1. 为 Coil 3 增加 OkHttp 网络加载依赖 `io.coil-kt.coil3:coil-network-okhttp:3.4.0`。
2. 保持现有 `NetworkBackdropImage`、图片 URL 构造逻辑和 Emby API 调用不变。
3. 增加最小构建验证，确认 APK 运行时依赖中包含 `coil-network-okhttp`。
4. 更新知识库，记录 Coil 3 网络图片依赖要求，避免后续误删。

## 范围边界
- **范围内:** Gradle 版本目录依赖补充、App 模块依赖补充、构建验证、知识库同步。
- **范围外:** 重写图片组件、增加图片缓存策略、增加认证图片请求头、改造 Emby 图片 URL、修改版本号、提交推送。
- **拆分说明:** 本方案只处理“所有网络封面无法加载”的根因修复；失败占位 UI、图片加载错误日志、认证图片请求头可作为后续增强独立处理。

## 影响范围
- **模块:** build、ui、知识库。
- **文件:** `gradle/libs.versions.toml`、`app/build.gradle.kts`、`helloagents/main/wiki/modules/ui.md`、`helloagents/main/CHANGELOG.md`。
- **API:** 无 Emby API 变更。
- **数据:** 无数据模型变更。

## 核心场景

### 需求: Emby 网络封面显示
**模块:** ui
用户登录 Emby 后，首页、媒体库列表、收藏页和详情页中的网络图片应能通过 Coil 正常加载。

#### 场景: 首页媒体库封面显示
登录成功并进入首页后：
- `LibraryCard` 使用 `library.imageUrl` 加载 Emby 图片 URL。
- Coil 具备 HTTP/HTTPS 网络 fetcher。
- 图片 URL 有效时展示真实封面，而不是统一占位。

#### 场景: 媒体卡片封面显示
首页横排、媒体库列表和收藏列表展示媒体卡片时：
- `MediaPosterCard` 使用 `MediaCardUiModel.imageUrl` 加载真实图片。
- Movie、Series、Episode 缩略图可以按现有 URL 兜底顺序显示。
- 图片缺失时仍显示占位图。

#### 场景: 详情页封面显示
打开 Movie/Series 详情页后：
- 详情页海报和季卡片继续复用 `NetworkBackdropImage`。
- 有效 Emby 图片 URL 应显示真实图片。

## 风险评估
- **风险:** 新增依赖与当前 OkHttp 版本存在传递依赖差异。
- **缓解:** 使用与 Coil Compose 相同版本号 `3.4.0`，并运行 `:app:dependencies`、`:app:testDebugUnitTest`、`:app:assembleDebug` 验证。
- **风险:** 部分 Emby 部署需要图片接口携带 token，新增网络模块后仍可能有个别封面加载失败。
- **缓解:** 当前测试服务器图片端点不带 token 可返回 200；本方案先修复全量无法加载的网络 fetcher 根因。认证图片请求头作为后续增强，不混入本次最小修复。
