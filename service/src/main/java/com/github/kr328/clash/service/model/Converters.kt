package com.github.kr328.clash.service.model

import android.content.Context
import android.net.Uri
import com.github.kr328.clash.service.data.ProfileEntity
import com.github.kr328.clash.service.util.resolveProfileFile

fun ProfileEntity.asProfile(context: Context): Profile {
    val type = when (this.type) {
        ProfileEntity.TYPE_FILE -> Profile.Type.FILE
        ProfileEntity.TYPE_URL -> Profile.Type.URL
        ProfileEntity.TYPE_EXTERNAL -> Profile.Type.EXTERNAL
        else -> Profile.Type.UNKNOWN
    }
    val lastModified = context.resolveProfileFile(id).lastModified()

    return Profile(
        id = id,
        name = name,
        type = type,
        uri = Uri.parse(uri),
        source = source,
        active = active,
        interval = interval,
        lastModified = lastModified
    )
}

fun Profile.asEntity(): ProfileEntity {
    val type = when (this.type) {
        Profile.Type.FILE -> ProfileEntity.TYPE_FILE
        Profile.Type.URL -> ProfileEntity.TYPE_URL
        Profile.Type.EXTERNAL -> ProfileEntity.TYPE_EXTERNAL
        Profile.Type.UNKNOWN -> ProfileEntity.TYPE_UNKNOWN
    }

    return ProfileEntity(
        name = name,
        type = type,
        uri = uri.toString(),
        source = source,
        active = active,
        interval = interval,
        id = id
    )
}