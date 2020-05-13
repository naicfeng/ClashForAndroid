package com.github.kr328.clash.service

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import com.github.kr328.clash.common.Global

class ServiceStatusProvider : ContentProvider() {
    companion object {
        const val METHOD_PING_CLASH_SERVICE = "pingClashService"
        private const val CLASH_SERVICE_RUNNING_FILE = "service_running"

        var serviceRunning: Boolean = false
            set(value) {
                field = value
                shouldStartClashOnBoot = value
            }
        var shouldStartClashOnBoot: Boolean
            get() = Global.application.cacheDir.resolve(CLASH_SERVICE_RUNNING_FILE).exists()
            set(value) {
                Global.application.cacheDir.resolve(CLASH_SERVICE_RUNNING_FILE).apply {
                    if (value)
                        createNewFile()
                    else
                        delete()
                }
            }
        var currentProfile: String? = null
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        return when (method) {
            METHOD_PING_CLASH_SERVICE -> {
                return if (serviceRunning)
                    Bundle().apply {
                        putString("name", currentProfile)
                    }
                else
                    null
            }
            else -> super.call(method, arg, extras)
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        throw IllegalArgumentException("Stub!")
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        throw IllegalArgumentException("Stub!")
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        throw IllegalArgumentException("Stub!")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        throw IllegalArgumentException("Stub!")
    }

    override fun getType(uri: Uri): String? {
        throw IllegalArgumentException("Stub!")
    }

    override fun onCreate(): Boolean {
        return true
    }
}