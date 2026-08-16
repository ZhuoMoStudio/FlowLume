package com.zhuomo.flowlume.app

import android.app.Application
import com.zhuomo.flowlume.app.di.AppContainer

class FlowLumeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.init(this)
    }
}
