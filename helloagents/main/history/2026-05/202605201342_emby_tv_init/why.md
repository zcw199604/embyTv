# 变更提案: Emby TV 客户端初始化

## 需求背景
需要初始化一个支持 Emby 与弹幕的 Android TV 客户端，明确技术栈为 Jetpack Compose、TV Compose、Media3、FFmpeg 扩展、AkDanmaku、Retrofit、OkHttp、MVVM、Coroutines 和 Flow。

## 产品分析

### 目标用户与场景
- **用户群体:** 使用 Android TV 或电视盒子观看 Emby 媒体库的家庭用户。
- **使用场景:** 遥控器操作、横屏电视观看、局域网 Emby 服务器播放。
- **核心痛点:** 需要在 TV 端播放 Emby 媒体，并叠加弹幕层。

### 价值主张与成功指标
- **价值主张:** 提供可继续迭代的 Android TV 工程基础。
- **成功指标:** 工程可导入 Android Studio；依赖集中管理；具备登录、列表、播放、弹幕的基础闭环。

### 人文关怀
保留明文 HTTP 局域网能力以兼容家庭 Emby 部署，同时在文档中提示正式发布前应收敛明文流量风险。

## 变更内容
1. 创建 Android Gradle 工程与 app 模块。
2. 接入 Compose、TV Compose、Media3、Retrofit、OkHttp、AkDanmaku 依赖。
3. 建立 MVVM 分层、Emby API、Repository、播放器和弹幕桥接。
4. 增加样例播放入口，便于无 Emby 服务器时验证播放器和弹幕层。
5. 创建项目知识库与历史归档。

## 范围边界
- **范围内:** 初始化工程、基础播放闭环、基础 Emby 认证和媒体列表、样例弹幕。
- **范围外:** 账号持久化、复杂媒体库导航、真实弹幕源解析、完整播放控制器、发布签名。
- **拆分说明:** 本次只做可运行骨架和关键技术入口，业务完整度后续切片实现。

## 影响范围
- **模块:** app、ui、data、core、player、danmaku、domain、knowledge base。
- **文件:** Gradle 配置、Android Manifest、Compose UI、Repository、文档。
- **API:** 新增 Emby `Users/AuthenticateByName` 和 `Users/{userId}/Items` 客户端调用。
- **数据:** 无本地持久化数据变更。

## 核心场景

### 需求: TV 客户端工程初始化
**模块:** app
创建可导入 Android Studio 的 Android TV 工程。

#### 场景: 工程导入
具备 Gradle Wrapper、settings、version catalog 和 app 模块。
- Android Studio 能识别 Android 应用模块。

### 需求: Emby 基础接入
**模块:** data
提供登录和媒体列表读取。

#### 场景: 登录后读取媒体
用户输入 Emby 地址和账号。
- 认证成功后生成 session。
- 读取 Movie 和 Episode 列表。

### 需求: 播放与弹幕基础闭环
**模块:** player, danmaku, ui
选择媒体或样例后进入播放页。

#### 场景: 样例播放
用户点击样例播放。
- Media3 全屏播放视频。
- AkDanmaku 在播放器上方显示样例弹幕。

## 风险评估
- **风险:** 本机缺少 Android SDK、Gradle、JDK 17，无法完成本地编译验证。
- **缓解:** 创建标准 Gradle Wrapper 和工程文件，最终总结中明确环境限制。
- **风险:** Media3 FFmpeg 扩展未发布到 Maven。
- **缓解:** 采用 `app/libs` 本地 AAR 自动纳入策略，并在文档记录。
- **风险:** 明文 HTTP 兼容局域网 Emby 时存在安全风险。
- **缓解:** 文档标注正式发布前应收敛到 HTTPS 或限定域名。
