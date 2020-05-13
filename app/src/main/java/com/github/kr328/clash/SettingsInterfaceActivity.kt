package com.github.kr328.clash

import android.os.Bundle
import com.github.kr328.clash.settings.InterfaceFragment

class SettingsInterfaceActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_fragment)
        setSupportActionBar(findViewById(R.id.toolbar))

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment, InterfaceFragment())
            .commit()
    }
}