package com.github.kr328.clash

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.github.kr328.clash.common.utils.intent
import com.github.kr328.clash.common.utils.asBytesString
import com.github.kr328.clash.core.model.General
import com.github.kr328.clash.remote.withClash
import com.github.kr328.clash.remote.withProfile
import com.github.kr328.clash.service.util.startClashService
import com.github.kr328.clash.service.util.stopClashService
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*

class MainActivity : BaseActivity() {
    companion object {
        private const val REQUEST_CODE = 40000
    }

    private var bandwidthJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        status.setOnClickListener {
            if (clashRunning) {
                stopClashService()
            } else {
                val vpnRequest = startClashService()
                if (vpnRequest != null) {
                    val resolved = packageManager.resolveActivity(vpnRequest, 0)
                    if (resolved != null) {
                        startActivityForResult(vpnRequest, REQUEST_CODE)
                    } else {
                        showSnackbarException(getString(R.string.missing_vpn_component), null)
                    }
                }
            }
        }

        proxies.setOnClickListener {
            startActivity(ProxiesActivity::class.intent)
        }

        profiles.setOnClickListener {
            startActivity(ProfilesActivity::class.intent)
        }

        logs.setOnClickListener {
            startActivity(LogsActivity::class.intent)
        }

        settings.setOnClickListener {
            startActivity(SettingsActivity::class.intent)
        }

        support.setOnClickListener {
            startActivity(SupportActivity::class.intent)
        }

        about.setOnClickListener {
            showAboutDialog()
        }
    }

    override fun onStart() {
        super.onStart()

        updateClashStatus()
    }

    override fun onStop() {
        super.onStop()

        stopBandwidthPolling()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK)
                startClashService()
            return
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    override suspend fun onClashStarted() {
        updateClashStatus()
    }

    override suspend fun onClashStopped(reason: String?) {
        updateClashStatus()

        if (reason != null)
            showSnackbarException(getString(R.string.clash_start_failure), reason)
    }

    override suspend fun onClashProfileLoaded() {
        updateClashStatus()
    }

    private fun startBandwidthPolling() {
        if (bandwidthJob != null)
            return

        bandwidthJob = launch {
            withClash {
                try {
                    while (clashRunning && isActive) {
                        val bandwidth = queryBandwidth()
                        status.summary = getString(
                            R.string.format_traffic_forwarded,
                            bandwidth.asBytesString()
                        )
                        delay(1000)
                    }
                } finally {
                    bandwidthJob = null
                }
            }
        }
    }

    private fun stopBandwidthPolling() {
        bandwidthJob?.cancel()
    }

    private fun updateClashStatus() {
        if (clashRunning) {
            startBandwidthPolling()

            status.setCardBackgroundColor(getColor(R.color.primaryCardColorStarted))
            status.icon = getDrawable(R.drawable.ic_started)
            status.title = getText(R.string.running)

            proxies.visibility = View.VISIBLE
        } else {
            stopBandwidthPolling()

            status.setCardBackgroundColor(getColor(R.color.primaryCardColorStopped))
            status.icon = getDrawable(R.drawable.ic_stopped)
            status.title = getText(R.string.stopped)
            status.summary = getText(R.string.tap_to_start)

            proxies.visibility = View.GONE
        }

        launch {
            val general = withClash {
                queryGeneral()
            }
            val active = withProfile {
                queryActive()
            }

            val modeResId = when (general.mode) {
                General.Mode.DIRECT -> R.string.direct_mode
                General.Mode.GLOBAL -> R.string.global_mode
                General.Mode.RULE -> R.string.rule_mode
            }

            val profileString =
                if (active == null)
                    getText(R.string.not_selected)
                else
                    getString(R.string.format_profile_activated, active.name)

            proxies.summary = getText(modeResId)
            profiles.summary = profileString
        }
    }

    private fun showAboutDialog() {
        launch {
            val content = withContext(Dispatchers.Default) {
                val packageInfo = packageManager.getPackageInfo(packageName, 0)

                LayoutInflater.from(this@MainActivity)
                    .inflate(R.layout.dialog_abort, rootView as ViewGroup?, false).apply {
                        findViewById<View>(android.R.id.icon).background =
                            getDrawable(R.drawable.ic_logo)
                        findViewById<TextView>(android.R.id.title).text =
                            getText(R.string.application_name)
                        findViewById<TextView>(android.R.id.summary).text = packageInfo.versionName
                    }
            }

            AlertDialog.Builder(this@MainActivity)
                .setView(content)
                .show()
        }
    }
}