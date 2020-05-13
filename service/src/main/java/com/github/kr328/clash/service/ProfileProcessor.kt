package com.github.kr328.clash.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.URLUtil
import com.github.kr328.clash.common.utils.Log
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.service.data.ProfileDao
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.service.model.Profile.Type
import com.github.kr328.clash.service.model.asEntity
import com.github.kr328.clash.service.util.resolveBaseDir
import com.github.kr328.clash.service.util.resolveProfileFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.lang.Exception
import java.util.*

object ProfileProcessor {
    suspend fun createOrUpdate(context: Context, metadata: Profile) =
        withContext(Dispatchers.IO) {
            metadata.enforceFieldValid()

            context.resolveBaseDir(metadata.id).mkdirs()
            context.resolveProfileFile(metadata.id).parentFile?.mkdirs()

            downloadProfile(
                context, metadata.uri,
                context.resolveProfileFile(metadata.id),
                context.resolveBaseDir(metadata.id)
            )

            val entity = if (metadata.type == Type.FILE)
                metadata.copy(
                    uri = ProfileProvider.resolveUri(
                        context,
                        context.resolveProfileFile(metadata.id)
                    )
                ).asEntity()
            else
                metadata.asEntity()

            if (ProfileDao.queryById(metadata.id) == null)
                ProfileDao.insert(entity)
            else
                ProfileDao.update(entity)

            ProfileReceiver.requestNextUpdate(context, metadata.id)
        }

    private suspend fun downloadProfile(
        context: Context,
        source: Uri,
        target: File,
        baseDir: File
    ) = withContext(Dispatchers.IO) {
        when (source.scheme?.toLowerCase(Locale.getDefault())) {
            "http", "https" ->
                Clash.downloadProfile(source.toString(), target, baseDir)
            "content", "file", "resource" -> {
                val fd = context.contentResolver.openFileDescriptor(source, "r")
                    ?: throw FileNotFoundException("$source not found")
                Clash.downloadProfile(fd.detachFd(), target, baseDir)
            }
            else -> throw IllegalArgumentException("Invalid uri type")
        }.await()
    }

    private fun Profile.enforceFieldValid() {
        when {
            id < 0 ->
                throw IllegalArgumentException("Invalid id")
            name.isBlank() ->
                throw IllegalArgumentException("Empty name")
            type != Type.FILE && type != Type.URL && type != Type.EXTERNAL ->
                throw IllegalArgumentException("Invalid type")
            !URLUtil.isValidUrl(uri.toString()) ->
                throw IllegalArgumentException("Invalid uri")
            source?.isValidIntent() == false ->
                throw IllegalArgumentException("Invalid source")
            interval < 0 ->
                throw IllegalArgumentException("Invalid interval")
        }
    }

    private fun String.isValidIntent(): Boolean {
        return try {
            Intent.parseUri(this, 0)
            true
        } catch (e: Exception) {
            false
        }
    }
}