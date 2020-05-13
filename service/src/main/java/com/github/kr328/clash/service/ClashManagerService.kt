package com.github.kr328.clash.service

import android.content.Intent
import android.os.IBinder

class ClashManagerService : BaseService() {
    override fun onBind(intent: Intent?): IBinder? {
        return ClashManager(this)
    }
}