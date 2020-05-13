package com.github.kr328.clash.remote

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Base64
import androidx.core.content.edit
import com.github.kr328.clash.ApkBrokenActivity
import com.github.kr328.clash.BuildConfig
import com.github.kr328.clash.Constants
import com.github.kr328.clash.common.Global
import com.github.kr328.clash.common.utils.intent
import com.github.kr328.clash.common.utils.Log
import com.github.kr328.clash.service.ClashManagerService
import com.github.kr328.clash.service.IClashManager
import com.github.kr328.clash.service.IProfileService
import com.github.kr328.clash.service.ProfileService
import com.github.kr328.clash.utils.ApplicationObserver
import com.microsoft.appcenter.crashes.Crashes
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.File
import java.util.zip.ZipFile

object Remote {
    var clash = Channel<ClashClient>()
    var profile = Channel<ProfileClient>()

    private var clashConnection: ClashConnection? = null
    private var profileConnection: ProfileConnection? = null

    class ClashConnection : ServiceConnection {
        private var instance: ClashClient? = null
        private var sender: Job? = null

        override fun onServiceDisconnected(name: ComponentName?) {
            sender?.cancel()
            instance = null
            sender = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service != null)
                instance = ClashClient(IClashManager.Stub.asInterface(service))

            service?.linkToDeath({ onServiceDisconnected(null) }, 0)

            sender = GlobalScope.launch {
                while (isActive) {
                    val client = instance ?: return@launch
                    clash.send(client)
                }
            }
        }
    }

    class ProfileConnection : ServiceConnection {
        private var instance: ProfileClient? = null
        private var sender: Job? = null

        override fun onServiceDisconnected(name: ComponentName?) {
            sender?.cancel()
            instance = null
            sender = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service != null)
                instance = ProfileClient(IProfileService.Stub.asInterface(service))

            service?.linkToDeath({ onServiceDisconnected(null) }, 0)

            sender = GlobalScope.launch {
                while (isActive) {
                    val client = instance ?: return@launch
                    profile.send(client)
                }
            }
        }
    }

    private val handler = Handler()
    private val observer = ApplicationObserver {
        Log.d("Remote Connection State = $it")

        val application = Global.application

        if (it) {
            handler.removeMessages(0)

            GlobalScope.launch {
                val valid = withContext(Dispatchers.IO) {
                    verifyApk(application)
                }

                if (!valid) {
                    application.startActivity(
                        ApkBrokenActivity::class.intent
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                    return@launch
                }

                clashConnection = ClashConnection().apply {
                    application.bindService(
                        ClashManagerService::class.intent,
                        this,
                        Context.BIND_AUTO_CREATE
                    )
                }

                profileConnection = ProfileConnection().apply {
                    application.bindService(
                        ProfileService::class.intent,
                        this,
                        Context.BIND_AUTO_CREATE
                    )
                }
            }
        } else {
            handler.postDelayed({
                clashConnection?.also {
                    application.unbindService(it)
                    it.onServiceDisconnected(null)
                }
                profileConnection?.also {
                    application.unbindService(it)
                    it.onServiceDisconnected(null)
                }

                clashConnection = null
                profileConnection = null
            }, 5000)
        }
    }

    fun init(application: Application) {
        observer.register(application)
    }

    private fun verifyApk(application: Application): Boolean {
        return try {
            val sp = application.getSharedPreferences(
                Constants.PREFERENCE_NAME_APP,
                Context.MODE_PRIVATE
            )
            val pkg = application.packageManager.getPackageInfo(application.packageName, 0)

            if (sp.getLong(Constants.PREFERENCE_KEY_LAST_INSTALL, 0) == pkg.lastUpdateTime)
                return true

            val pkgName: String = try {
                application::class.java.getMethod(
                    String(
                        charArrayOf(
                            'g',
                            'e',
                            't',
                            'P',
                            'a',
                            'c',
                            'k',
                            'a',
                            'g',
                            'e',
                            'N',
                            'a',
                            'm',
                            'e'
                        )
                    )
                ).invoke(application)?.toString()
            } catch (e: Exception) {
                Log.w("getPackageName failure", e)
                null
            } ?: application.packageName

            val packageNameBase64 = Base64
                .encodeToString(pkgName.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

            if (packageNameBase64 != BuildConfig.PACKAGE_NAME_BASE64)
                return false

            val info = application.applicationInfo
            val sources =
                info.splitSourceDirs ?: arrayOf(info.sourceDir) ?: return false

            val regexNativeLibrary = Regex("lib/(\\S+)/libgojni.so")
            val availableAbi = Build.SUPPORTED_ABIS.toSet()
            val apkAbi =
                sources
                    .asSequence()
                    .filter { File(it).exists() }
                    .flatMap { ZipFile(it).entries().asSequence() }
                    .mapNotNull { regexNativeLibrary.matchEntire(it.name) }
                    .mapNotNull { it.groups[1]?.value }
                    .toSet()

            if (availableAbi.intersect(apkAbi).isNotEmpty()) {
                sp.edit {
                    putLong(Constants.PREFERENCE_KEY_LAST_INSTALL, pkg.lastUpdateTime)
                }

                true
            } else {
                false
            }
        } catch (e: Exception) {
            Crashes.trackError(e)

            false
        }
    }
}