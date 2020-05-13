package com.github.kr328.clash

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.github.kr328.clash.common.Permissions
import com.github.kr328.clash.common.ids.Intents
import com.github.kr328.clash.remote.RemoteUtils
import com.github.kr328.clash.service.util.startClashService
import com.github.kr328.clash.service.util.stopClashService

class TileService : TileService() {
    private var currentProfile = ""
    private var clashRunning = false

    override fun onClick() {
        val tile = qsTile

        when (tile.state) {
            Tile.STATE_INACTIVE -> {
                startClashService()
            }
            Tile.STATE_ACTIVE -> {
                stopClashService()
            }
        }
    }

    override fun onStartListening() {
        refreshStatus()
    }

    private fun refreshStatus() {
        if (qsTile == null)
            return

        qsTile.state = if (clashRunning)
            Tile.STATE_ACTIVE
        else
            Tile.STATE_INACTIVE

        qsTile.label = if (currentProfile.isEmpty())
            getText(R.string.launch_name)
        else
            currentProfile

        qsTile.icon = Icon.createWithResource(this, R.drawable.ic_notification)

        qsTile.updateTile()
    }

    private val clashStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intents.INTENT_ACTION_CLASH_STARTED -> {
                    clashRunning = true

                    currentProfile = ""
                }
                Intents.INTENT_ACTION_CLASH_STOPPED -> {
                    clashRunning = false

                    currentProfile = ""
                }
                Intents.INTENT_ACTION_PROFILE_LOADED -> {
                    currentProfile = RemoteUtils
                        .getCurrentClashProfileName(this@TileService) ?: ""
                }
            }

            refreshStatus()
        }
    }

    override fun onCreate() {
        super.onCreate()

        registerReceiver(
            clashStatusReceiver,
            IntentFilter().apply {
                addAction(Intents.INTENT_ACTION_CLASH_STARTED)
                addAction(Intents.INTENT_ACTION_CLASH_STOPPED)
                addAction(Intents.INTENT_ACTION_PROFILE_LOADED)
            },
            Permissions.PERMISSION_RECEIVE_BROADCASTS,
            null
        )

        val name = RemoteUtils.getCurrentClashProfileName(this)

        clashRunning = name != null
        currentProfile = name ?: ""
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(clashStatusReceiver)
    }
}