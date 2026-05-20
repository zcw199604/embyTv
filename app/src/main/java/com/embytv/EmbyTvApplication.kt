package com.embytv

import android.app.Application
import com.embytv.core.di.AppContainer
import com.embytv.core.di.DefaultAppContainer

class EmbyTvApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
