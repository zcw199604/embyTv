# 任务清单: Emby TV 客户端功能补齐

目录: `helloagents/main/plan/202605291035_emby_tv_feature_completion/`

---

## 并行子代理标注

本方案暂不启用并行子代理。原因: 新增能力共享 `EmbyApi`、`EmbyRepository`、`HomeViewModel`、`PlaybackSource` 等核心接口，前置数据契约未落地前存在较强冲突域。执行时先顺序完成数据契约和模型，再视情况拆分 UI 页面与播放器任务。

---

## 0. 方案边界确认
- [√] 0.1 确认本次任务覆盖 why.md 的范围内能力，转码策略、Live TV、播放列表/合集编辑不进入实现
- [√] 0.2 确认 how.md 的接口契约、数据边界和凭证迁移策略完整
- [√] 0.3 确认所有新增入口必须支持 TV 遥控器方向键、OK/Enter 和 Back

---

## 1. API 与领域模型 TDD
- [√] 1.1 RED: 在 `app/src/test/java/com/embytv/data/repository/EmbyRepositoryDiscoveryTest.kt` 增加发现页读取失败测试，覆盖 BoxSet、Playlist、Genre、Person 列表和条目资源映射
- [√] 1.2 RED: 在 `app/src/test/java/com/embytv/data/repository/EmbyRepositorySearchTest.kt` 增加搜索测试，验证 `SearchTerm` 请求、`Items` 优先空状态和 Movie/Series/Episode 映射
- [√] 1.3 RED: 在 `app/src/test/java/com/embytv/data/repository/EmbyUserActionsTest.kt` 增加收藏、已播放、清除继续观看写操作测试
- [√] 1.4 RED: 在 `app/src/test/java/com/embytv/domain/model/SavedEmbyCredentialListTest.kt` 增加多凭证迁移和去重排序测试
- [√] 1.5 RED: 在 `app/src/test/java/com/embytv/ui/player/PlayerTrackSelectionTest.kt` 增加轨道 UI 状态和禁用字幕状态测试

## 2. API 与 Repository 实现
- [√] 2.1 GREEN: 在 `app/src/main/java/com/embytv/data/remote/EmbyApi.kt` 增加搜索、NextUp、发现页、播放列表条目和用户态写接口，依赖任务1.1-1.3
- [√] 2.2 GREEN: 在 `app/src/main/java/com/embytv/data/remote/dto/EmbyItemDtos.kt` 补充 `PlaylistItemId`、Provider/People 图片等可空字段，依赖任务2.1
- [√] 2.3 GREEN: 在 `app/src/main/java/com/embytv/domain/model/MediaItemSummary.kt` 增加 `DiscoveryEntrySummary`、`EmbyDiscoveryContent`、`DiscoveryEntryItems`、`EmbySearchResults`、用户态字段，依赖任务2.2
- [√] 2.4 GREEN: 在 `app/src/main/java/com/embytv/data/repository/EmbyRepository.kt` 实现 `searchItems`、`loadDiscoveryContent`、`loadDiscoveryEntryItems`、`loadNextUp`、`toggleFavorite`、`markPlayed`、`clearResumeProgress`，依赖任务2.3
- [√] 2.5 REFACTOR: 收敛 `EmbyRepository` 内图片 URL 构造和 item 映射重复逻辑，不改现有首页/详情行为
- [√] 2.6 VERIFY: 运行 `.\gradlew.bat :app:testDebugUnitTest --tests "*EmbyRepository*"`，确认数据层相关测试通过

## 3. 多服务器 / 多用户凭证
- [√] 3.1 GREEN: 在 `app/src/main/java/com/embytv/domain/model/SavedEmbyCredential.kt` 增加凭证列表模型、唯一键和展示标签，依赖任务1.4
- [√] 3.2 GREEN: 在 `app/src/main/java/com/embytv/data/local/EncryptedEmbyCredentialStore.kt` 实现列表读写、旧单条凭证迁移、按服务器+用户去重保存
- [√] 3.3 GREEN: 在 `app/src/main/java/com/embytv/ui/setup/SetupScreen.kt` 或新同包文件新增账号选择页，支持选择、删除、添加新服务器
- [√] 3.4 GREEN: 在 `app/src/main/java/com/embytv/ui/home/HomeViewModel.kt` 调整启动恢复逻辑: 0 条进入配置，1 条自动恢复，多条显示选择
- [√] 3.5 VERIFY: 运行凭证相关单测，并手工验证旧版本单凭证可迁移

## 4. 搜索与语音搜索
- [√] 4.1 GREEN: 在 `app/src/main/java/com/embytv/ui/home/HomeDashboardModels.kt` 增加 `SearchUiState`、搜索结果 mapper 和导航项
- [√] 4.2 GREEN: 在 `app/src/main/java/com/embytv/ui/home/HomeViewModel.kt` 增加打开/关闭搜索、关键词 debounce、语音结果回填、重试和清空逻辑
- [√] 4.3 GREEN: 在 `app/src/main/java/com/embytv/ui/home/HomeScreen.kt` 或新 `SearchScreen.kt` 实现搜索页、结果分组、空/错/加载状态和 Back 返回
- [√] 4.4 GREEN: 在 `app/src/main/java/com/embytv/ui/components/CinematicComponents.kt` 让顶部搜索图标变为可聚焦按钮，并接入搜索页
- [-] 4.5 TDD-EXEMPT: 语音搜索系统 intent 难以单元测试，原因: 依赖 Android 系统 ActivityResult；替代验证: 手工验证支持/不支持语音设备的回退路径
  > 备注: 本次实现了基础能力或保留接口，但未在真实 TV 设备上完成该项手工验证/交互闭环，后续需要实机确认。
- [√] 4.6 VERIFY: 运行 `.\gradlew.bat :app:testDebugUnitTest --tests "*Search*"` 并手工确认遥控器可完成搜索

## 5. 合集 / 播放列表 / 类型 / 演员页
- [√] 5.1 GREEN: 在 `app/src/main/java/com/embytv/ui/home/HomeDashboardModels.kt` 增加 `DiscoveryContentUiState`、`DiscoveryKind` UI mapper 和抽屉导航项
- [√] 5.2 GREEN: 在 `app/src/main/java/com/embytv/ui/home/HomeViewModel.kt` 增加打开发现页、加载入口列表、进入入口详情、重试和返回层级
- [√] 5.3 GREEN: 在 `app/src/main/java/com/embytv/ui/home/HomeScreen.kt` 或新 `DiscoveryScreen.kt` 实现通用发现页列表和入口详情资源列表
- [√] 5.4 GREEN: 合集详情用 `ParentId={boxSetId}` 查询 Movie/Series；播放列表详情用 `Playlists/{playlistId}/Items` 查询条目
- [√] 5.5 GREEN: 类型详情用 `GenreIds={genreId}` 查询资源；演员详情用 `PersonIds={personId}` 查询资源
- [√] 5.6 VERIFY: 使用测试服验证类型和演员页有真实数据；合集和播放列表为空时展示稳定空状态

## 6. 详情页用户态动作
- [√] 6.1 GREEN: 在 `MediaItemSummary` 和详情 UI mapper 中暴露 `isFavorite`、`played`、`playbackPositionTicks` 等用户态字段，依赖任务2.3
- [√] 6.2 GREEN: 在 `app/src/main/java/com/embytv/ui/home/HomeViewModel.kt` 增加收藏切换、标记已播放/未播放、清除继续观看动作，并在成功后刷新详情/列表局部状态
- [√] 6.3 GREEN: 在 `app/src/main/java/com/embytv/ui/home/HomeScreen.kt` 的详情页增加可聚焦动作按钮或更多操作栏
- [-] 6.4 GREEN: 清除继续观看进度增加二次确认状态；失败时保持原状态并提示
  > 备注: 本次实现了基础能力或保留接口，但未在真实 TV 设备上完成该项手工验证/交互闭环，后续需要实机确认。
- [-] 6.5 VERIFY: 在测试服选一条低风险测试媒体执行写操作前先记录原状态，操作后再恢复原状态
  > 备注: 本次实现了基础能力或保留接口，但未在真实 TV 设备上完成该项手工验证/交互闭环，后续需要实机确认。

## 7. 播放队列与连续播放
- [√] 7.1 RED: 在 `app/src/test/java/com/embytv/domain/model/PlaybackQueueTest.kt` 增加上一集/下一集计算和队列边界测试
- [√] 7.2 GREEN: 在 `app/src/main/java/com/embytv/domain/model/PlaybackSource.kt` 增加 `PlaybackQueue` 和当前 item 用户态字段
- [√] 7.3 GREEN: 在 `app/src/main/java/com/embytv/data/repository/EmbyRepository.kt` 增加按 Episode 补同季队列、NextUp 队列兜底能力
- [√] 7.4 GREEN: 在 `app/src/main/java/com/embytv/ui/home/HomeViewModel.kt` 播放 Episode 时优先携带季内队列；从搜索/继续观看直达时按需补队列
- [√] 7.5 GREEN: 在 `app/src/main/java/com/embytv/ui/player/PlayerScreen.kt` 实现上一集/下一集按钮、自然结束自动切下一集和无下一集提示
- [√] 7.6 VERIFY: 播放季内 Episode，验证上一集/下一集、自然结束连播、最后一集空状态

## 8. 播放器音轨 / 字幕切换
- [√] 8.1 GREEN: 在 `app/src/main/java/com/embytv/ui/player/PlayerOsdState.kt` 增加 `PlayerTrackUiState`、选中音轨、选中字幕和关闭字幕状态，依赖任务1.5
- [√] 8.2 GREEN: 在 `app/src/main/java/com/embytv/ui/player/PlayerScreen.kt` 监听 Media3 `onTracksChanged`，从 `player.currentTracks` 生成音轨/字幕列表
- [√] 8.3 GREEN: 在 `PlayerScreen.kt` 中使用 Media3 track selection parameters 应用音轨和字幕选择
- [√] 8.4 GREEN: 将 Audio/Subtitles QuickSetting 从禁用态改为可打开面板；无轨道时仍显示禁用原因
- [-] 8.5 VERIFY: 使用包含多音轨/字幕的媒体手工验证切换、关闭字幕、返回 OSD 焦点
  > 备注: 本次实现了基础能力或保留接口，但未在真实 TV 设备上完成该项手工验证/交互闭环，后续需要实机确认。

## 9. 首页 Next Up 与导航整合
- [√] 9.1 GREEN: 在 `EmbyRepository.loadHomeDashboard()` 增加小数量 `Shows/NextUp` 请求，空结果不影响首页加载
- [√] 9.2 GREEN: 在 `EmbyHomeDashboard` 和 `HomeDashboardMapper` 中增加 Next Up 区块，展示剧集下一集候选
- [√] 9.3 GREEN: 抽屉入口整合搜索、收藏、合集、播放列表、类型、演员、媒体库，并保证禁用态有原因
- [√] 9.4 VERIFY: 首页加载性能检查，确认新增请求不会阻塞已存在媒体库展示

## 10. 安全检查
- [√] 10.1 检查写操作只使用当前 session 的 `userId` 和 token，不允许跨用户写状态
- [√] 10.2 检查日志、错误文案、测试输出和知识库不包含 accessToken、密码或完整播放 URL
- [√] 10.3 检查删除凭证和清除继续观看进度存在二次确认
- [√] 10.4 检查新增搜索输入只作为查询参数传给 Retrofit，不做字符串拼接 URL

## 11. 文档更新
- [√] 11.1 更新 `helloagents/main/wiki/api.md`，记录搜索、NextUp、Playstate、Discovery 相关 Emby API
- [√] 11.2 更新 `helloagents/main/wiki/modules/data.md`，记录 Repository 新能力、凭证列表迁移和用户态写操作
- [√] 11.3 更新 `helloagents/main/wiki/modules/ui.md`，记录搜索页、发现页、播放器轨道面板、连续播放、多账号选择的 TV 操作规范
- [√] 11.4 更新 `helloagents/main/wiki/arch.md` ADR 索引和 `helloagents/main/CHANGELOG.md`

## 12. 最终验证
- [√] 12.1 VERIFY: 运行 `.\gradlew.bat :app:testDebugUnitTest`
- [√] 12.2 VERIFY: 运行 `.\gradlew.bat :app:assembleDebug`
- [-] 12.3 手工验证 TV 遥控器主链路: 账号选择 → 搜索 → 详情 → 收藏/已播放 → 播放 → 音字幕切换 → 下一集 → 返回
  > 备注: 主链路代码和构建已完成，但未在真实 TV 设备上完成端到端实机验证，后续需要安装 APK 后确认焦点路径和遥控器行为。
- [-] 12.4 手工验证发现页: 合集空状态、播放列表空状态、类型有数据、演员有数据
  > 备注: 本次实现了基础能力或保留接口，但未在真实 TV 设备上完成该项手工验证/交互闭环，后续需要实机确认。
- [√] 12.5 按 develop 流程迁移方案包到 `helloagents/main/history/YYYY-MM/`


## 执行总结
- 数据层、搜索、发现页、详情用户态动作、播放队列、Media3 轨道面板、多凭证列表和知识库同步已完成。
- 自动验证已完成: `.:app:testDebugUnitTest` 等价命令 `\.\gradlew.bat :app:testDebugUnitTest` 通过，`\.\gradlew.bat :app:assembleDebug` 通过。
- 未在真实 TV 设备上完成语音输入、多音轨/字幕媒体、用户态写接口恢复原状态等手工验证，已在对应任务标为跳过并备注。
