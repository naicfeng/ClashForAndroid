package com.github.kr328.clash.service.util

import android.content.Context
import android.content.Intent
import android.net.VpnService
import com.github.kr328.clash.common.ids.Intents
import com.github.kr328.clash.common.utils.intent
import com.github.kr328.clash.common.utils.startForegroundServiceCompat
import com.github.kr328.clash.service.ClashService
import com.github.kr328.clash.service.TunService
import com.github.kr328.clash.service.settings.ServiceSettings

fun Context.startClashService(): Intent? {
    val startTun = ServiceSettings(this).get(ServiceSettings.ENABLE_VPN)

    if (startTun) {
        val vpnRequest = VpnService.prepare(this)
        if (vpnRequest != null)
            return vpnRequest

        startForegroundServiceCompat(TunService::class.intent)
    } else {
        startForegroundServiceCompat(ClashService::class.intent)
    }

    return null
}

fun Context.stopClashService() {
    sendBroadcastSelf(Intent(Intents.INTENT_ACTION_CLASH_REQUEST_STOP))
}