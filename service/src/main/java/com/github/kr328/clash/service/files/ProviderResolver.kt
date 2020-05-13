package com.github.kr328.clash.service.files

import android.content.Context
import android.os.ParcelFileDescriptor
import com.github.kr328.clash.service.util.resolveBaseDir
import java.io.FileNotFoundException
import java.net.URLDecoder

class ProviderResolver(private val context: Context) {
    fun resolve(id: Long, fileName: String): VirtualFile {
        val file = context.resolveBaseDir(id).resolve(fileName)
        if (!file.exists())
            throw FileNotFoundException()

        return object : VirtualFile {
            override fun name(): String {
                return URLDecoder.decode(file.name, "utf-8")
            }

            override fun lastModified(): Long {
                return file.lastModified()
            }

            override fun size(): Long {
                return file.length()
            }

            override fun mimeType(): String {
                return "text/plain"
            }

            override fun listFiles(): List<String> {
                return emptyList()
            }

            override fun openFile(mode: Int): ParcelFileDescriptor {
                return ParcelFileDescriptor.open(file, mode)
            }
        }
    }
}