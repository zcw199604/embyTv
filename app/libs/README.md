# 本地媒体扩展目录

Media3 FFmpeg 扩展没有随 AndroidX Media3 发布到 Google Maven。需要 FFmpeg 解码能力时，从 Media3 源码编译扩展 AAR，并将产物放到本目录。

`app/build.gradle.kts` 会自动包含本目录下的 `*.aar` 和 `*.jar`。
