package com.github.kr328.clash.dump

object LogcatDumper {
    fun dumpCrash(): String {
        return try {
            val process =
                Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-s", "Go", "AndroidRuntime", "DEBUG"))

            val result = process.inputStream.use {
                it.reader().readText()
            }

            process.waitFor()

            result
        } catch (e: Exception) {
            ""
        }
    }

    fun dumpAll(): String {
        return try {
            val process =
                Runtime.getRuntime().exec(arrayOf("logcat", "-d"))

            val result = process.inputStream.use {
                it.reader().readText()
            }

            process.waitFor()

            result
        } catch (e: Exception) {
            ""
        }
    }
}
