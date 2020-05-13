package com.github.kr328.clash

import android.os.Bundle
import com.github.kr328.clash.common.utils.intent
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(toolbar)

        commonUi.build {
            option(
                icon = getDrawable(R.drawable.ic_settings_applications),
                title = getString(R.string.behavior)
            ) {
                paddingHeight = true

                onClick {
                    startActivity(SettingsBehaviorActivity::class.intent)
                }
            }
            option(
                icon = getDrawable(R.drawable.ic_network),
                title = getString(R.string.network)
            ) {
                paddingHeight = true

                onClick {
                    startActivity(SettingsNetworkActivity::class.intent)
                }
            }
            option(
                icon = getDrawable(R.drawable.ic_interface),
                title = getString(R.string.interface_)
            ) {
                paddingHeight = true

                onClick {
                    startActivity(SettingsInterfaceActivity::class.intent)
                }
            }
        }
    }
}