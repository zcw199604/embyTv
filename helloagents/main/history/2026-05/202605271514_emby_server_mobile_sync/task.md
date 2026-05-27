# 任务清单: Emby 服务器配置与手机同步

目录: `helloagents/main/plan/202605271514_emby_server_mobile_sync/`

---

## 并行子代理标注

- 并行组 A: 任务 [1.1, 1.2, 1.3]；允许写入: `app/src/main/java/com/embytv/domain/model/`, `app/src/test/java/com/embytv/domain/model/`；冲突域: `ServerConfig` 数据契约；验证: `.\gradlew.bat :app:testDebugUnitTest`
- 并行组 B: 任务 [3.1, 3.2]；允许写入: `app/src/main/java/com/embytv/core/network/`, `app/src/test/java/com/embytv/core/network/`；冲突域: 本机 HTTP 同步接口；验证: `.\gradlew.bat :app:testDebugUnitTest`
- 不可并行任务: [2.1, 2.2, 2.3, 4.1, 5.1, 7.1]；原因: UI 与 ViewModel 状态依赖领域模型，集成验证需在所有实现后执行。

---

## 0. 方案边界确认
- [√] 0.1 确认本次任务仅覆盖 why.md 的范围内切片，范围外能力不进入实现。
- [√] 0.2 确认 how.md 的字段模型、同步接口、官方认证凭证策略、安全边界和依赖边界完整。
- [√] 0.3 确认最小改动策略: 不重构播放器、弹幕、首页媒体流或 Emby Repository 主流程。

---

## 1. RED: 结构化配置模型测试
- [√] 1.1 RED: 新增 `ServerConfigDraftTest`，覆盖 `https` 默认端口 `443`、`http` 默认端口 `8096`，先确认测试因生产模型缺失失败，验证 why.md#需求-tv-端结构化服务器配置-场景-默认协议与端口。
- [√] 1.2 RED: 新增 baseUrl 规范化测试，覆盖空路径、`emby`、`/emby/`、非法端口、空 host、空 username，先确认测试因生产逻辑缺失失败，验证 why.md#需求-tv-端结构化服务器配置-场景-拼装-emby-baseurl。
- [√] 1.3 RED: 新增协议切换端口策略测试，覆盖默认端口自动切换和自定义端口不被覆盖，先确认测试因生产逻辑缺失失败。
- [√] 1.4 RED: 新增 `SavedEmbyCredential` 相关测试，确认登录成功后的可保存字段包含 `username` 展示字段但不包含密码，验证 why.md#需求-emby-官方认证凭证策略-场景-保存登录态。

## 2. GREEN: 结构化配置模型实现
- [√] 2.1 在 `app/src/main/java/com/embytv/domain/model/` 新增 `ServerProtocol`、`ServerConfigDraft`，实现默认端口、端口切换策略和 `toBaseUrl()`，依赖任务 1.1-1.3。
- [√] 2.2 调整 `ServerConfig.kt`，保留 `baseUrl/username/password/deviceId` 对 Repository 的契约，配置构造改由草稿模型产生。
- [√] 2.3 新增 `SavedEmbyCredential` 与 `EmbyCredentialStore` 契约，字段包含 `serverUrl/userId/username/accessToken/serverId/deviceId/savedAtEpochMillis`，不包含密码。
- [√] 2.4 调整 `EmbyRepository` 或其调用层，登录成功后保存访问凭证和用户名展示字段，后续请求只使用 token，不保存密码。
- [√] 2.5 运行 `.\gradlew.bat :app:testDebugUnitTest`，确认结构化配置和凭证策略相关测试通过。

## 3. RED/GREEN: 手机同步服务
- [√] 3.1 RED: 新增同步 payload 解析和校验测试，覆盖 token 不匹配、缺失 host、非法端口、合法表单同步，先确认测试因同步服务缺失失败，验证 why.md#需求-手机扫码同步配置-场景-手机填写并同步。
- [√] 3.2 GREEN: 在 `app/src/main/java/com/embytv/core/network/` 新增 `MobileSetupSyncServer`、同步 payload 和 HTML 页面生成逻辑，提供 `qrUrl` 与同步事件 Flow。
- [√] 3.3 在 `gradle/libs.versions.toml` 和 `app/build.gradle.kts` 加入候选依赖 `com.google.zxing:core:3.5.4`、`org.nanohttpd:nanohttpd:2.3.1`、`androidx.security:security-crypto:1.1.0`，执行阶段记录依赖审计结果。
- [√] 3.4 新增二维码生成工具，将 `qrUrl` 转为 Compose 可显示的位图或 ImageBitmap。

## 4. TV 设置页和 ViewModel 集成
- [√] 4.1 调整 `HomeUiState.kt`，以 `ServerConfigDraft` 替代单一 `serverUrl`，并加入 `MobileSetupSyncUiState`。
- [√] 4.2 调整 `HomeViewModel.kt`，新增协议、地址、端口、路径、用户名、密码更新方法，连接时由草稿生成 `ServerConfig`。
- [√] 4.3 在 `HomeViewModel.kt` 启动并收集 `MobileSetupSyncServer`，收到手机同步 payload 后更新 TV 表单，不自动调用 `connect()`。
- [√] 4.4 调整 `SetupScreen.kt`，手动配置区改为服务器地址、协议选择、端口、路径、用户名、密码六项，协议选择支持遥控器方向键和 OK。
- [√] 4.5 调整 `QuickSetupPanel`，展示真实二维码、同步 URL 或错误提示，并保留 TV 手动输入兜底。

## 5. 安全检查
- [√] 5.1 检查同步服务只在设置页生命周期运行，离开设置页或连接成功后停止。
- [√] 5.2 检查 token 生成、校验、过期和单次使用策略；请求体、密码和 token 不进入日志。
- [√] 5.3 检查手机同步接口只接受允许字段，非法字段和非法端口返回错误，不更新 TV 表单。
- [√] 5.4 检查登录成功后的持久化内容包含用户名展示字段但不包含密码，访问令牌使用加密存储。
- [√] 5.5 检查新增依赖版本、许可证和已知漏洞；如 NanoHTTPD 或 security-crypto 风险不可接受，停止实现并回到方案设计替换方案。

## 6. 文档更新
- [√] 6.1 更新 `helloagents/main/wiki/modules/ui.md`，记录结构化服务器配置字段和扫码同步交互。
- [√] 6.2 更新 `helloagents/main/wiki/arch.md`，补充 TV 本机同步服务流程和 ADR-002 索引。
- [√] 6.3 更新 `helloagents/main/project.md`，记录新增依赖、本地同步安全约束和“保存用户名展示字段、不保存密码、使用 Emby 访问凭证恢复认证”的认证约定。
- [√] 6.4 更新 `helloagents/main/CHANGELOG.md`。

## 7. 验证
- [√] 7.1 REFACTOR: 在所有相关测试通过后整理命名和职责边界，不扩大功能范围。
- [√] 7.2 VERIFY: 使用 JDK 17 运行 `.\gradlew.bat :app:testDebugUnitTest`。
- [√] 7.3 VERIFY: 使用 JDK 17 运行 `.\gradlew.bat :app:assembleDebug`。
- [-] 7.4 手工验收: Android TV 设备或模拟器打开设置页，确认遥控器可操作六项字段、协议切换默认端口正确、二维码可被手机扫码访问。TDD-EXEMPT: 当前未连接真实 TV 设备/模拟器，已用单测和 Debug 构建替代。
- [-] 7.5 手工验收: 手机浏览器填写六项字段并点击“同步到电视”，确认 TV 表单实时更新且不会自动登录。TDD-EXEMPT: 当前未连接真实 TV 与同网手机，已用 payload 单测和 Debug 构建替代。
