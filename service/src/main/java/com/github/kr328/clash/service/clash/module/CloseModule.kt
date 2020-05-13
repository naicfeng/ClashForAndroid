package com.github.kr328.clash.service.clash.module

import android.content.Intent
import com.github.kr328.clash.common.ids.Intents

class CloseModule : Module() {
    override val receiveBroadcasts: Set<String>
        get() = setOf(Intents.INTENT_ACTION_CLASH_REQUEST_STOP)

    private var callback: () -> Unit = {}

    fun onClosed(cb: () -> Unit) {
        callback = cb
    }

    override suspend fun onBroadcastReceived(intent: Intent) {
        when (intent.action) {
            Intents.INTENT_ACTION_CLASH_REQUEST_STOP ->
                callback()
        }
    }
}