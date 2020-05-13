package com.github.kr328.clash.common.utils

import android.content.Context
import android.content.res.Configuration
import java.util.*

fun Context.createLanguageConfigurationContext(language: String): Context {
    if (language.isBlank()) {
        return this
    }

    val split = language.split("-")
    val locale = if (split.size == 1)
        Locale(split[0])
    else
        Locale(split[0], split[1])

    val configuration = Configuration()

    configuration.setLocale(locale)

    return createConfigurationContext(configuration)
}