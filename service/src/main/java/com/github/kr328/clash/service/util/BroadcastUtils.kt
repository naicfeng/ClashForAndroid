package com.github.kr328.clash.service.util

import android.content.Context
import android.content.Intent
import com.github.kr328.clash.common.Permissions
import com.github.kr328.clash.common.ids.Intents

fun Context.sendBroadcastSelf(intent: Intent) {
    this.sendBroadcast(
        intent.setPackage(this.packageName),
        Permissions.PERMISSION_RECEIVE_BROADCASTS
    )
}

fun Context.broadcastProfileChanged() {
    val intent = Intent(Intents.INTENT_ACTION_PROFILE_CHANGED)

    this.sendBroadcastSelf(intent)
}

fun Context.broadcastProfileLoaded() {
    val intent = Intent(Intents.INTENT_ACTION_PROFILE_LOADED)

    this.sendBroadcastSelf(intent)
}

fun Context.broadcastNetworkChanged() {
    this.sendBroadcastSelf(Intent(Intents.INTENT_ACTION_NETWORK_CHANGED))
}

fun Context.broadcastClashStarted() {
    this.sendBroadcastSelf(Intent(Intents.INTENT_ACTION_CLASH_STARTED))
}

fun Context.broadcastClashStopped(reason: String?) {
    this.sendBroadcastSelf(
        Intent(Intents.INTENT_ACTION_CLASH_STOPPED).putExtra(
            Intents.INTENT_EXTRA_CLASH_STOP_REASON,
            reason
        )
    )
}
