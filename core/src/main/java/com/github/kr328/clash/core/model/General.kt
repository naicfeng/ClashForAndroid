package com.github.kr328.clash.core.model

import android.os.Parcel
import android.os.Parcelable
import com.github.kr328.clash.common.serialization.Parcels
import kotlinx.serialization.*

@Serializable
data class General(val mode: Mode, val http: Int, val socks: Int, val redirect: Int) : Parcelable {
    @Serializable
    enum class Mode(val string: String) {
        DIRECT("Direct"), GLOBAL("Global"), RULE("Rule");

        override fun toString(): String {
            return string
        }

        companion object {
            fun fromString(mode: String): Mode {
                return when (mode) {
                    DIRECT.string -> DIRECT
                    GLOBAL.string -> GLOBAL
                    RULE.string -> RULE
                    else -> throw IllegalArgumentException("Invalid mode $mode")
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

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<General> {
            override fun createFromParcel(parcel: Parcel): General {
                return Parcels.load(serializer(), parcel)
            }

            override fun newArray(size: Int): Array<General?> {
                return arrayOfNulls(size)
            }
        }
    }
}