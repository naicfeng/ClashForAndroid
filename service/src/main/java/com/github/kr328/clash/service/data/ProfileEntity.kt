package com.github.kr328.clash.service.data

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "profiles", primaryKeys = ["id"])
@Keep
data class ProfileEntity(
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "type") val type: Int,
    @ColumnInfo(name = "uri") val uri: String,
    @ColumnInfo(name = "source") val source: String?,
    @ColumnInfo(name = "active") val active: Boolean,
    @ColumnInfo(name = "interval") val interval: Long,
    @ColumnInfo(name = "id") val id: Long
) {
    companion object {
        const val TYPE_FILE = 1
        const val TYPE_URL = 2
        const val TYPE_EXTERNAL = 3
        const val TYPE_UNKNOWN = -1
    }
}