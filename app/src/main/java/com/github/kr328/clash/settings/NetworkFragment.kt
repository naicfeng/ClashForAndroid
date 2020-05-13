package com.github.kr328.clash.settings

import android.os.Bundle
import com.github.kr328.clash.PackagesActivity
import com.github.kr328.clash.R
import com.github.kr328.clash.common.utils.intent
import com.github.kr328.clash.remote.Broadcasts
import com.github.kr328.clash.service.settings.ServiceSettings

class NetworkFragment : BaseSettingFragment() {
    companion object {
        private const val KEY_ENABLE_VPN_SERVICE = "enable_vpn_service"
        private const val BYPASS_PRIVATE_NETWORK = "bypass_private_network"
        private const val KEY_DNS_HIJACKING = "dns_hijacking"
        private const val KEY_DNS_OVERRIDE = "dns_override"
        private const val KEY_APPEND_SYS_DNS = "append_system_dns"
        private const val KEY_ACCESS_CONTROL_MODE = "access_control_mode"
        private const val KEY_ACCESS_CONTROL_PACKAGES = "access_control_packages"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferenceScreen.isEnabled = !Broadcasts.clashRunning

        findPreference(KEY_ACCESS_CONTROL_PACKAGES).setOnPreferenceClickListener {
            startActivity(PackagesActivity::class.intent)
            true
        }
    }

    override fun onCreateDataStore(): SettingsDataStore {
        return SettingsDataStore().apply {
            on(KEY_ENABLE_VPN_SERVICE, ServiceSettings.ENABLE_VPN.asSource(service))
            on(BYPASS_PRIVATE_NETWORK, ServiceSettings.BYPASS_PRIVATE_NETWORK.asSource(service))
            on(KEY_DNS_HIJACKING, ServiceSettings.DNS_HIJACKING.asSource(service))
            on(KEY_DNS_OVERRIDE, ServiceSettings.OVERRIDE_DNS.asSource(service))
            on(KEY_APPEND_SYS_DNS, ServiceSettings.AUTO_ADD_SYSTEM_DNS.asSource(service))
            on(KEY_ACCESS_CONTROL_MODE, ServiceSettings.ACCESS_CONTROL_MODE.asSource(service))
        }
    }

    override val xmlResourceId: Int
        get() = R.xml.settings_network
}

