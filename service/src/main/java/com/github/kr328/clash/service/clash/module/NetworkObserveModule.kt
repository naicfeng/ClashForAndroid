package com.github.kr328.clash.service.clash.module

import android.content.Context
import android.net.*
import com.github.kr328.clash.common.utils.Log
import kotlinx.coroutines.sync.Mutex
import java.net.InetAddress

class NetworkObserveModule(context: Context) : Module() {
    private var networkChanged: (Network?, List<InetAddress>) -> Unit = { _, _ -> }
    private var network: Network? = null
    private val lock = Mutex()
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)
    private val callback = object : ConnectivityManager.NetworkCallback() {
        private val internet = mutableMapOf<Network, Boolean>()
        private val dns = mutableMapOf<Network, List<InetAddress>>()

        override fun onAvailable(network: Network) {
            detectDefaultNetwork()
        }

        override fun onLost(network: Network) {
            internet.remove(network)
            dns.remove(network)

            detectDefaultNetwork()
        }

        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
            val old = dns[network]
            val new = linkProperties.dnsServers

            if (old != new) {
                dns[network] = new
                detectDefaultNetwork()
            }
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            val old = internet[network]
            val new = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

            if (old != new) {
                internet[network] = new
                detectDefaultNetwork()
            }
        }
    }

    override suspend fun onStart() {
        try {
            connectivity.registerNetworkCallback(NetworkRequest.Builder().build(), callback)
        } catch (e: Exception) {
            Log.w("Register NetworkCallback failure", e)
        }
    }

    override suspend fun onStop() {
        try {
            connectivity.unregisterNetworkCallback(callback)
        } catch (e: Exception) {
            Log.w("Unregister NetworkCallback failure", e)
        }
    }

    fun onNetworkChanged(callback: (Network?, List<InetAddress>) -> Unit) {
        networkChanged = callback
    }

    private fun detectDefaultNetwork() {
        if (!lock.tryLock())
            return

        val def = connectivity.allNetworks
            .asSequence()
            .mapNotNull { network ->
                connectivity.getNetworkCapabilities(network)?.let { it to network }
            }
            .filterNot {
                it.first.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ||
                        !it.first.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
            .sortedBy {
                when {
                    it.first.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> 0
                    it.first.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> 1
                    it.first.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> 2
                    it.first.hasTransport(NetworkCapabilities.TRANSPORT_LOWPAN) -> 3
                    it.first.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> 4
                    else -> 5
                } + if (it.first.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED))
                    -1000
                else
                    0
            }
            .map {
                it.second
            }
            .firstOrNull()

        if (def != network) {
            network = def

            networkChanged(def, connectivity.getLinkProperties(def)?.dnsServers ?: emptyList())
        }

        lock.unlock()
    }
}