package com.github.kr328.clash.service.files

import android.os.ParcelFileDescriptor

interface VirtualFile {
    fun name(): String
    fun lastModified(): Long
    fun size(): Long
    fun mimeType(): String
    fun listFiles(): List<String>
    fun openFile(mode: Int): ParcelFileDescriptor
}