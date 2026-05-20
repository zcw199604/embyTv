package com.embytv.core.di

import android.content.Context
import com.embytv.core.danmaku.AkDanmakuBridge
import com.embytv.core.network.NetworkModule
import com.embytv.core.player.Media3PlayerFactory
import com.embytv.data.remote.EmbyApiFactory
import com.embytv.data.repository.EmbyRepository
import com.embytv.data.repository.EmbyStreamUrlBuilder
import com.embytv.domain.model.DanmakuCue
import com.embytv.domain.model.DanmakuMode
import com.embytv.domain.model.PlaybackSource

interface AppContainer {
    val embyRepository: EmbyRepository
    val playerFactory: Media3PlayerFactory
    val danmakuBridge: AkDanmakuBridge
    fun samplePlaybackSource(): PlaybackSource
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val appContext = context.applicationContext
    private val okHttpClient = NetworkModule.createOkHttpClient()
    private val streamUrlBuilder = EmbyStreamUrlBuilder()

    override val embyRepository: EmbyRepository =
        EmbyRepository(
            apiFactory = EmbyApiFactory(okHttpClient),
            streamUrlBuilder = streamUrlBuilder,
        )

    override val playerFactory: Media3PlayerFactory =
        Media3PlayerFactory(appContext, okHttpClient)

    override val danmakuBridge: AkDanmakuBridge = AkDanmakuBridge()

    override fun samplePlaybackSource(): PlaybackSource =
        PlaybackSource(
            itemId = "sample-bbb",
            title = "Big Buck Bunny",
            streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            danmaku = listOf(
                DanmakuCue(1, 1_000, "Emby TV 初始化成功", 0xFFFFFF, DanmakuMode.Rolling),
                DanmakuCue(2, 3_500, "Media3 播放层已接入", 0x37C8A3, DanmakuMode.Top),
                DanmakuCue(3, 6_000, "AkDanmaku 弹幕层已覆盖", 0xFFD166, DanmakuMode.Bottom),
            ),
        )
}
