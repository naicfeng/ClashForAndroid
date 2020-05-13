package com.github.kr328.clash.service

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

class ProfileProvider : FileProvider() {
    companion object {
        fun resolveUri(context: Context, file: File): Uri {
            return getUriForFile(
                context,
                context.packageName + Constants.PROFILE_PROVIDER_SUFFIX,
                file
            )
        }
    }
}