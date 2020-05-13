package com.github.kr328.clash.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.getSystemService
import com.github.kr328.clash.common.ids.Intents
import com.github.kr328.clash.common.ids.PendingIds
import com.github.kr328.clash.common.utils.componentName
import com.github.kr328.clash.common.utils.startForegroundServiceCompat
import com.github.kr328.clash.service.data.ProfileDao
import com.github.kr328.clash.service.model.asProfile
import kotlinx.coroutines.sync.Mutex

class ProfileReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return

        when (intent?.action) {
            Intents.INTENT_ACTION_PROFILE_REQUEST_UPDATE -> {
                // Redirect to service
                intent.component = ProfileBackgroundService::class.componentName
                context.startForegroundServiceCompat(intent)
            }
        }
    }

    companion object {
        private val initialized = Mutex()

        @Synchronized
        suspend fun initialize(context: Context) {
            if ( !initialized.tryLock() )
                return

            ProfileDao.queryAllIds().forEach {
                requestNextUpdate(context, it)
            }
        }

        suspend fun requestNextUpdate(context: Context, id: Long) {
            val metadata = ProfileDao.queryById(id)?.asProfile(context) ?: return
            val service = context.getSystemService<AlarmManager>() ?: return

            val pendingIntent = cancelNextUpdate(context, id)

            if (metadata.interval <= 0)
                return

            service.set(
                AlarmManager.RTC,
                metadata.lastModified + metadata.interval,
                pendingIntent
            )
        }

        fun cancelNextUpdate(context: Context, id: Long): PendingIntent {
            val intent = buildUpdatePendingIntent(context, id)
            val service = context.getSystemService<AlarmManager>() ?: return intent

            service.cancel(intent)

            return intent
        }

        fun buildUpdateIntentForId(id: Long): Intent {
            return Intent(Intents.INTENT_ACTION_PROFILE_REQUEST_UPDATE)
                .setComponent(ProfileReceiver::class.componentName)
                .setData(Uri.fromParts("id", id.toString(), null))
        }

        private fun buildUpdatePendingIntent(context: Context, id: Long): PendingIntent {
            return PendingIntent.getBroadcast(
                context,
                PendingIds.generateProfileResultId(id),
                buildUpdateIntentForId(id),
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }
}