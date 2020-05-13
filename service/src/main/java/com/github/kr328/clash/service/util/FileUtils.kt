package com.github.kr328.clash.service.util

import android.content.Context
import com.github.kr328.clash.service.Constants
import java.io.File

fun Context.resolveProfileFile(id: Long): File {
    return filesDir.resolve(Constants.PROFILES_DIR).resolve("$id.yaml")
}

fun Context.resolveBaseDir(id: Long): File {
    return filesDir.resolve(Constants.CLASH_DIR).resolve(id.toString())
}

fun Context.resolveTempProfileFile(id: Long): File {
    return cacheDir.resolve(Constants.PROFILES_DIR).resolve("$id.yaml")
}