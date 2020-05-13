package com.github.kr328.clash.core

import bridge.Bridge
import bridge.TunCallback
import com.github.kr328.clash.common.Global
import com.github.kr328.clash.core.event.LogEvent
import com.github.kr328.clash.core.model.General
import com.github.kr328.clash.core.model.Proxy
import com.github.kr328.clash.core.model.ProxyGroup
import com.github.kr328.clash.core.model.Traffic
import com.github.kr328.clash.core.transact.DoneCallbackImpl
import com.github.kr328.clash.core.transact.ProxyCollectionImpl
import com.github.kr328.clash.core.transact.ProxyGroupCollectionImpl
import kotlinx.coroutines.CompletableDeferred
import java.io.File
import java.io.InputStream

object Clash {
    private val logReceivers = mutableMapOf<String, (LogEvent) -> Unit>()

    init {
        val context = Global.application

        val bytes = context.assets.open("Country.mmdb")
            .use(InputStream::readBytes)

        Bridge.initCore(bytes, context.cacheDir.absolutePath, BuildConfig.VERSION_NAME)
        Bridge.reset()
    }

    fun start() {
        Bridge.reset()
    }

    fun stop() {
        Bridge.reset()
    }

    fun startTunDevice(
        fd: Int,
        mtu: Int,
        gateway: String,
        mirror: String,
        dns: String,
        onNewSocket: (Int) -> Boolean,
        onTunStop: () -> Unit
    ) {
        Bridge.startTunDevice(fd.toLong(), mtu.toLong(), gateway, mirror, dns, object: TunCallback {
            override fun onCreateSocket(fd: Long) {
                onNewSocket(fd.toInt())
            }

            override fun onStop() {
                onTunStop()
            }
        })
    }

    fun stopTunDevice() {
        Bridge.stopTunDevice()
    }

    fun appendDns(dns: List<String>) {
        Bridge.resetDnsAppend(dns.joinToString(","))
    }

    fun setDnsOverrideEnabled(enabled: Boolean) {
        Bridge.setDnsOverrideEnabled(enabled)
    }

    fun loadProfile(path: File, baseDir: File): CompletableDeferred<Unit> {
        return DoneCallbackImpl().apply {
            Bridge.loadProfileFile(path.absolutePath, baseDir.absolutePath, this)
        }
    }

    fun downloadProfile(url: String, output: File, baseDir: File): CompletableDeferred<Unit> {
        return DoneCallbackImpl().apply {
            Bridge.downloadProfileAndCheck(url, output.absolutePath, baseDir.absolutePath, this)
        }
    }

    fun downloadProfile(fd: Int, output: File, baseDir: File): CompletableDeferred<Unit> {
        return DoneCallbackImpl().apply {
            Bridge.readProfileAndCheck(fd.toLong(), output.absolutePath, baseDir.absolutePath, this)
        }
    }

    fun queryProxyGroups(): List<ProxyGroup> {
        return ProxyGroupCollectionImpl().also { Bridge.queryAllProxyGroups(it) }
            .filterNotNull()
            .map { group ->
                ProxyGroup(group.name,
                    Proxy.Type.fromString(group.type),
                    group.delay,
                    group.current,
                    ProxyCollectionImpl().also { pc ->
                        group.queryAllProxies(pc)
                    }.filterNotNull().map {
                        Proxy(it.name, Proxy.Type.fromString(it.type), it.delay)
                    })
            }
    }

    fun setSelectedProxy(name: String, selected: String): Boolean {
        return Bridge.setSelectedProxy(name, selected)
    }

    fun startHealthCheck(name: String): CompletableDeferred<Unit> {
        return DoneCallbackImpl().apply {
            Bridge.startUrlTest(name, this)
        }
    }

    fun setProxyMode(mode: String) {
        Bridge.setProxyMode(mode)
    }

    fun queryGeneral(): General {
        val t = Bridge.queryGeneral()

        return General(
            General.Mode.fromString(t.mode),
            t.httpPort.toInt(), t.socksPort.toInt(), t.redirectPort.toInt()
        )
    }

    fun queryTraffic(): Traffic {
        val data = Bridge.queryTraffic()

        return Traffic(data.upload, data.download)
    }

    fun queryBandwidth(): Traffic {
        val data = Bridge.queryBandwidth()

        return Traffic(data.upload, data.download)
    }

    fun registerLogReceiver(key: String, receiver: (LogEvent) -> Unit) {
        synchronized(logReceivers) {
            logReceivers[key] = receiver

            Bridge.setLogCallback(this::onLogEvent)
        }
    }

    fun unregisterLogReceiver(key: String) {
        synchronized(logReceivers) {
            logReceivers.remove(key)

            if (logReceivers.isEmpty())
                Bridge.setLogCallback(null)
        }
    }

    private fun onLogEvent(level: String, payload: String) {
        synchronized(logReceivers) {
            logReceivers.forEach {
                it.value(LogEvent(LogEvent.Level.fromString(level), payload))
            }
        }
    }
}