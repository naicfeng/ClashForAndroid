package com.github.kr328.clash.service

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.github.kr328.clash.service.clash.ClashRuntime
import com.github.kr328.clash.service.clash.module.*
import com.github.kr328.clash.service.settings.ServiceSettings
import com.github.kr328.clash.service.util.broadcastClashStarted
import com.github.kr328.clash.service.util.broadcastClashStopped
import com.github.kr328.clash.service.util.broadcastProfileLoaded
import kotlinx.coroutines.launch

class ClashService : BaseService() {
    private val service = this
    private val runtime = ClashRuntime(this)
    private var reason: String? = null

    override fun onCreate() {
        super.onCreate()

        if (ServiceStatusProvider.serviceRunning)
            return stopSelf()

        ServiceStatusProvider.serviceRunning = true

        StaticNotificationModule.createNotificationChannel(service)
        StaticNotificationModule.notifyLoadingNotification(service)

        launch {
            val settings = ServiceSettings(service)

            runtime.install(ReloadModule(service)) {
                onLoaded {
                    if (it != null) {
                        service.stopSelfForReason(it.message)
                    } else {
                        service.broadcastProfileLoaded()
                    }
                }
            }
            runtime.install(CloseModule()) {
                onClosed {
                    service.stopSelfForReason(null)
                }
            }

            if (settings.get(ServiceSettings.NOTIFICATION_REFRESH))
                runtime.install(DynamicNotificationModule(service))
            else
                runtime.install(StaticNotificationModule(service))

            runtime.exec()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        this.broadcastClashStarted()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return Binder()
    }

    override fun onDestroy() {
        ServiceStatusProvider.serviceRunning = false

        service.broadcastClashStopped(reason)

        super.onDestroy()
    }

    private fun stopSelfForReason(reason: String?) {
        this.reason = reason

        stopSelf()
    }
}