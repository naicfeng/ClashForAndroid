package com.github.kr328.clash.service.clash.module

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.github.kr328.clash.common.Global
import com.github.kr328.clash.common.ids.Intents
import com.github.kr328.clash.common.ids.NotificationChannels
import com.github.kr328.clash.common.ids.NotificationIds
import com.github.kr328.clash.service.R
import com.github.kr328.clash.service.ServiceStatusProvider

class StaticNotificationModule(private val service: Service) : Module() {
    override val receiveBroadcasts: Set<String>
        get() = setOf(Intents.INTENT_ACTION_PROFILE_LOADED)
    private val builder = NotificationCompat.Builder(service, NotificationChannels.CLASH_STATUS)
        .setSmallIcon(R.drawable.ic_notification)
        .setOngoing(true)
        .setColor(service.getColor(R.color.colorAccentService))
        .setOnlyAlertOnce(true)
        .setShowWhen(false)
        .setGroup(NotificationChannels.CLASH_STATUS)
        .setContentIntent(
            PendingIntent.getActivity(
                service,
                NotificationIds.CLASH_STATUS,
                Global.openMainIntent(),
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        )

    override suspend fun onBroadcastReceived(intent: Intent) {
        when (intent.action) {
            Intents.INTENT_ACTION_PROFILE_LOADED -> {
                update()
            }
        }
    }

    override suspend fun onStop() {
        service.stopForeground(true)
    }

    private fun update() {
        val profileName = ServiceStatusProvider.currentProfile ?: "Not selected"

        val notification = builder
            .setContentTitle(profileName)
            .setContentText(service.getText(R.string.running))
            .build()

        service.startForeground(NotificationIds.CLASH_STATUS, notification)
    }

    companion object {
        fun createNotificationChannel(service: Service) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
                return
            NotificationManagerCompat.from(service).createNotificationChannel(
                NotificationChannel(
                    NotificationChannels.CLASH_STATUS,
                    service.getText(R.string.clash_service_status_channel),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }

        fun notifyLoadingNotification(service: Service) {
            val notification =
                NotificationCompat.Builder(service, NotificationChannels.CLASH_STATUS)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setOngoing(true)
                    .setColor(service.getColor(R.color.colorAccentService))
                    .setOnlyAlertOnce(true)
                    .setShowWhen(false)
                    .setGroup(NotificationChannels.CLASH_STATUS)
                    .setContentTitle(service.getText(R.string.loading))
                    .build()

            service.startForeground(NotificationIds.CLASH_STATUS, notification)
        }
    }
}