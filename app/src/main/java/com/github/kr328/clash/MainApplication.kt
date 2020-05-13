package com.github.kr328.clash

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.github.kr328.clash.common.Global
import com.github.kr328.clash.common.utils.componentName
import com.github.kr328.clash.dump.LogcatDumper
import com.github.kr328.clash.remote.Broadcasts
import com.github.kr328.clash.remote.Remote
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.AbstractCrashesListener
import com.microsoft.appcenter.crashes.Crashes
import com.microsoft.appcenter.crashes.ingestion.models.ErrorAttachmentLog
import com.microsoft.appcenter.crashes.model.ErrorReport

@Suppress("unused")
class MainApplication : Application() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        Global.init(this)
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize AppCenter
        if (BuildConfig.APP_CENTER_KEY.isNotEmpty() && !BuildConfig.DEBUG) {
            AppCenter.start(
                this,
                BuildConfig.APP_CENTER_KEY,
                Analytics::class.java, Crashes::class.java
            )

            Crashes.setListener(object : AbstractCrashesListener() {
                override fun getErrorAttachments(report: ErrorReport?): MutableIterable<ErrorAttachmentLog> {
                    report ?: return mutableListOf()

                    if (!report.stackTrace.contains("DeadObjectException"))
                        return mutableListOf()

                    val logcat = LogcatDumper.dumpCrash()

                    return mutableListOf(
                        ErrorAttachmentLog.attachmentWithText(logcat, "logcat.txt")
                    )
                }
            })
        }

        Global.openMainIntent = {
            Intent(Intent.ACTION_MAIN).apply {
                component = MainActivity::class.componentName
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        }
        Global.openProfileIntent = {
            Intent(Intent.ACTION_MAIN).apply {
                component = ProfileEditActivity::class.componentName
                data = Uri.fromParts("id", it.toString(), null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }

        Remote.init(this)
        Broadcasts.init(this)
    }
}