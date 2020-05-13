package com.github.kr328.clash.preference

import android.content.Context
import com.github.kr328.clash.common.settings.BaseSettings

class UiSettings(context: Context) :
    BaseSettings(context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)) {
    companion object {
        private const val FILE_NAME = "ui"

        const val PROXY_SORT_DEFAULT = "default"
        const val PROXY_SORT_NAME = "name"
        const val PROXY_SORT_DELAY = "delay"

        const val DARK_MODE_AUTO = "auto"
        const val DARK_MODE_DARK = "dark"
        const val DARK_MODE_LIGHT = "light"

        val PROXY_GROUP_SORT = StringEntry("proxy_group_sort", PROXY_SORT_DEFAULT)
        val PROXY_PROXY_SORT = StringEntry("proxy_proxy_sort", PROXY_SORT_DEFAULT)
        val PROXY_LAST_SELECT_GROUP = StringEntry("proxy_last_select_group", "")
        val PROXY_MERGE_PREFIX = BooleanEntry("proxy_merge_prefix", false)
        val LANGUAGE = StringEntry("language", "")
        val DARK_MODE = StringEntry("dark_mode", DARK_MODE_AUTO)
    }
}