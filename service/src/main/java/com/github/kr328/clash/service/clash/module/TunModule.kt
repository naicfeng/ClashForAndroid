package com.github.kr328.clash.service.clash.module

import android.app.PendingIntent
import android.net.VpnService
import android.os.Build
import com.github.kr328.clash.common.Global
import com.github.kr328.clash.common.ids.PendingIds
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.service.util.parseCIDR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.NullPointerException

class TunModule(private val service: VpnService) : Module() {
    interface Configure {
        val builder: VpnService.Builder
        val mtu: Int
        val gateway: String
        val mirror: String
        val route: List<String>
        val dnsAddress: String
        val dnsHijacking: Boolean
        val allowApplications: Collection<String>
        val disallowApplication: Collection<String>

        fun onCreateTunFailure()
    }

    var configure: Configure? = null

    override suspend fun onStart() {
        withContext(Dispatchers.IO) {
            val c = configure ?: throw IllegalArgumentException("Configure required")

            val builder = c.builder

            parseCIDR(c.gateway).let {
                builder.addAddress(it.ip, it.prefix)
            }
            c.route.map { parseCIDR(it) }.forEach {
                builder.addRoute(it.ip, it.prefix)
            }
            c.allowApplications.forEach {
                builder.addAllowedApplication(it)
            }
            c.disallowApplication.forEach {
                builder.addDisallowedApplication(it)
            }

            builder.setBlocking(false)
            builder.setMtu(c.mtu)
            builder.setSession("Clash")
            builder.addDnsServer(c.dnsAddress)
            builder.setConfigureIntent(
                PendingIntent.getActivity(
                    service,
                    PendingIds.CLASH_VPN,
                    Global.openMainIntent(),
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setMetered(false)
            }

            val fd = try {
                builder.establish() ?: throw NullPointerException()
            }
            catch (e: Exception) {
                return@withContext c.onCreateTunFailure()
            }

            if (c.dnsHijacking) {
                Clash.startTunDevice(
                    fd.detachFd(),
                    c.mtu,
                    c.gateway,
                    c.mirror,
                    IPV4_ANY,
                    service::protect,
                    service::stopSelf
                )
            } else {
                Clash.startTunDevice(
                    fd.detachFd(),
                    c.mtu,
                    c.gateway,
                    c.mirror,
                    c.dnsAddress,
                    service::protect,
                    service::stopSelf
                )
            }
        }
    }

    override suspend fun onStop() {
        Clash.stopTunDevice()
    }

    companion object {
        private const val IPV4_ANY = "0.0.0.0"

        fun requestStop() {
            Clash.stopTunDevice()
        }
    }
}