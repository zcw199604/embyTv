# 任务清单: 修复 Emby 封面图片不显示

目录: `helloagents/main/plan/202605281915_coil_network_images_fix/`

---

## 0. 方案边界确认
- [√] 0.1 确认本次只修复 Coil 3 缺少网络图片加载模块导致的封面不显示，不改图片 URL 构造逻辑。
- [√] 0.2 确认不修改版本号、不提交推送，除非用户后续明确要求。
- [√] 0.3 确认最小改动策略: 只修改构建依赖、知识库和必要验证，不做 UI 重构。

## 1. RED: 根因验证
- [√] 1.1 运行 `.\gradlew.bat :app:dependencies --configuration debugRuntimeClasspath`，确认当前依赖树不包含 `io.coil-kt.coil3:coil-network-okhttp`。
- [√] 1.2 使用已验证的 Emby 图片端点确认图片 URL 本身可返回 `200 image/*`，排除 URL 构造和 Android 明文 HTTP 配置为主因。

## 2. GREEN: 补齐 Coil 网络加载依赖
- [√] 2.1 在 `gradle/libs.versions.toml` 新增 `coil-network-okhttp = { module = "io.coil-kt.coil3:coil-network-okhttp", version.ref = "coil" }`。
- [√] 2.2 在 `app/build.gradle.kts` 新增 `implementation(libs.coil.network.okhttp)`。
- [√] 2.3 保持 `NetworkBackdropImage` 和 `AsyncImage` 调用不变，验证 why.md#需求-emby-网络封面显示。

## 3. 安全与兼容检查
- [√] 3.1 检查新增依赖不引入 token、密码或完整播放 URL 日志。
- [√] 3.2 检查不修改 `network_security_config.xml` 和 Android 权限，避免扩大网络安全面。
- [√] 3.3 检查 Coil 相关依赖版本均为 `3.4.0`，避免版本漂移。

## 4. 文档更新
- [√] 4.1 更新 `helloagents/main/wiki/modules/ui.md`，记录 Coil 3 网络图片需要 `coil-network-okhttp`。
- [√] 4.2 更新 `helloagents/main/wiki/arch.md`，追加 ADR-009 索引。
- [√] 4.3 更新 `helloagents/main/CHANGELOG.md`，记录封面不显示修复。

## 5. 验证
- [√] 5.1 VERIFY: 运行 `.\gradlew.bat :app:dependencies --configuration debugRuntimeClasspath`，确认包含 `coil-network-okhttp:3.4.0`。
- [√] 5.2 VERIFY: 运行 `.\gradlew.bat :app:testDebugUnitTest`。
- [√] 5.3 VERIFY: 运行 `.\gradlew.bat :app:assembleDebug`。
- [-] 5.4 TDD-EXEMPT: 真机图片显示验收，原因: 需要 Android TV/模拟器实际运行 Coil 图片加载；替代验证: 安装 APK 后登录 Emby，确认首页媒体库、媒体卡片和详情页海报显示真实图片。
> 备注: 当前执行环境无法直接操作真机 TV，已用依赖树、Emby 图片端点 200 响应、单测和 Debug 构建替代验证。
