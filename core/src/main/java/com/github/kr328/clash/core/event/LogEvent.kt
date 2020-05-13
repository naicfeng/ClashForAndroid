package com.github.kr328.clash.core.event

import android.os.Parcel
import android.os.Parcelable
import com.github.kr328.clash.common.serialization.Parcels
import kotlinx.serialization.Serializable

@Serializable
@Suppress("UNUSED")
data class LogEvent(
    val level: Level,
    val message: String,
    val time: Long = System.currentTimeMillis()
) : Parcelable {
    companion object {
        const val DEBUG_VALUE = "debug"
        const val INFO_VALUE = "info"
        const val WARN_VALUE = "warning"
        const val ERROR_VALUE = "error"

        @JvmField
        val CREATOR = object : Parcelable.Creator<LogEvent> {
            override fun createFromParcel(parcel: Parcel): LogEvent {
                return Parcels.load(serializer(), parcel)
            }

            override fun newArray(size: Int): Array<LogEvent?> {
                return arrayOfNulls(size)
            }
        }
    }

    enum class Level {
        DEBUG,
        INFO,
        WARN,
        ERROR,
        UNKNOWN;

        companion object {
            fun fromString(type: String): Level {
                return when (type) {
                    DEBUG_VALUE -> DEBUG
                    INFO_VALUE -> INFO
                    WARN_VALUE -> WARN
                    ERROR_VALUE -> ERROR
                    else -> UNKNOWN
                }
            }
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        Parcels.dump(serializer(), this, parcel)
    }

    override fun describeContents(): Int {
        return 0
    }
}