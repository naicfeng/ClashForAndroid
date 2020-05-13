package com.github.kr328.clash.remote

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.github.kr328.clash.common.Global
import com.github.kr328.clash.common.Permissions
import com.github.kr328.clash.common.ids.Intents
import com.github.kr328.clash.common.utils.Log
import com.github.kr328.clash.utils.ApplicationObserver

object Broadcasts {
    interface Receiver {
        fun onStarted()
        fun onStopped(cause: String?)
        fun onProfileChanged()
        fun onProfileLoaded()
    }

    var clashRunning: Boolean = false

    private val receivers = mutableListOf<Receiver>()
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.`package` != context?.packageName)
                return

            when (intent?.action) {
                Intents.INTENT_ACTION_CLASH_STARTED -> {
                    clashRunning = true

                    receivers.forEach {
                        it.onStarted()
                    }
                }
                Intents.INTENT_ACTION_CLASH_STOPPED -> {
                    clashRunning = false

                    receivers.forEach {
                        it.onStopped(intent.getStringExtra(Intents.INTENT_EXTRA_CLASH_STOP_REASON))
                    }
                }
                Intents.INTENT_ACTION_PROFILE_CHANGED ->
                    receivers.forEach {
                        it.onProfileChanged()
                    }
                Intents.INTENT_ACTION_PROFILE_LOADED -> {
                    receivers.forEach {
                        it.onProfileLoaded()
                    }
                }
            }
        }
    }

    fun register(receiver: Receiver) {
        receivers.add(receiver)
    }

    fun unregister(receiver: Receiver) {
        receivers.remove(receiver)
    }

    private val observer = ApplicationObserver {
        Log.d("Global Broadcast Receiver State = $it")

        if ( it ) {
            Global.application.registerReceiver(broadcastReceiver, IntentFilter().apply {
                addAction(Intents.INTENT_ACTION_PROFILE_CHANGED)
                addAction(Intents.INTENT_ACTION_CLASH_STOPPED)
                addAction(Intents.INTENT_ACTION_CLASH_STARTED)
                addAction(Intents.INTENT_ACTION_PROFILE_LOADED)
            }, Permissions.PERMISSION_RECEIVE_BROADCASTS, null)

            val current = RemoteUtils.detectClashRunning(Global.application)
            if (current != clashRunning) {
                clashRunning = current

                if (current) {
                    receivers.forEach { receiver ->
                        receiver.onStarted()
                    }
                } else {
                    receivers.forEach { receiver ->
                        receiver.onStopped(null)
                    }
                }
            }
        } else {
            Global.application.unregisterReceiver(broadcastReceiver)
        }
    }

    fun init(application: Application) {
        observer.register(application)
    }
}