package com.github.kr328.clash.common.utils

object ByteFormatter {
    fun byteToString(bytes: Long): String {
        return when {
            bytes > 1024 * 1024 * 1024 ->
                String.format("%.2f GiB", (bytes.toDouble() / 1024 / 1024 / 1024))
            bytes > 1024 * 1024 ->
                String.format("%.2f MiB", (bytes.toDouble() / 1024 / 1024))
            bytes > 1024 ->
                String.format("%.2f KiB", (bytes.toDouble() / 1024))
            else ->
                "$bytes Bytes"
        }
    }

    fun byteToStringSecond(bytes: Long): String {
        return byteToString(bytes) + "/s"
    }
}

fun Long.asBytesString(): String {
    return ByteFormatter.byteToString(this)
}

fun Long.asSpeedString(): String {
    return ByteFormatter.byteToStringSecond(this)
}