package com.github.kr328.clash.service.data.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.core.database.getStringOrNull
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.github.kr328.clash.common.utils.Log
import com.github.kr328.clash.service.data.ProfileEntity
import com.github.kr328.clash.service.data.SelectedProxyEntity

object Migration34: Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        try {
            val profiles = mutableListOf<ProfileEntity>()
            try {
                database.query("SELECT name, type, uri, source, active, interval, id FROM profiles")
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

                            profiles.add(ProfileEntity(name, type, uri, source, active != 0, interval, id))

                            cursor.moveToNext()
                        }
                    }
            }
            catch (e: Exception) {
                Log.w("Query old data failure", e)
            }

            val selectedProxies = mutableListOf<SelectedProxyEntity>()

            try {
                database.query("SELECT profile_id, proxy, selected FROM selected_proxies")
                    .use { cursor ->
                        cursor.moveToFirst()
                        while (!cursor.isAfterLast) {
                            // just copy
                            // profile_id, proxy, selected
                            val profileId = cursor.getLong(0)
                            val proxy = cursor.getString(1)
                            val selected = cursor.getString(2)

                            selectedProxies.add(SelectedProxyEntity(profileId, proxy, selected))

                            cursor.moveToNext()
                        }
                    }
            }
            catch (e: Exception) {
                Log.w("Query old data failure", e)
            }

            // Clean up database
            runCatching {
                database.execSQL("DROP TABLE IF EXISTS profile_select_proxies")
                database.execSQL("DROP TABLE IF EXISTS selected_proxies")
                database.execSQL("DROP TABLE IF EXISTS profiles")
                database.execSQL("DROP TABLE IF EXISTS _profile_select_proxies")
                database.execSQL("DROP TABLE IF EXISTS _selected_proxies")
                database.execSQL("DROP TABLE IF EXISTS _profiles")
            }
            runCatching {
                database.execSQL("DROP TABLE IF EXISTS profile_select_proxies")
                database.execSQL("DROP TABLE IF EXISTS selected_proxies")
                database.execSQL("DROP TABLE IF EXISTS profiles")
                database.execSQL("DROP TABLE IF EXISTS _profile_select_proxies")
                database.execSQL("DROP TABLE IF EXISTS _selected_proxies")
                database.execSQL("DROP TABLE IF EXISTS _profiles")
            }

            database.execSQL("CREATE TABLE IF NOT EXISTS `profiles` (`name` TEXT NOT NULL, `type` INTEGER NOT NULL, `uri` TEXT NOT NULL, `source` TEXT, `active` INTEGER NOT NULL, `interval` INTEGER NOT NULL, `id` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `selected_proxies` (`profile_id` INTEGER NOT NULL, `proxy` TEXT NOT NULL, `selected` TEXT NOT NULL, PRIMARY KEY(`profile_id`, `proxy`), FOREIGN KEY(`profile_id`) REFERENCES `profiles`(`id`) ON UPDATE CASCADE ON DELETE CASCADE )")

            profiles.forEach {
                database.insert("profiles", SQLiteDatabase.CONFLICT_ABORT, ContentValues().apply {
                    put("name", it.name)
                    put("type", it.type)
                    put("uri", it.uri)
                    put("source", it.source)
                    put("active", it.active)
                    put("interval", it.interval)
                    put("id", it.id)
                })
            }

            selectedProxies.forEach {
                database.insert("selected_proxies",
                    SQLiteDatabase.CONFLICT_REPLACE, ContentValues().apply {
                    put("profile_id", it.profileId)
                    put("proxy", it.proxy)
                    put("selected", it.selected)
                })
            }

            Log.i("Database Migrated 3 -> 4")
        } catch (e: Exception) {
            Log.e("Migration failure", e)
        }
    }
}