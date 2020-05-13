package com.github.kr328.clash

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import kotlinx.android.synthetic.main.activity_application_broken.*

class ApkBrokenActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_application_broken)
        setSupportActionBar(toolbar)

        text.text = Html.fromHtml(
            getString(R.string.application_broken_description),
            Html.FROM_HTML_MODE_COMPACT
        )

        commonUi.build {
            option(
                icon = getDrawable(R.drawable.ic_info),
                title = getString(R.string.learn_more_about_split_apks)
            ) {
                onClick {
                    startActivity(
                        Intent(Intent.ACTION_VIEW)
                            .setData(Uri.parse(getString(R.string.about_split_apks_url)))
                    )
                }
            }
            option(
                icon = getDrawable(R.drawable.ic_input),
                title = getString(R.string.reinstall_from_google_play)
            ) {
                onClick {
                    startActivity(
                        Intent(Intent.ACTION_VIEW)
                            .setData(Uri.parse(getString(R.string.google_play_url)))
                    )
                }
            }
            option(
                icon = getDrawable(R.drawable.ic_play_for_work),
                title = getString(R.string.download_from_github_releases)
            ) {
                onClick {
                    startActivity(
                        Intent(Intent.ACTION_VIEW)
                            .setData(Uri.parse(getString(R.string.github_releases_url)))
                    )
                }
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()

        finishAffinity()
        finish()
    }

    override fun shouldDisplayHomeAsUpEnabled(): Boolean {
        return false
    }
}