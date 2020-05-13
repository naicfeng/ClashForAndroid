package com.github.kr328.clash.common.ids

import com.github.kr328.clash.common.BuildConfig

object Intents {
    const val INTENT_ACTION_CLASH_STARTED =
        "${BuildConfig.LIBRARY_PACKAGE_NAME}.intent.action.clash.STARTED"
    const val INTENT_ACTION_CLASH_STOPPED =
        "${BuildConfig.LIBRARY_PACKAGE_NAME}.intent.action.clash.STOPPED"
    const val INTENT_ACTION_CLASH_REQUEST_STOP =
        "${BuildConfig.LIBRARY_PACKAGE_NAME}.intent.action.clash.REQUEST_STOP"
    const val INTENT_ACTION_PROFILE_CHANGED =
        "${BuildConfig.LIBRARY_PACKAGE_NAME}.intent.action.profile.CHANGED"
    const val INTENT_ACTION_PROFILE_REQUEST_UPDATE =
        "${BuildConfig.LIBRARY_PACKAGE_NAME}.intent.action.profile.REQUEST_UPDATE"
    const val INTENT_ACTION_PROFILE_LOADED =
        "${BuildConfig.LIBRARY_PACKAGE_NAME}.intent.action.profile.LOADED"
    const val INTENT_ACTION_NETWORK_CHANGED =
        "${BuildConfig.LIBRARY_PACKAGE_NAME}.intent.action.network.CHANGED"

    const val INTENT_EXTRA_CLASH_STOP_REASON =
        "${BuildConfig.LIBRARY_PACKAGE_NAME}.intent.extra.clash.STOP_REASON"
}