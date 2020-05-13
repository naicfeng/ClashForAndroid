package com.github.kr328.clash.fragment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import androidx.fragment.app.Fragment
import com.github.kr328.clash.Constants
import com.github.kr328.clash.R
import com.github.kr328.clash.design.common.TextInput
import com.github.kr328.clash.design.view.CommonUiLayout
import com.github.kr328.clash.service.model.Profile.Type
import com.google.android.material.snackbar.Snackbar

class ProfileEditFragment(
    val id: Long,
    var name: String,
    var uri: Uri,
    var interval: Long,
    private val type: Type,
    private val source: String?
) : Fragment() {
    private var root: CommonUiLayout? = null

    var isModified = false

    companion object {
        private const val REQUEST_CODE = 10000

        private const val KEY_NAME = "name"
        private const val KEY_URL = "url"
        private const val KEY_AUTO_UPDATE = "auto_update"

        private val TYPE_YAML = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension("yaml") ?: "*/*"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return CommonUiLayout(requireContext()).apply {
            root = this
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

            build {
                tips(icon = requireContext().getDrawable(R.drawable.ic_info)) {
                    title =
                        Html.fromHtml(getString(R.string.tips_profile), Html.FROM_HTML_MODE_LEGACY)
                }

                textInput(
                    title = getString(R.string.name),
                    icon = requireContext().getDrawable(R.drawable.ic_label_outline),
                    hint = getString(R.string.profile_name),
                    id = KEY_NAME,
                    content = name
                ) {
                    onTextChanged {
                        name = content.toString()
                        isModified = true
                    }
                }
                textInput(
                    title = getString(R.string.url),
                    icon = requireContext().getDrawable(R.drawable.ic_content),
                    hint = getString(R.string.profile_url),
                    id = KEY_URL,
                    content = uri.toString()
                ) {
                    onOpenInput {
                        if (!openUrlProvider())
                            openDialogInput()
                    }
                    onDisplayContent {
                        it.split("/").last()
                    }
                    onTextChanged {
                        if (!URLUtil.isValidUrl(content.toString())) {
                            content = ""
                            Snackbar.make(view, R.string.invalid_url, Snackbar.LENGTH_LONG).show()
                            return@onTextChanged
                        }

                        uri = Uri.parse(content.toString())
                        isModified = true
                    }
                }
                textInput(
                    title = getString(R.string.auto_update),
                    icon = requireContext().getDrawable(R.drawable.ic_update),
                    hint = getString(R.string.more_than_15_minutes),
                    id = KEY_AUTO_UPDATE,
                    content = (interval / 1000 / 60).toStringIfNonZero()
                ) {
                    onDisplayContent {
                        val interval = it.toString().toIntOrNull() ?: 0

                        if (interval <= 0)
                            getString(R.string.disabled)
                        else
                            getString(R.string.format_minutes, interval)
                    }
                    onTextChanged {
                        val s = it.toString()

                        if (s.isBlank()) {
                            content = ""
                            interval = 0
                            return@onTextChanged
                        }

                        val value = s.toIntOrNull()
                        if (value == null || value < 15) {
                            content = ""
                            interval = 0
                            Snackbar.make(view, R.string.invalid_interval, Snackbar.LENGTH_LONG)
                                .show()
                            return@onTextChanged
                        }

                        interval = content.toString().toLong() * 1000 * 60
                        isModified = true
                    }

                    if ( type == Type.FILE )
                        isHidden = true
                }

                screen.restoreState(savedInstanceState)

                if (type == Type.EXTERNAL)
                    openUrlProvider()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode != Activity.RESULT_OK || data == null)
                return

            root?.apply {
                data.data?.apply {
                    screen.requireElement<TextInput>(KEY_URL).content = this.toString()
                }

                data.getStringExtra(Constants.URL_PROVIDER_INTENT_EXTRA_NAME)?.also {
                    screen.requireElement<TextInput>(KEY_NAME).apply {
                        if (content.isBlank())
                            content = it
                    }
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        root?.apply {
            screen.saveState(outState)
        }
    }

    private fun openUrlProvider(): Boolean {
        try {
            when (type) {
                Type.FILE ->
                    startActivityForResult(
                        Intent(Intent.ACTION_GET_CONTENT).setType(TYPE_YAML),
                        REQUEST_CODE
                    )
                Type.EXTERNAL ->
                    startActivityForResult(
                        source?.toIntent() ?: return false,
                        REQUEST_CODE
                    )
                else -> return false
            }
        } catch (e: Exception) {
            root?.apply {
                Snackbar.make(
                    this,
                    R.string.start_url_provider_failure,
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }

        return true
    }

    private fun Long.toStringIfNonZero(): String {
        return if ( this == 0L ) "" else this.toString()
    }

    private fun String.toIntent(): Intent? {
        return try {
            Intent.parseUri(this, 0)
        } catch (e: Exception) {
            null
        }
    }
}