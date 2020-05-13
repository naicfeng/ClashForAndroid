package com.github.kr328.clash.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.RemoteException
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.github.kr328.clash.common.Global
import com.github.kr328.clash.common.ids.Intents
import com.github.kr328.clash.common.ids.NotificationChannels
import com.github.kr328.clash.common.ids.NotificationIds
import com.github.kr328.clash.common.ids.PendingIds
import com.github.kr328.clash.common.utils.intent
import com.github.kr328.clash.service.data.ProfileDao
import com.github.kr328.clash.service.transact.IStreamCallback
import com.github.kr328.clash.service.transact.ParcelableContainer
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

class ProfileBackgroundService : BaseService() {
    private val self = this
    private val requests = Channel<Long>(Channel.UNLIMITED)
    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            stopSelf()
        }

        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = IProfileService.Stub.asInterface(binder) ?: return stopSelf()

            processProfiles(service)
        }
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannels()

        refreshStatusNotification(0)

        bindService(ProfileService::class.intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()

        unbindService(connection)

        stopForeground(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            Intents.INTENT_ACTION_PROFILE_REQUEST_UPDATE -> {
                val id = intent.data?.schemeSpecificPart?.toLongOrNull()
                    ?: return START_NOT_STICKY

                requests.offer(id)
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return Binder()
    }

    private fun processProfiles(service: IProfileService) = launch {
        val queue: MutableSet<Long> = mutableSetOf()
        val responses = Channel<Pair<Long, Exception?>>(Channel.UNLIMITED)

        while (true) {
            val stop = select<Boolean> {
                requests.onReceive {
                    ProfileReceiver.cancelNextUpdate(self, it)

                    queue.add(it)

                    service.commit(it, object : IStreamCallback.Stub() {
                        override fun completeExceptionally(reason: String?) {
                            responses.offer(it to RemoteException(reason))
                        }

                        override fun complete() {
                            responses.offer(it to null)
                        }

                        override fun send(data: ParcelableContainer?) {}
                    })

                    false
                }
                responses.onReceive {
                    queue.remove(it.first)

                    if (it.second == null)
                        sendUpdateCompleted(it.first)
                    else
                        sendUpdateFailed(it.first, it.second!!.message ?: "Unknown")

                    false
                }
                if (queue.isEmpty()) {
                    launch { delay(1000 * 5) }.onJoin {
                        true
                    }
                }
            }

            refreshStatusNotification(queue.size)

            if (stop) break
        }

        stopSelf()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return

        NotificationManagerCompat.from(this).createNotificationChannels(
            listOf(
                NotificationChannel(
                    NotificationChannels.PROFILE_STATUS,
                    getText(R.string.profile_service_status_channel),
                    NotificationManager.IMPORTANCE_LOW
                ),
                NotificationChannel(
                    NotificationChannels.PROFILE_RESULT,
                    getText(R.string.profile_status_channel),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        )
    }

    private fun refreshStatusNotification(queueSize: Int) {
        val content = if (queueSize != 0)
            getString(R.string.format_in_queue, queueSize)
        else
            getString(R.string.waiting)

        val notification = NotificationCompat.Builder(this, NotificationChannels.PROFILE_STATUS)
            .setContentTitle(getText(R.string.processing_profiles))
            .setContentText(content)
            .setColor(getColor(R.color.colorAccentService))
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setGroup(NotificationChannels.PROFILE_STATUS)
            .build()

        startForeground(NotificationIds.PROFILE_STATUS, notification)
    }

    private suspend fun sendUpdateCompleted(id: Long) {
        val entity = ProfileDao.queryById(id) ?: return

        val intent = PendingIntent.getActivity(
            this,
            PendingIds.generateProfileResultId(id),
            Global.openProfileIntent(id),
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, NotificationChannels.PROFILE_RESULT)
            .setContentTitle(getText(R.string.process_result))
            .setContentText(getString(R.string.format_update_complete, entity.name))
            .setColor(getColor(R.color.colorAccentService))
            .setSmallIcon(R.drawable.ic_notification)
            .setOnlyAlertOnce(true)
            .setGroup(NotificationChannels.PROFILE_RESULT)
            .setAutoCancel(true)
            .setContentIntent(intent)
            .build()

        NotificationManagerCompat.from(this)
            .notify(NotificationIds.generateProfileResultId(id), notification)
    }

    private suspend fun sendUpdateFailed(id: Long, reason: String) {
        val entity = ProfileDao.queryById(id) ?: return

        val intent = PendingIntent.getActivity(
            this,
            PendingIds.generateProfileResultId(id),
            Global.openProfileIntent(id),
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, NotificationChannels.PROFILE_RESULT)
            .setContentTitle(getString(R.string.format_update_failure, entity.name))
            .setColor(getColor(R.color.colorAccentService))
            .setSmallIcon(R.drawable.ic_notification)
            .setStyle(NotificationCompat.BigTextStyle().bigText(reason))
            .setOnlyAlertOnce(true)
            .setGroup(NotificationChannels.PROFILE_RESULT)
            .setAutoCancel(true)
            .setContentIntent(intent)
            .build()

        NotificationManagerCompat.from(this)
            .notify(NotificationIds.generateProfileResultId(id), notification)
    }
}