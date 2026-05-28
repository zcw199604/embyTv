# 变更历史索引

本文件记录所有已完成变更的索引，便于追溯和查询。

---

## 索引

| 时间戳 | 功能名称 | 类型 | 状态 | 方案包路径 |
|--------|----------|------|------|------------|
| 202605281948 | performance_optimization | 优化 | ✅已完成 | [202605281948_performance_optimization](2026-05/202605281948_performance_optimization/) |
| 202605281928 | remote_ok_single_press_fix | 修复 | ✅已完成 | [202605281928_remote_ok_single_press_fix](2026-05/202605281928_remote_ok_single_press_fix/) |
| 202605281915 | coil_network_images_fix | 修复 | ✅已完成 | [202605281915_coil_network_images_fix](2026-05/202605281915_coil_network_images_fix/) |
| 202605281300 | media_detail_seasons | 功能 | ✅已完成 | [202605281300_media_detail_seasons](2026-05/202605281300_media_detail_seasons/) |
| 202605281045 | favorite_resources_by_type | 功能 | ✅已完成 | [202605281045_favorite_resources_by_type](2026-05/202605281045_favorite_resources_by_type/) |
| 202605272217 | library_browse_series_grouping | 功能 | ✅已完成 | [202605272217_library_browse_series_grouping](2026-05/202605272217_library_browse_series_grouping/) |
| 202605272133 | emby_playback_reporting | 功能 | ✅已完成 | [202605272133_emby_playback_reporting](2026-05/202605272133_emby_playback_reporting/) |
| 202605272047 | home_library_latest_sections | 功能 | ✅已完成 | [202605272047_home_library_latest_sections](2026-05/202605272047_home_library_latest_sections/) |
| 202605271602 | emby_real_data_replacement | 功能 | ✅已完成 | [202605271602_emby_real_data_replacement](2026-05/202605271602_emby_real_data_replacement/) |
| 202605271514 | emby_server_mobile_sync | 功能 | ✅已完成 | [202605271514_emby_server_mobile_sync](2026-05/202605271514_emby_server_mobile_sync/) |
| 202605271434 | remote_control_support | 修复 | ✅已完成 | [202605271434_remote_control_support](2026-05/202605271434_remote_control_support/) |
| 202605271353 | tv_ui_redesign_core | 功能 | ✅已完成 | [202605271353_tv_ui_redesign_core](2026-05/202605271353_tv_ui_redesign_core/) |
| 202605201342 | emby_tv_init | 功能 | ✅已完成 | [202605201342_emby_tv_init](2026-05/202605201342_emby_tv_init/) |

---

## 按月归档

### 2026-05

- [202605281948_performance_optimization](2026-05/202605281948_performance_optimization/) - 优化 token 冷启动、首页 Dashboard 受控并发、API service 复用和图片尺寸化。
- [202605281928_remote_ok_single_press_fix](2026-05/202605281928_remote_ok_single_press_fix/) - 修复 TV 遥控器 OK/Enter 在通用媒体卡片和按钮上需要按两次才触发的问题。
- [202605281915_coil_network_images_fix](2026-05/202605281915_coil_network_images_fix/) - 补齐 Coil 3 OkHttp 网络图片加载依赖，修复 Emby 封面全不显示。
- [202605281300_media_detail_seasons](2026-05/202605281300_media_detail_seasons/) - 新增 Movie/Series 详情页，Series 多季列表和季内 Episode 播放入口。
- [202605281045_favorite_resources_by_type](2026-05/202605281045_favorite_resources_by_type/) - 新增收藏资源页，按电影/电视剧展示收藏内容，收藏单集聚合为剧集卡片。
- [202605272217_library_browse_series_grouping](2026-05/202605272217_library_browse_series_grouping/) - 修复媒体库/电影/剧集封面兜底，新增媒体库列表页，剧集库按 Series 展示并显示剩余集数角标。
- [202605272133_emby_playback_reporting](2026-05/202605272133_emby_playback_reporting/) - 播放开始、进度、暂停/恢复和退出时同步 Emby 后台状态。
- [202605272047_home_library_latest_sections](2026-05/202605272047_home_library_latest_sections/) - 首页媒体库真实封面、继续观看剧集信息和按库最新资源分区。
- [202605271602_emby_real_data_replacement](2026-05/202605271602_emby_real_data_replacement/) - 首页和播放器可见数据替换为 Emby 真实 API 数据。
- [202605271514_emby_server_mobile_sync](2026-05/202605271514_emby_server_mobile_sync/) - 拆分 Emby 服务器配置字段，支持手机扫码同步和 token 凭证保存。
- [202605271434_remote_control_support](2026-05/202605271434_remote_control_support/) - 补齐 TV 遥控器焦点、Back、禁用反馈和播放 OSD 操作闭环。
- [202605271353_tv_ui_redesign_core](2026-05/202605271353_tv_ui_redesign_core/) - 落地 Cinematic Glass 配置页、首页媒体中心和播放 OSD。
- [202605201342_emby_tv_init](2026-05/202605201342_emby_tv_init/) - 初始化 Android TV + Emby + Media3 + AkDanmaku 工程。
