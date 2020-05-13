package com.github.kr328.clash

import android.os.Bundle
import com.github.kr328.clash.settings.NetworkFragment
import com.google.android.material.snackbar.Snackbar

class SettingsNetworkActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_fragment)
        setSupportActionBar(findViewById(R.id.toolbar))

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment, NetworkFragment())
            .commit()

        if (clashRunning)
            Snackbar.make(rootView, R.string.options_unavailable, Snackbar.LENGTH_INDEFINITE).show()
    }

    override suspend fun onClashStopped(reason: String?) {
        recreate()
    }

    override suspend fun onClashStarted() {
        recreate()
    }
}
