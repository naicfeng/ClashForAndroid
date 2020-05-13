package com.github.kr328.clash.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.kr328.clash.common.utils.intent
import com.github.kr328.clash.common.utils.startForegroundServiceCompat
import com.github.kr328.clash.service.util.startClashService

class RestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null)
            return

        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
            }
            else -> return
        }

        context.startForegroundServiceCompat(ProfileBackgroundService::class.intent)

        if (ServiceStatusProvider.shouldStartClashOnBoot)
            context.startClashService()
    }
}