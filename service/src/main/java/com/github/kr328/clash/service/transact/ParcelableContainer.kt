package com.github.kr328.clash.service.transact

import android.os.Parcel
import android.os.Parcelable

data class ParcelableContainer(val data: Parcelable?) : Parcelable {
    constructor(parcel: Parcel) :
            this(parcel.readParcelable<Parcelable>(ParcelableContainer::class.java.classLoader))

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(data, 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ParcelableContainer> {
        override fun createFromParcel(parcel: Parcel): ParcelableContainer {
            return ParcelableContainer(parcel)
        }

        override fun newArray(size: Int): Array<ParcelableContainer?> {
            return arrayOfNulls(size)
        }
    }

}