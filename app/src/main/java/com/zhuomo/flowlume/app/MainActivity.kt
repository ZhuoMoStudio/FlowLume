package com.zhuomo.flowlume.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.zhuomo.flowlume.app.di.AppContainer
import com.zhuomo.flowlume.app.ui.AppNavHost
import com.zhuomo.flowlume.app.util.LocaleHelper
import com.zhuomo.flowlume.ui.FlowLumeTheme

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.apply(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val charcoal by AppContainer.charcoalTheme.collectAsState()
            FlowLumeTheme(charcoal = charcoal) {
                AppNavHost()
            }
        }
    }
}
