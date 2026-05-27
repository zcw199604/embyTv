# 任务清单: Emby TV 客户端初始化

目录: `helloagents/main/history/2026-05/202605201342_emby_tv_init/`

---

## 0. 方案边界确认
- [√] 0.1 确认本次任务仅覆盖初始化骨架和基础闭环。
- [√] 0.2 确认模块职责、接口契约、数据边界和依赖边界。
- [√] 0.3 确认不实现账号持久化、真实弹幕源和完整媒体库导航。

---

## 1. Gradle 与 Android TV 工程
- [√] 1.1 创建 settings、root build、version catalog、Gradle properties。
- [√] 1.2 创建 app 模块、Manifest、TV launcher、资源和 ProGuard 规则。
- [√] 1.3 配置 `app/libs` 作为 FFmpeg 扩展 AAR 预留目录。

## 2. 核心架构与数据层
- [√] 2.1 创建 domain 模型。
- [√] 2.2 创建 Retrofit EmbyApi、DTO、EmbyApiFactory。
- [√] 2.3 创建 EmbyRepository 和 EmbyStreamUrlBuilder。
- [√] 2.4 创建 OkHttp 网络模块。

## 3. 播放与弹幕
- [√] 3.1 创建 Media3PlayerFactory。
- [√] 3.2 创建 AkDanmakuBridge。
- [√] 3.3 在 PlayerScreen 组合 PlayerView 与 DanmakuView。

## 4. UI 与 ViewModel
- [√] 4.1 创建 Application、Activity、AppContainer。
- [√] 4.2 创建 HomeViewModel、HomeScreen。
- [√] 4.3 创建 Compose 根组件和主题。

## 5. 安全检查
- [√] 5.1 检查未硬编码账号、密码、令牌。
- [√] 5.2 记录 cleartext HTTP 的局域网兼容风险。

## 6. 文档更新
- [√] 6.1 创建知识库核心文档。
- [√] 6.2 迁移方案包至 history。
- [√] 6.3 更新 CHANGELOG。

## 7. 测试
- [√] 7.1 添加 URL 构造单元测试。
- [√] 7.2 运行 Gradle 编译与测试。
> 备注: 已验证 `C:\Users\MyPC\.jdks\corretto-17.0.16` 可用；设置该路径为 `JAVA_HOME` 后，`.\gradlew.bat --version` 显示 Launcher JVM 17.0.16。已配置 `local.properties` 指向 `C:\Users\MyPC\AppData\Local\Android\Sdk`，并执行 `.\gradlew.bat :app:testDebugUnitTest` 成功。
- [√] 7.3 修复 AGP 9 构建配置。
> 备注: 根据 Gradle 失败信息移除 `org.jetbrains.kotlin.android` 插件；AGP 9 已内置 Android Kotlin 支持，Compose 编译插件保留。
- [√] 7.4 验证 Debug APK 构建。
> 备注: 执行 `.\gradlew.bat :app:assembleDebug` 成功。构建期间提示 `libandroidx.graphics.path.so` 和 `libgdx.so` 未 strip，会按原样打包；当前不影响 debug 构建。
