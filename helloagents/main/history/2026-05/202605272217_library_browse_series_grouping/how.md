# 技术设计: 媒体库封面、列表页与剧集维度聚合

目录: `helloagents/main/history/2026-05/202605272217_library_browse_series_grouping/`

---

## 方案选择

采用“最小闭环 + 渐进扩展”方案：优先修复图片模型和首页聚合，再新增媒体库资源列表页。保持现有 MVVM、Repository、Flow 和 Compose 结构，不引入导航框架和数据库。

备选方案:

- **方案 A: 最小闭环接入（推荐）**
  - 使用现有 `HomeViewModel` 管理首页和库列表状态。
  - 新增 `LibraryContentUiState` 与列表页 Composable。
  - Repository 新增 `loadLibraryContent()` 与剧集聚合方法。
  - 优点: 改动可控，符合当前项目结构，能快速解决三个问题。
  - 缺点: HomeViewModel 职责会进一步变大，后续媒体详情页可能需要拆分。
- **方案 B: 引入 Navigation + 独立 LibraryViewModel**
  - 优点: 页面职责更清晰。
  - 缺点: 当前工程还没有导航层，改动范围大，不适合作为本次缺陷闭环。
- **方案 C: 只修首页，不做列表页**
  - 优点: 快。
  - 缺点: 不满足用户第二个问题，主路径仍不完整。

本次采用方案 A。

## 数据获取策略

### 图片兜底策略

当前只使用 `ImageTags["Primary"]`、`ImageTags["Thumb"]` 和 `BackdropImageTags[0]`。需要扩展为多级兜底：

1. 当前条目 `ImageTags.Primary` 或 `PrimaryImageTag`。
2. 当前条目 `ImageTags.Thumb`。
3. 当前条目 `BackdropImageTags[0]`。
4. 父级条目图片:
   - `ParentThumbItemId + ParentThumbImageTag`
   - `ParentBackdropItemId + ParentBackdropImageTags[0]`
   - `SeriesId + SeriesPrimaryImageTag`
5. 无 tag 但有 item id 时，允许构造 `/Items/{id}/Images/{type}` 作为保守兜底。
6. 仍缺失时显示本地占位。

图片 URL 构造器新增:

```kotlin
buildPrimaryImageUrl(serverUrl, itemId, tag, allowUntagged = true)
buildThumbImageUrl(serverUrl, itemId, tag, allowUntagged = true)
buildBackdropImageUrl(serverUrl, itemId, tag, allowUntagged = true)
```

### 媒体库资源列表

新增 Repository 方法:

```kotlin
suspend fun loadLibraryContent(
    session: EmbySession,
    deviceId: String,
    library: EmbyLibrarySummary,
    limit: Int = 60,
): Result<EmbyLibraryContent>
```

按库类型选择 IncludeItemTypes:

| CollectionType | IncludeItemTypes | 说明 |
|----------------|------------------|------|
| movies | Movie | 电影列表 |
| tvshows | Series | 剧集列表 |
| mixed/unknown | Movie,Series | 优先不展示 Episode 明细 |

请求:

```text
GET Users/{userId}/Items?ParentId={libraryId}&Recursive=true&IncludeItemTypes={types}&SortBy=SortName&SortOrder=Ascending&Limit=60
```

### 首页按剧集维度展示

首页按库最新资源改为按库类型处理：

| CollectionType | 首页横排维度 | 推荐请求 |
|----------------|--------------|----------|
| movies | Movie | `Items/Latest?ParentId={libraryId}&IncludeItemTypes=Movie&Limit=8` |
| tvshows | Series | `Items/Latest?ParentId={libraryId}&IncludeItemTypes=Episode&GroupItems=true&Limit=8` |
| mixed/unknown | Movie/Series | 先 Movie/Series，必要时 fallback |

若 `GroupItems=true` 返回仍是 Episode 或缺少 Series 图片，则 fallback:

1. 使用 Episode 的 `SeriesId`/`SeriesName` 聚合。
2. 每个 Series 只保留最新 Episode 的日期排序位置。
3. 使用 `SeriesPrimaryImageTag`、`ParentThumbImageTag` 或 SeriesId 的 Primary 图片作为剧集封面。

### 剩余播放集数角标

优先从 Emby 返回字段获取未播放集数；如果字段不可用，本次以可验证的保守方案实现：

- DTO 增加 `ChildCount`、`RecursiveItemCount`、`UserData.UnplayedItemCount`、`SeriesId`、`SeriesPrimaryImageTag`、`ParentThumbItemId`、`ParentThumbImageTag` 等字段。
- `MediaItemSummary` 增加:
  - `seriesId: String?`
  - `unplayedItemCount: Int?`
  - `cardKind: MediaCardKind` 或 `type` 扩展语义
- UI `MediaCardUiModel` 增加:
  - `cornerBadge: String?`
- 映射规则:
  - Series 且 `unplayedItemCount > 0` -> `剩 {n} 集`
  - Movie -> 不显示剩余集数角标
  - Episode 明细 fallback -> 不显示或显示 Episode badge，不冒充 Series

## UI 设计

### 首页

- 媒体库卡片仍为横向 `LazyRow`。
- `LibraryCard.onClick` 从提示改为进入库列表页。
- 首页按库最新 section 的标题保持 `{库名} · 最新入库`，但 tvshows 库卡片按剧集展示。
- `MediaPosterCard` 支持右上角类型 badge 和另一个角标:
  - 类型 badge: Movie / Series
  - 剩余角标: `剩 3 集`

### 媒体库资源列表页

新增 `LibraryContentScreen`，复用现有深色玻璃风格：

- 顶部栏: 返回按钮、媒体库名称、数量/类型提示。
- 内容: TV 友好的固定网格，建议 `LazyVerticalGrid` 或 LazyColumn + 行分组；若 TV Compose 网格依赖不便，先用 `LazyColumn` 每行 5 个卡片。
- 空状态: “该媒体库暂无可展示资源”。
- 加载状态和错误状态可遥控重试。
- Back 返回首页，焦点回到对应媒体库卡片或菜单区域。

## 状态模型

新增领域模型:

```kotlin
data class EmbyLibraryContent(
    val library: EmbyLibrarySummary,
    val items: List<MediaItemSummary>,
)
```

扩展 `HomeUiState`:

```kotlin
val selectedLibrary: EmbyLibrarySummary? = null
val libraryContent: EmbyLibraryContent? = null
val libraryContentLoading: Boolean = false
val libraryContentError: String? = null
```

新增 ViewModel 方法:

```kotlin
fun openLibrary(libraryId: String)
fun closeLibrary()
fun retryLibrary()
```

## 代码落点

- `app/src/main/java/com/embytv/data/remote/EmbyApi.kt`
  - 扩展 `getLatestItems` 支持 `ParentId`、`GroupItems`。
  - 扩展 `getItemsByParent` 支持 `StartIndex`、动态 IncludeItemTypes、SortBy/SortOrder。
- `app/src/main/java/com/embytv/data/remote/dto/EmbyItemDtos.kt`
  - 增加图片和剧集聚合相关字段。
- `app/src/main/java/com/embytv/data/repository/EmbyStreamUrlBuilder.kt`
  - 支持无 tag 图片 URL 和父级图片 URL。
- `app/src/main/java/com/embytv/domain/model/MediaItemSummary.kt`
  - 增加 Series 聚合字段、未播放集数字段和 `EmbyLibraryContent`。
- `app/src/main/java/com/embytv/data/repository/EmbyRepository.kt`
  - 修复图片兜底。
  - 新增 `loadLibraryContent()`。
  - 改造首页按库最新资源为电影按 Movie、剧集按 Series。
- `app/src/main/java/com/embytv/ui/home/HomeDashboardModels.kt`
  - 增加角标字段和 Series 映射。
- `app/src/main/java/com/embytv/ui/components/CinematicComponents.kt`
  - `MediaPosterCard` 增加角标展示。
- `app/src/main/java/com/embytv/ui/home/HomeViewModel.kt`
  - 增加打开/关闭媒体库列表状态和加载方法。
- `app/src/main/java/com/embytv/ui/home/HomeScreen.kt`
  - 首页/媒体库列表页切换。

## 测试策略

### RED

- Repository 测试:
  - 图片字段缺失 `ImageTags.Primary` 但有 `PrimaryImageTag` 时仍生成 Primary URL。
  - Episode 有 `SeriesId/SeriesPrimaryImageTag` 时聚合为 Series 卡片。
  - tvshows 库首页最新资源请求携带 `GroupItems=true`。
  - `loadLibraryContent()` 对 movies 使用 Movie，对 tvshows 使用 Series。
- Mapper 测试:
  - Series 卡片显示剧集标题、Series 图片和 `剩 n 集` 角标。
  - Movie 卡片不显示剩余集数角标。
  - 媒体库卡片 enabled 且不再输出“详情暂未支持”。
- ViewModel 状态测试:
  - `openLibrary()` 设置 loading，成功后进入列表状态。
  - `closeLibrary()` 返回首页并清理错误。

### GREEN

- 实现 DTO/API/Repository。
- 实现 UI 模型和组件角标。
- 实现列表页和遥控器返回。

### VERIFY

- `.\gradlew.bat :app:testDebugUnitTest`
- `.\gradlew.bat :app:assembleDebug`
- 安装到 TV/模拟器手工验证:
  - 首页封面显示。
  - OK 进入媒体库列表。
  - Back 返回首页。
  - 剧集库横排按剧集去重并显示剩余集数。

## 安全与性能

- 不记录真实 token、密码、完整播放 URL 或私有媒体标题。
- 首页每个媒体库最新资源固定 Limit=8。
- 媒体库列表首屏固定 Limit=60；分页后续单独规划。
- 图片 URL 不携带 access token；沿用 Emby 图片公开/会话访问能力。
- 本次不做全库 Episode 扫描统计剩余集数，避免大库性能风险；优先使用 Emby 返回的未播放计数字段。

## 回滚策略

- 可回滚 `HomeScreen` 的列表页切换，恢复媒体库卡片提示。
- 可将 tvshows 最新资源临时回退为 `IncludeItemTypes=Episode`，但保留图片兜底修复。
- DTO 扩展字段为可空，不影响既有接口解析。

## ADR-006: 首页剧集库按 Series 维度展示

- **状态:** 提议
- **日期:** 2026-05-27
- **决策:** 剧集库首页横排不再按 Episode 展示，改为按 Series 聚合，并显示剩余未播放集数角标。
- **原因:** TV 首页需要按作品维度浏览；按单集维度会造成重复剧集卡片和浏览噪声。
- **后果:** Repository 需要处理 `GroupItems=true` 返回差异和本地 fallback 聚合，数据模型需要增加 Series 与未播放计数字段。
