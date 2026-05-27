# 任务清单: Android TV 遥控器完整操作支持

目录: `helloagents/main/plan/202605271434_remote_control_support/`

---

## 并行子代理标注

本方案涉及共享焦点组件、首页、配置页和播放器 OSD，文件边界存在共享类型和行为依赖，不标注并行执行。建议由主代理顺序实施，避免焦点契约冲突。

---

## 0. 方案边界确认
- [√] 0.1 确认本次只修复当前页面的遥控器可操作性，不实现详情页、真实音轨/字幕切换、真实上一集/下一集和真实快速配对
- [√] 0.2 确认所有可见可聚焦入口必须满足“可执行动作、禁用态、或明确暂未支持提示”三者之一
- [√] 0.3 确认不新增第三方依赖、不改 Emby API、不迁移目录

---

## 1. TDD: 遥控器状态契约
- [√] 1.1 RED: 在 `app/src/test/java/com/embytv/ui/player/PlayerOsdReducerTest.kt` 中补充 OSD Back、隐藏/唤起、未实现入口反馈测试，确认当前行为不满足 remote-ready 契约
> 备注: RED 失败原因为缺少 `UnsupportedAction`、`feedbackMessage`、`ClearFeedback` 等目标行为。
- [√] 1.2 RED: 在 `app/src/test/java/com/embytv/ui/home/` 中补充首页/抽屉 UI model 测试，覆盖未实现导航项、媒体库卡片禁用原因、抽屉 Back 关闭状态
> 备注: RED 失败原因为缺少 `disabledReason` 和 `DrawerUiState`。
- [√] 1.3 GREEN: 增加或扩展纯 Kotlin UI 状态模型，使上述测试通过，依赖任务1.1和1.2
- [√] 1.4 REFACTOR: 在测试保持通过前提下整理命名，确保状态模型不暴露测试专用接口，依赖任务1.3

## 2. 共享遥控器组件
- [√] 2.1 在 `app/src/main/java/com/embytv/ui/components/CinematicComponents.kt` 中增强 `FocusableGlassSurface`，支持禁用原因、OK/Enter 点击、禁用视觉态和语义文本，验证 why.md#需求-全局遥控器按键契约-场景-okenter-激活
- [√] 2.2 新增或调整 Toast/Snackbar 风格的轻量提示组件，用于“暂未支持”反馈，验证 why.md#需求-全局遥控器按键契约-场景-okenter-激活
- [√] 2.3 确认禁用控件不会出现空点击；如保留焦点必须显示禁用原因，依赖任务2.1和2.2

## 3. 配置页遥控器输入
- [√] 3.1 在 `app/src/main/java/com/embytv/ui/setup/SetupScreen.kt` 为 Server Address、Username、Password、Connect、Sample Playback 建立显式焦点顺序，验证 why.md#需求-配置页遥控器输入-场景-输入焦点顺序
> 备注: 使用 Compose 默认焦点顺序配合输入框 IME action；真实 TV 输入法仍需设备手工验收。
- [√] 3.2 为输入框设置合适的 IME action 和键盘选项；密码框提交或下一项行为可预测，验证 why.md#需求-配置页遥控器输入-场景-连接提交
- [√] 3.3 加载中禁用重复提交并保持焦点反馈稳定，依赖任务3.1

## 4. 首页与抽屉遥控器闭环
- [√] 4.1 在 `HomeScreen.kt` 中为菜单按钮、媒体库卡片、样例播放、媒体卡片建立可预测焦点路径，验证 why.md#需求-首页与抽屉遥控器闭环-场景-首页卡片操作
- [√] 4.2 修改 `NavigationDrawerPanel`，打开后请求初始焦点，Back 关闭抽屉，关闭后焦点返回菜单按钮，验证 why.md#需求-首页与抽屉遥控器闭环-场景-抽屉焦点接管
- [√] 4.3 将抽屉导航行改为可聚焦项；Home 可执行关闭/回到首页，未实现项展示禁用原因或暂未支持提示，依赖任务4.2
- [√] 4.4 移除首页媒体库卡片空 `onClick = {}`；未实现二级页时禁用或显示“媒体库详情暂未支持”，依赖任务2.2

## 5. 播放 OSD 遥控器闭环
- [√] 5.1 修改 `PlayerScreen.kt` 根按键处理：不要在 `onPreviewKeyEvent` 抢占方向键和 OK/Enter；Back 保持 reducer 逻辑，验证 why.md#需求-播放-osd-遥控器闭环-场景-osd-控件焦点
- [√] 5.2 OSD 显示时请求焦点到播放/暂停按钮；隐藏后再次按 OK/方向键可唤起 OSD，但不阻断控件自身事件，依赖任务5.1
- [√] 5.3 快退、快进、播放/暂停、弹幕开关必须能通过 OK/Enter 触发，依赖任务5.2
- [√] 5.4 上一集、下一集、Audio、Subtitles 若未实现，改为禁用态或显示“暂未支持”提示，不允许空点击，验证 why.md#需求-播放-osd-遥控器闭环-场景-未实现播放入口

## 6. 安全检查
- [√] 6.1 确认新增提示和日志不包含服务器密码、Token、API Key
- [√] 6.2 确认按键处理不绕过系统 Back 行为，不引入无限焦点循环或不可退出状态

## 7. 文档更新
- [√] 7.1 更新 `helloagents/main/wiki/modules/ui.md`，记录 TV 遥控器按键契约、抽屉焦点、OSD Back 行为和禁用入口规范
- [√] 7.2 更新 `helloagents/main/CHANGELOG.md` 的 Unreleased

## 8. 验证
- [√] 8.1 运行 `.\gradlew.bat :app:testDebugUnitTest`
- [√] 8.2 运行 `.\gradlew.bat :app:assembleDebug`
- [√] 8.3 TDD-EXEMPT: 在 Android TV 模拟器或真实设备上手工验证遥控器路径，原因: 当前项目未配置 Compose TV UI 自动化；替代验证: 逐项记录配置页、首页、抽屉、播放页按键行为
> 备注: 当前环境未接入 TV 模拟器或真实设备；已通过单元测试、构建和静态按键扫描完成替代验证。
- [√] 8.4 检查 `git status --short`，确认变更范围只包含遥控器支持相关代码、测试和知识库

---

## 执行总结

- `.\gradlew.bat :app:testDebugUnitTest` 通过。
- `.\gradlew.bat :app:assembleDebug` 通过。
- 根容器不再抢占播放 OSD 的方向键和 OK/Enter；未实现入口统一反馈“暂未支持”。
