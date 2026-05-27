package com.embytv.core.di

import android.content.Context
import com.embytv.core.danmaku.AkDanmakuBridge
import com.embytv.core.network.NetworkModule
import com.embytv.core.player.Media3PlayerFactory
import com.embytv.data.local.EncryptedEmbyCredentialStore
import com.embytv.data.remote.EmbyApiFactory
import com.embytv.data.repository.EmbyRepository
import com.embytv.data.repository.EmbyStreamUrlBuilder

interface AppContainer {
    val embyRepository: EmbyRepository
    val playerFactory: Media3PlayerFactory
    val danmakuBridge: AkDanmakuBridge
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val appContext = context.applicationContext
    private val okHttpClient = NetworkModule.createOkHttpClient()
    private val streamUrlBuilder = EmbyStreamUrlBuilder()

    override val embyRepository: EmbyRepository =
        EmbyRepository(
            apiFactory = EmbyApiFactory(okHttpClient),
            streamUrlBuilder = streamUrlBuilder,
            credentialStore = EncryptedEmbyCredentialStore(appContext),
        )

    override val playerFactory: Media3PlayerFactory =
        Media3PlayerFactory(appContext, okHttpClient)

    override val danmakuBridge: AkDanmakuBridge = AkDanmakuBridge()
}
