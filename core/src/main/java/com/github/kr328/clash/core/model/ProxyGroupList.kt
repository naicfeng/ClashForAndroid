package com.github.kr328.clash.core.model

import android.os.Parcel
import android.os.Parcelable
import com.github.kr328.clash.common.serialization.MergedParcels
import kotlinx.serialization.Serializable

@Serializable
data class ProxyGroupList(val list: List<ProxyGroup>) : Parcelable {
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        MergedParcels.dump(serializer(), this, parcel)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ProxyGroupList> {
        override fun createFromParcel(parcel: Parcel): ProxyGroupList {
            return MergedParcels.load(serializer(), parcel)
        }

        override fun newArray(size: Int): Array<ProxyGroupList?> {
            return arrayOfNulls(size)
        }
    }
}