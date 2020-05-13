package com.github.kr328.clash

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import androidx.appcompat.app.AlertDialog
import com.github.kr328.clash.dump.LogcatDumper
import com.google.android.material.snackbar.Snackbar
import com.microsoft.appcenter.crashes.Crashes
import com.microsoft.appcenter.crashes.ingestion.models.ErrorAttachmentLog
import kotlinx.android.synthetic.main.activity_support.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SupportActivity : BaseActivity() {
    class UserRequestTrackException: Exception()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_support)
        setSupportActionBar(toolbar)

        commonUi.build {
            tips {
                icon = getDrawable(R.drawable.ic_info)
                title = Html.fromHtml(getString(R.string.tips_support), Html.FROM_HTML_MODE_LEGACY)
            }

            category(text = getString(R.string.sources))

            option(
                title = getString(R.string.clash),
                summary = getString(R.string.clash_url)
            ) {
                onClick {
                    startActivity(
                        Intent(Intent.ACTION_VIEW)
                            .setData(Uri.parse(getString(R.string.clash_url)))
                    )
                }
            }
            option(
                title = getString(R.string.clashr),
                summary = getString(R.string.clashr_url)
            ) {
                onClick {
                    startActivity(
                        Intent(Intent.ACTION_VIEW)
                            .setData(Uri.parse(getString(R.string.clashr_url)))
                    )
                }
            }
            option(
                title = getString(R.string.clash_for_android),
                summary = getString(R.string.clash_for_android_url)
            ) {
                onClick {
                    startActivity(
                        Intent(Intent.ACTION_VIEW)
                            .setData(Uri.parse(getString(R.string.clash_for_android_url)))
                    )
                }
            }
            option(
                title = getString(R.string.clashr_for_android),
                summary = getString(R.string.clashr_for_android_url)
            ) {
                onClick {
                    startActivity(
                        Intent(Intent.ACTION_VIEW)
                            .setData(Uri.parse(getString(R.string.clashr_for_android_url)))
                    )
                }
            }

            category(text = getString(R.string.feedback))

            option(
                title = getString(R.string.upload_logcat),
                summary = getString(R.string.upload_logcat_summary)
            ) {
                onClick {
                    AlertDialog.Builder(this@SupportActivity)
                        .setTitle(R.string.upload_logcat)
                        .setMessage(R.string.upload_logcat_warn)
                        .setNegativeButton(R.string.cancel) {_, _ -> }
                        .setPositiveButton(R.string.ok) {_, _ -> upload() }
                        .show()
                }
            }

            option(
                title = getString(R.string.github_issues),
                summary = getString(R.string.github_issues_url)
            ) {
                onClick {
                    startActivity(
                        Intent(Intent.ACTION_VIEW)
                            .setData(Uri.parse(getString(R.string.github_issues_url)))
                    )
                }
            }

            val firstLanguage = resources.configuration.locales.get(0).language

            if (firstLanguage.equals("zh", true)) {
                category(getString(R.string.donate))

                option(
                    title = getString(R.string.telegram_channel),
                    summary = getString(R.string.telegram_channel_url)
                ) {
                    onClick {
                        startActivity(
                            Intent(Intent.ACTION_VIEW)
                                .setData(Uri.parse(getString(R.string.telegram_channel_url)))
                        )
                    }
                }
            }

            option(
                title = getString(R.string.mod_clashr_text),
                summary = getString(R.string.mod_clashr_url)
            ) {
                onClick {
                    startActivity(
                        Intent(Intent.ACTION_VIEW)
                            .setData(Uri.parse(getString(R.string.mod_clashr_url)))
                    )
                }
            }
        }
    }

    private fun upload() {
        launch {
            withContext(Dispatchers.IO) {
                val attachment = ErrorAttachmentLog
                    .attachmentWithText(LogcatDumper.dumpAll(), "logcat.txt")

                Crashes.trackError(UserRequestTrackException(), null, listOf(attachment))
            }

            withContext(Dispatchers.Main) {
                Snackbar.make(rootView, R.string.uploaded, Snackbar.LENGTH_LONG).show()
            }
        }
    }
}