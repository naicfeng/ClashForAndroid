package com.github.kr328.clash.service.data.migrations

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.core.database.getStringOrNull
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.github.kr328.clash.common.Global
import com.github.kr328.clash.common.utils.Log

object Migration23: Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        try {
            database.execSQL("ALTER TABLE profile_select_proxies RENAME TO _selected_proxies")
            database.execSQL("ALTER TABLE profiles RENAME TO _profiles")

            database.execSQL("CREATE TABLE IF NOT EXISTS `profiles` (`name` TEXT NOT NULL, `type` INTEGER NOT NULL, `uri` TEXT NOT NULL, `source` TEXT, `active` INTEGER NOT NULL, `interval` INTEGER NOT NULL, `id` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `selected_proxies` (`profile_id` INTEGER NOT NULL, `proxy` TEXT NOT NULL, `selected` TEXT NOT NULL, PRIMARY KEY(`profile_id`, `proxy`), FOREIGN KEY(`profile_id`) REFERENCES `profiles`(`id`) ON UPDATE CASCADE ON DELETE CASCADE )")

            database.query("SELECT name, type, uri, source, active, update_interval, id FROM _profiles")
                .use { cursor ->
                    cursor.moveToFirst()
                    while (!cursor.isAfterLast) {
                        // old
                        // name, type, uri, source, active, last_update, update_interval(seconds), id
                        // new
                        // name, type, uri, source, active, interval(millis seconds), id
                        val name = cursor.getString(0)
                        val type = cursor.getInt(1)
                        val uri = cursor.getString(2)
                        val source = cursor.getStringOrNull(3)
                        val active = cursor.getInt(4)
                        val interval = cursor.getLong(5)
                        val id = cursor.getLong(6)

                        database.insert("profiles",
                            SQLiteDatabase.CONFLICT_ABORT,
                            ContentValues().apply {
                                put("name", name)
                                put("type", type)
                                put("uri", uri)
                                put("source", source)
                                put("active", active)
                                put("interval", interval * 1000)
                                put("id", id)
                            })

                        cursor.moveToNext()
                    }
                }

            database.query("SELECT profile_id, proxy, selected FROM _selected_proxies")
                .use { cursor ->
                    cursor.moveToFirst()
                    while (!cursor.isAfterLast) {
                        // just copy
                        // profile_id, proxy, selected
                        val profileId = cursor.getLong(0)
                        val proxy = cursor.getString(1)
                        val selected = cursor.getString(2)

                        database.insert("selected_proxies",
                            SQLiteDatabase.CONFLICT_REPLACE,
                            ContentValues().apply {
                                put("profile_id", profileId)
                                put("proxy", proxy)
                                put("selected", selected)
                            })

                        cursor.moveToNext()
                    }
                }

            database.execSQL("DROP TABLE IF EXISTS _profiles")
            database.execSQL("DROP TABLE IF EXISTS _selected_proxies")

            val uiSp = Global.application
                .getSharedPreferences("ui", Context.MODE_PRIVATE)
            val srvSp = Global.application
                .getSharedPreferences("service", Context.MODE_PRIVATE)

            srvSp.edit {
                putBoolean("enable_vpn", uiSp.getBoolean("enable_vpn", true))
            }

            NotificationManagerCompat.from(Global.application).apply {
                deleteNotificationChannel("profile_service_status")
                deleteNotificationChannel("profile_service_result")
            }

            Log.i("Database Migrated 2 -> 3")
        } catch (e: Exception) {
            Log.e("Migration failure", e)
        }
    }
}