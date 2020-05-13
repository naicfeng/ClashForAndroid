package com.github.kr328.clash

import android.os.Bundle
import com.github.kr328.clash.settings.BehaviorFragment

class SettingsBehaviorActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_fragment)
        setSupportActionBar(findViewById(R.id.toolbar))

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment, BehaviorFragment())
            .commit()
    }
}