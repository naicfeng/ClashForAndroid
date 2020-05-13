package com.github.kr328.clash.service.settings

import android.content.Context
import android.content.SharedPreferences
import com.github.kr328.clash.common.settings.BaseSettings
import com.github.kr328.clash.service.ServiceSettingsProvider

class ServiceSettings(preference: SharedPreferences) :
    BaseSettings(preference) {
    constructor(context: Context) : this(
        ServiceSettingsProvider.createSharedPreferencesFromContext(context)
    )

    companion object {
        const val ACCESS_CONTROL_MODE_ALL = "access_control_mode_all"
        const val ACCESS_CONTROL_MODE_BLACKLIST = "access_control_mode_blacklist"
        const val ACCESS_CONTROL_MODE_WHITELIST = "access_control_mode_whitelist"

        val ENABLE_VPN =
            BooleanEntry("enable_vpn", true)
        val LANGUAGE =
            StringEntry("language", "")
        val BYPASS_PRIVATE_NETWORK =
            BooleanEntry("bypass_private_network", true)
        val ACCESS_CONTROL_MODE =
            StringEntry("access_control_mode", ACCESS_CONTROL_MODE_ALL)
        val ACCESS_CONTROL_PACKAGES =
            StringSetEntry("access_control_packages", emptySet())
        val DNS_HIJACKING =
            BooleanEntry("dns_hijacking", true)
        val NOTIFICATION_REFRESH =
            BooleanEntry("notification_refresh", true)
        val AUTO_ADD_SYSTEM_DNS =
            BooleanEntry("auto_add_system_dns", true)
        val OVERRIDE_DNS =
            BooleanEntry("override_dns", true)
    }
}