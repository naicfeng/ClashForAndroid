package com.github.kr328.clash.common

import android.app.Application
import android.content.Intent

object Global {
    var openMainIntent: () -> Intent = { Intent() }
    var openProfileIntent: (Long) -> Intent = { Intent() }

    lateinit var application: Application
        private set

    fun init(application: Application) {
        Global.application = application
    }
}