package com.github.kr328.clash.core

import android.os.ParcelFileDescriptor
import com.github.kr328.clash.common.Global
import com.github.kr328.clash.common.utils.Log
import com.github.kr328.clash.core.bridge.Bridge
import com.github.kr328.clash.core.bridge.TunCallback
import com.github.kr328.clash.core.event.LogEvent
import com.github.kr328.clash.core.model.General
import com.github.kr328.clash.core.model.ProxyGroup
import com.github.kr328.clash.core.model.Traffic
import java.io.File
import java.io.InputStream
import java.util.concurrent.CompletableFuture

object Clash {
    private val logReceivers = mutableMapOf<String, (LogEvent) -> Unit>()

    init {
        val context = Global.application

        val bytes = context.assets.open("Country.mmdb")
            .use(InputStream::readBytes)

        Bridge.initialize(bytes, context.cacheDir.absolutePath, BuildConfig.VERSION_NAME)
        Bridge.reset()

        Bridge.setLogCallback {
            synchronized(logReceivers) {
                logReceivers.forEach { (_, e) -> e(it) }
            }
        }

        Log.i("Clash core initialized")
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
        Bridge.startTunDevice(fd, mtu, gateway, mirror, dns, object: TunCallback {
            override fun onNewSocket(socket: Int) {
                onNewSocket(socket)
            }
            override fun onStop() {
                onTunStop()
            }
        })
    }

    fun stopTunDevice() {
        Bridge.stopTunDevice()
    }

    fun setDnsOverride(dnsOverride: Boolean, appendNameservers: List<String>) {
        Bridge.setDnsOverride(dnsOverride, appendNameservers.joinToString(","))
    }

    fun loadProfile(path: File, baseDir: File): CompletableFuture<Unit> {
        return Bridge.loadProfile(path.absolutePath, baseDir.absolutePath).thenApply { Unit }
    }

    fun downloadProfile(url: String, output: File, baseDir: File): CompletableFuture<Unit> {
        return Bridge.downloadProfile(url, baseDir.absolutePath, output.absolutePath).thenApply { Unit }
    }

    fun downloadProfile(fd: ParcelFileDescriptor, output: File, baseDir: File): CompletableFuture<Unit> {
        return Bridge.downloadProfile(fd.detachFd(), baseDir.absolutePath, output.absolutePath).thenApply { Unit }
    }

    fun queryProxyGroups(): List<ProxyGroup> {
        return Bridge.queryProxyGroups().toList()
    }

    fun setSelector(name: String, selected: String): Boolean {
        return Bridge.setSelector(name, selected)
    }

    fun performHealthCheck(group: String): CompletableFuture<Unit> {
        return Bridge.performHealthCheck(group).thenApply { Unit }
    }

    fun setProxyMode(mode: String) {
        Bridge.setProxyMode(mode)
    }

    fun queryGeneral(): General {
        return Bridge.queryGeneral()
    }

    fun querySpeed(): Traffic {
        return Bridge.querySpeed()
    }

    fun queryBandwidth(): Traffic {
        return Bridge.queryBandwidth()
    }

    fun registerLogReceiver(key: String, receiver: (LogEvent) -> Unit) {
        synchronized(logReceivers) {
            if ( logReceivers.isEmpty() )
                Bridge.enableLogReport()
            logReceivers[key] = receiver
        }
    }

    fun unregisterLogReceiver(key: String) {
        synchronized(logReceivers) {
            logReceivers.remove(key)
            if ( logReceivers.isEmpty() )
                Bridge.disableLogReport();
        }
    }
}