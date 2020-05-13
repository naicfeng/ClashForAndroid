package com.github.kr328.clash

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.IInterface
import androidx.collection.CircularArray
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.github.kr328.clash.common.utils.createLanguageConfigurationContext
import com.github.kr328.clash.common.utils.intent
import com.github.kr328.clash.common.utils.Log
import com.github.kr328.clash.core.event.LogEvent
import com.github.kr328.clash.model.LogFile
import com.github.kr328.clash.preference.UiSettings
import com.github.kr328.clash.service.ClashManagerService
import com.github.kr328.clash.service.IClashManager
import com.github.kr328.clash.service.transact.IStreamCallback
import com.github.kr328.clash.service.transact.ParcelableContainer
import com.github.kr328.clash.utils.format
import com.github.kr328.clash.utils.logsDir
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import java.io.FileWriter
import java.io.IOException
import java.util.*
import kotlin.math.max

class LogcatService : Service(), CoroutineScope by MainScope(), IInterface {
    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "clash_logcat_channel"
        private const val NOTIFICATION_ID = 256
        private const val MAX_CACHE_COUNT = 200
        private const val LOG_LISTENER_KEY = "logcat_service"

        private const val LOG_CONTENT_FORMAT = "%d %s %s"

        var isServiceRunning: Boolean = false
    }

    data class Request(val offset: Long, val response: CompletableDeferred<Response>)
    data class Response(val offset: Long, val logs: List<LogEvent>)

    private val logChannel = Channel<LogEvent>(MAX_CACHE_COUNT)
    private val requestChannel = Channel<Request>()
    private val cache: CircularArray<LogEvent> = CircularArray()
    private var cacheOffset = 0L
    private val entity = LogFile.generate()

    private val connection = object : ServiceConnection {
        private var manager: IClashManager? = null

        override fun onServiceDisconnected(name: ComponentName?) {
            manager?.unregisterLogListener(LOG_LISTENER_KEY)
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            manager = IClashManager.Stub.asInterface(service) ?: return stopSelf()

            manager?.registerLogListener(LOG_LISTENER_KEY, object : IStreamCallback.Stub() {
                override fun complete() {}
                override fun completeExceptionally(reason: String?) {}
                override fun send(data: ParcelableContainer?) {
                    data ?: return
                    data.data ?: return

                    logChannel.offer(data.data as LogEvent)
                }
            })
        }
    }

    override fun onCreate() {
        super.onCreate()

        isServiceRunning = true

        createNotificationChannel()
        showNotification()

        bindService(ClashManagerService::class.intent, connection, Context.BIND_AUTO_CREATE)

        launchProcessor()

        launchSaveThread()
    }

    override fun onDestroy() {
        logChannel.close()

        cancel()

        connection.onServiceDisconnected(null)

        unbindService(connection)

        stopForeground(true)

        isServiceRunning = false

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return this.asBinder()
    }

    override fun asBinder(): IBinder {
        return object : Binder() {
            override fun queryLocalInterface(descriptor: String): IInterface? {
                return this@LogcatService
            }
        }
    }

    override fun attachBaseContext(base: Context?) {
        val b = base ?: return super.attachBaseContext(base)

        val language = UiSettings(b).get(UiSettings.LANGUAGE)

        super.attachBaseContext(b.createLanguageConfigurationContext(language))
    }

    // Export to UI
    suspend fun pollLogEvent(offset: Long): CompletableDeferred<Response> {
        val request = Request(offset, CompletableDeferred())

        requestChannel.send(request)

        return request.response
    }

    private fun launchProcessor() {
        launch {
            val pendingRequest: MutableList<Request> = mutableListOf()

            try {
                withContext(Dispatchers.Default) {
                    while (isActive) {
                        select<Unit> {
                            logChannel.onReceive {
                                cache.addLast(it)

                                if (cache.size() > MAX_CACHE_COUNT) {
                                    cache.removeFromStart(1)
                                    cacheOffset++
                                }
                            }
                            requestChannel.onReceive {
                                pendingRequest.add(it)
                            }
                        }

                        // Handle pending requests
                        val iterator = pendingRequest.iterator()
                        while (iterator.hasNext()) {
                            val request = iterator.next()

                            if (request.offset >= cacheOffset + cache.size())
                                continue

                            val logs = mutableListOf<LogEvent>()

                            val responseOffset = max(cacheOffset, request.offset)
                            val begin = (responseOffset - cacheOffset).toInt()

                            for (i in begin until cache.size())
                                logs.add(cache[i])

                            request.response.complete(Response(responseOffset, logs))

                            iterator.remove()
                        }
                    }
                }
            } catch (e: Exception) {
                return@launch
            }
        }
    }

    private fun launchSaveThread() {
        launch {
            withContext(Dispatchers.IO) {
                logsDir.mkdirs()
                try {
                    FileWriter(logsDir.resolve(entity.fileName)).buffered().use { output ->
                        var offset = 0L

                        output.write("# Logcat on ${Date(entity.date).format(this@LogcatService)}")
                        output.newLine()

                        while (isActive) {
                            val response = pollLogEvent(offset).await()

                            if (response.offset != offset) {
                                output.write("# Lost ${response.offset - offset} items")
                                output.newLine()
                            }

                            response.logs.forEach {
                                output.write(
                                    LOG_CONTENT_FORMAT.format(
                                        it.time,
                                        it.level,
                                        it.message
                                    )
                                )
                                output.newLine()
                            }

                            offset = response.offset + response.logs.size
                        }
                    }
                } catch (e: IOException) {
                    Log.w("Logcat file write failure", e)
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return

        NotificationManagerCompat.from(this)
            .createNotificationChannel(
                NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    getString(R.string.clash_logcat),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
    }

    private fun showNotification() {
        val notification = NotificationCompat
            .Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(getColor(R.color.colorAccentService))
            .setContentTitle(getString(R.string.clash_logcat))
            .setContentText(getString(R.string.running))
            .setGroup(NOTIFICATION_CHANNEL_ID)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    NOTIFICATION_ID,
                    LogsActivity::class.intent
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }
}