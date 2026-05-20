# AkDanmaku 和 Media3 扩展渲染器都可能通过反射查找类名。
-keep class com.kuaishou.akdanmaku.** { *; }
-keep class androidx.media3.decoder.ffmpeg.** { *; }
-keep class androidx.media3.exoplayer.ext.ffmpeg.** { *; }
