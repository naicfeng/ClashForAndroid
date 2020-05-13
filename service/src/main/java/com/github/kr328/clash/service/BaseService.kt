package com.github.kr328.clash.service

import android.app.Service
import android.content.Context
import com.github.kr328.clash.common.utils.createLanguageConfigurationContext
import com.github.kr328.clash.service.settings.ServiceSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

abstract class BaseService : Service(), CoroutineScope by MainScope() {
    lateinit var settings: ServiceSettings

    override fun attachBaseContext(base: Context?) {
        settings = ServiceSettings(base ?: return super.attachBaseContext(base))

        val language = settings.get(ServiceSettings.LANGUAGE)

        super.attachBaseContext(base.createLanguageConfigurationContext(language))
    }

    override fun onDestroy() {
        super.onDestroy()

        cancel()
    }
}