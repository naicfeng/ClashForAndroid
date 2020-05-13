package com.github.kr328.clash.settings

import android.content.pm.PackageManager
import android.os.Bundle
import com.github.kr328.clash.R
import com.github.kr328.clash.common.utils.componentName
import com.github.kr328.clash.remote.Broadcasts
import com.github.kr328.clash.service.RestartReceiver
import com.github.kr328.clash.service.settings.ServiceSettings

class BehaviorFragment : BaseSettingFragment() {
    companion object {
        private const val KEY_START_ON_BOOT = "start_on_boot"
        private const val KEY_SHOW_TRAFFIC = "show_traffic"
    }

    override fun onCreateDataStore(): SettingsDataStore {
        return SettingsDataStore().apply {
            on(KEY_START_ON_BOOT, StartOnBootSource())
            on(KEY_SHOW_TRAFFIC, ServiceSettings.NOTIFICATION_REFRESH.asSource(service))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        findPreference(KEY_SHOW_TRAFFIC).isEnabled = !Broadcasts.clashRunning
    }

    override val xmlResourceId: Int
        get() = R.xml.settings_behavior

    private inner class StartOnBootSource : SettingsDataStore.Source {
        override fun set(value: Any?) {
            val v = value as Boolean? ?: return

            val status = if (v)
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED

            requireActivity().packageManager.setComponentEnabledSetting(
                RestartReceiver::class.componentName,
                status,
                PackageManager.DONT_KILL_APP
            )
        }

        override fun get(): Any? {
            val status = requireActivity().packageManager
                .getComponentEnabledSetting(RestartReceiver::class.componentName)

            return status == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }
    }
}