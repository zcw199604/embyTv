# 变更提案: TV 客户端性能优化

## 需求背景
当前 Emby TV 客户端已经具备真实数据首页、媒体库列表、详情页、播放和播放状态上报能力。只读性能审查确认没有灾难级性能问题，但首屏加载和冷启动路径仍有明显优化空间：已有 token 时仍先启动手机同步服务，首页 Dashboard 会串行完成多组 Emby 请求后才刷新 UI，Retrofit service 会在每个 Repository 方法中重复创建，图片请求也缺少尺寸约束。

## 产品分析

### 目标用户与场景
- **用户群体:** Android TV / 电视盒子上的 Emby 用户。
- **使用场景:** 冷启动打开 App、恢复已保存 token、进入首页浏览媒体库、滚动浏览图片卡片、播放过程中同步 Emby 后台状态。
- **核心痛点:** 首页首屏等待时间偏长，媒体库较多时网络请求数量放大，电视设备图片解码和内存压力可能导致卡顿。

### 价值主张与成功指标
- **价值主张:** 在不改变功能行为的前提下，降低冷启动和首页首屏等待，减少重复对象创建与不必要网络/图片开销。
- **成功指标:** 已保存 token 冷启动不再启动扫码服务；首页 Dashboard 主要请求支持并发或分阶段加载；图片 URL 支持按用途限制尺寸；单元测试和 debug 构建通过。

### 人文关怀
性能优化应避免牺牲可理解的加载状态。网络慢或 Emby 服务器响应慢时，用户仍应看到明确加载/错误提示；凭证失效时仍应回到扫码或手动配置路径。

## 变更内容
1. 调整启动恢复顺序：优先读取保存凭证，只有无凭证或凭证失效时启动手机扫码同步服务。
2. 优化首页 Dashboard 加载：减少完整串行阻塞，库计数和按库最新资源采用受控并发或分阶段加载策略。
3. 缓存 Emby API service：避免每个 Repository 方法重复构造 Retrofit/OkHttp wrapper。
4. 图片 URL 尺寸化：为 poster、backdrop、library/detail 等用途追加合适的 `MaxWidth/MaxHeight/quality` 参数。
5. 播放上报排队化作为后续增强：评估以单队列顺序发送和合并过期 progress，避免弱网下并发堆积。

## 范围边界
- **范围内:** 冷启动恢复、首页 Dashboard 数据加载、EmbyApiFactory 复用、图片 URL 构造、必要测试与知识库同步。
- **范围外:** 不改 Emby 认证协议；不改媒体详情数据字段；不重构整体 MVVM；不新增数据库缓存；播放上报 actor 如风险较高可拆为后续独立方案。
- **拆分说明:** 本方案以高收益低风险性能优化为主线；播放上报队列化与 UI 列表 row model 优化属于次级任务，可在核心优化通过后执行或延期。

## 影响范围
- **模块:** `ui/home`、`data/repository`、`data/remote`、`core/network`、`ui/components`、`player`。
- **文件:** `HomeViewModel.kt`、`EmbyRepository.kt`、`EmbyApiFactory.kt`、`EmbyStreamUrlBuilder.kt`、`CinematicComponents.kt`、相关单元测试与知识库。
- **API:** 不新增外部 API；内部 `EmbyApiProvider` 可增加缓存行为但接口优先保持兼容。
- **数据:** 不变更持久化结构，不迁移本地凭证。

## 核心场景

### 需求: 冷启动恢复性能优化
**模块:** ui/home
已有保存 token 时，启动应优先恢复凭证并加载首页，不应先启动手机扫码同步服务。

#### 场景: 已保存 token 冷启动
App 启动并读取到有效 `SavedEmbyCredential` 时：
- 不启动 NanoHTTPD 手机同步服务。
- 使用保存的 `accessToken` 构造 `EmbySession` 并加载 Dashboard。
- 如果 token 失效，清除凭证后再启动手机同步服务。

### 需求: 首页 Dashboard 首屏优化
**模块:** data/repository
媒体库较多时，首页加载不应被所有库计数和按库 latest 串行阻塞。

#### 场景: 多媒体库首页加载
加载首页时：
- `Views`、继续观看和最近入库优先完成。
- 媒体库计数和按库 latest 采用受控并发或可分阶段更新。
- 请求失败时保留可用部分数据或明确错误，不静默吞掉核心失败。

### 需求: API 与图片请求开销优化
**模块:** data/remote, data/repository, ui/components
重复 service 构造和超大图片下载应被控制。

#### 场景: 重复 Repository 请求
调用多个 Repository 方法时：
- 相同 `baseUrl + accessToken` 复用 `EmbyApi` 或 Retrofit service。
- 登出、切换服务器或 token 变化不会复用旧 token。

#### 场景: 图片加载
展示首页、媒体库、详情页图片时：
- 图片 URL 按展示用途携带尺寸参数。
- 缺图兜底逻辑保持不变。
- TV 大屏画质不因尺寸过小明显变糊。

## 风险评估
- **风险:** Dashboard 并发请求可能增加 Emby 服务器瞬时压力。
- **缓解:** 使用受控并发上限，避免对每个媒体库无限并发。
- **风险:** API 缓存可能导致 token 切换后误用旧 service。
- **缓解:** 缓存 key 必须包含 normalized baseUrl 和 token；清凭证或新 token 自动生成新 key。
- **风险:** 图片尺寸参数设置过小导致大屏模糊。
- **缓解:** 按用途设置保守尺寸，详情页使用高于首页卡片的尺寸。
