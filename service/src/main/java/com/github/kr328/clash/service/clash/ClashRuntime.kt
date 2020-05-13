package com.github.kr328.clash.service.clash

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.github.kr328.clash.common.Permissions
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.service.clash.module.Module
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ClashRuntime(private val context: Context) {
    companion object {
        private val GIL = Mutex() // :)
    }

    private val modules: MutableList<Module> = mutableListOf()
    private val mutex = Mutex()

    suspend fun <T : Module> install(module: T, configure: T.() -> Unit = {}) = mutex.withLock {
        modules.add(module)

        module.onCreate()
        module.configure()
    }

    suspend fun exec() {
        GIL.withLock {
            execLocked()
        }
    }

    private suspend fun execLocked() {
        val broadcastChannel = Channel<Intent>(Channel.UNLIMITED)
        val tickerChannel = Channel<Unit>()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                broadcastChannel.offer(intent ?: return)
            }
        }
        var tickerEnabled: Boolean

        coroutineScope {
            launch {
                while (isActive) {
                    tickerChannel.offer(Unit)
                    delay(1000)
                }
            }

            try {
                Clash.start()

                context.registerReceiver(receiver, IntentFilter().apply {
                    modules.flatMap { it.receiveBroadcasts }.distinct().forEach {
                        addAction(it)
                    }
                }, Permissions.PERMISSION_RECEIVE_BROADCASTS, null)

                modules.forEach {
                    it.onStart()
                }

                while (isActive) {
                    tickerEnabled = modules.any { it.enableTicker }

                    select<Unit> {
                        broadcastChannel.onReceive { intent ->
                            modules.forEach {
                                it.onBroadcastReceived(intent)
                            }
                        }
                        if (tickerEnabled) {
                            tickerChannel.onReceive {
                                modules.forEach {
                                    it.onTick()
                                }
                            }
                        }
                    }
                }
            } finally {
                runCatching {
                    modules.reversed().forEach {
                        it.onStop()
                    }
                }

                runCatching {
                    context.unregisterReceiver(receiver)
                }

                Clash.stop()
            }
        }
    }
}
