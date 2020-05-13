package com.github.kr328.clash.service.clash.module

import android.content.Intent

abstract class Module {
    open suspend fun onCreate() {}
    open suspend fun onStart() {}
    open suspend fun onStop() {}
    open suspend fun onTick() {}
    open suspend fun onBroadcastReceived(intent: Intent) {}

    open val receiveBroadcasts: Set<String> = emptySet()
    var enableTicker: Boolean = false
}