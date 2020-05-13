package com.github.kr328.clash.service

import android.content.Intent
import android.net.VpnService
import com.github.kr328.clash.service.clash.ClashRuntime
import com.github.kr328.clash.service.clash.module.*
import com.github.kr328.clash.service.settings.ServiceSettings
import com.github.kr328.clash.service.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class TunService : VpnService(), CoroutineScope by MainScope() {
    companion object {
        // from https://github.com/shadowsocks/shadowsocks-android/blob/master/core/src/main/java/com/github/shadowsocks/bg/VpnService.kt
        private const val VPN_MTU = 9000
        private const val PRIVATE_VLAN4_SUBNET = 30
        private const val PRIVATE_VLAN4_CLIENT = "172.31.255.253"
        private const val PRIVATE_VLAN4_MIRROR = "172.31.255.254"
        private const val PRIVATE_VLAN_DNS = "198.18.0.1"
        private const val VLAN_ANY = "0.0.0.0/0"
    }

    private val service = this
    private val runtime = ClashRuntime(this)
    private var reason: String? = null

    override fun onCreate() {
        super.onCreate()

        if (ServiceStatusProvider.serviceRunning)
            return stopSelf()

        ServiceStatusProvider.serviceRunning = true

        StaticNotificationModule.createNotificationChannel(this)
        StaticNotificationModule.notifyLoadingNotification(this)

        launch {
            val settings = ServiceSettings(service)
            val dnsInject = DnsInjectModule()

            runtime.install(TunModule(service)) {
                configure = TunConfigure(settings)
            }

            runtime.install(ReloadModule(service)) {
                onLoaded {
                    if (it != null) {
                        service.stopSelfForReason(it.message)
                    } else {
                        service.broadcastProfileLoaded()
                    }
                }
            }
            runtime.install(CloseModule()) {
                onClosed {
                    service.stopSelfForReason(null)
                }
            }

            if (settings.get(ServiceSettings.NOTIFICATION_REFRESH))
                runtime.install(DynamicNotificationModule(service))
            else
                runtime.install(StaticNotificationModule(service))

            runtime.install(dnsInject) {
                dnsOverride = settings.get(ServiceSettings.OVERRIDE_DNS)
            }

            runtime.install(NetworkObserveModule(service)) {
                onNetworkChanged { network, dnsServers ->
                    setUnderlyingNetworks(network?.let { arrayOf(it) })

                    if (settings.get(ServiceSettings.AUTO_ADD_SYSTEM_DNS)) {
                        val dnsStrings = dnsServers.map {
                            it.asSocketAddressText(53)
                        }

                        dnsInject.appendDns = dnsStrings
                    }

                    broadcastNetworkChanged()
                }
            }

            runtime.exec()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        service.broadcastClashStarted()

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        TunModule.requestStop()

        ServiceStatusProvider.serviceRunning = false

        service.broadcastClashStopped(reason)

        cancel()

        super.onDestroy()
    }

    private inner class TunConfigure(private val settings: ServiceSettings) : TunModule.Configure {
        override val builder: Builder
            get() = Builder()
        override val mtu: Int
            get() = VPN_MTU
        override val gateway: String
            get() = "$PRIVATE_VLAN4_CLIENT/$PRIVATE_VLAN4_SUBNET"
        override val mirror: String
            get() = PRIVATE_VLAN4_MIRROR
        override val route: List<String>
            get() {
                return if (settings.get(ServiceSettings.BYPASS_PRIVATE_NETWORK))
                    resources.getStringArray(R.array.bypass_private_route).toList()
                else
                    resources.getStringArray(R.array.bypass_local_route).toList()
            }
        override val dnsAddress: String
            get() = PRIVATE_VLAN_DNS
        override val dnsHijacking: Boolean
            get() = settings.get(ServiceSettings.DNS_HIJACKING)
        override val allowApplications: Collection<String>
            get() {
                return if (settings.get(ServiceSettings.ACCESS_CONTROL_MODE) == ServiceSettings.ACCESS_CONTROL_MODE_WHITELIST) {
                    (settings.get(ServiceSettings.ACCESS_CONTROL_PACKAGES) + packageName)
                } else emptySet()
            }
        override val disallowApplication: Collection<String>
            get() {
                return if (settings.get(ServiceSettings.ACCESS_CONTROL_MODE) == ServiceSettings.ACCESS_CONTROL_MODE_BLACKLIST) {
                    (settings.get(ServiceSettings.ACCESS_CONTROL_PACKAGES) - packageName)
                } else emptySet()
            }

        override fun onCreateTunFailure() {
            stopSelfForReason("Establish VPN rejected by system")
        }
    }

    private fun stopSelfForReason(reason: String?) {
        this.reason = reason

        stopSelf()

        TunModule.requestStop()
    }
}