package com.github.kr328.clash.service

import android.content.Context
import android.content.SharedPreferences
import rikka.preference.MultiProcessPreference
import rikka.preference.PreferenceProvider

class ServiceSettingsProvider : PreferenceProvider() {
    override fun onCreatePreference(context: Context?): SharedPreferences {
        return context!!.getSharedPreferences(
            Constants.SERVICE_SETTING_FILE_NAME,
            Context.MODE_PRIVATE
        )
    }

    companion object {
        fun createSharedPreferencesFromContext(context: Context): SharedPreferences {
            return when (context) {
                is BaseService, is TunService ->
                    context.getSharedPreferences(
                        Constants.SERVICE_SETTING_FILE_NAME,
                        Context.MODE_PRIVATE
                    )
                else ->
                    MultiProcessPreference(
                        context,
                        context.packageName + Constants.SETTING_PROVIDER_SUFFIX
                    )
            }
        }
    }
}