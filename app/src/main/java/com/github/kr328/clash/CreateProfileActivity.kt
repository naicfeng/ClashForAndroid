package com.github.kr328.clash

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.github.kr328.clash.common.utils.intent
import com.github.kr328.clash.remote.withProfile
import com.github.kr328.clash.service.model.Profile.Type
import kotlinx.android.synthetic.main.activity_create_profile.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateProfileActivity : BaseActivity() {
    companion object {
        const val REQUEST_CODE = 20000
    }

    private val self = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_create_profile)

        setSupportActionBar(toolbar)

        launch {
            val providers = queryUrlProviders()

            mainList.adapter = Adapter(this@CreateProfileActivity, providers)
            mainList.divider = null
            mainList.dividerHeight = 0

            mainList.setOnItemClickListener { _, _, position, _ ->
                val item = providers[position]

                self.launch {
                    val id = withProfile {
                        acquireUnused(item.type, item.intent?.toUri(0))
                    }

                    startActivityForResult(
                        ProfileEditActivity::class.intent.setData(
                            Uri.fromParts(
                                "id",
                                id.toString(),
                                null
                            )
                        ),
                        REQUEST_CODE
                    )
                }
            }
            mainList.setOnItemLongClickListener { _, _, position, _ ->
                val item = providers[position]
                val packageName = item.intent?.component?.packageName

                if (packageName != null) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.fromParts("package", packageName, null))

                    startActivity(intent)

                    true
                } else {
                    false
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK)
            return finish()

        super.onActivityResult(requestCode, resultCode, data)
    }

    private suspend fun queryUrlProviders(): List<UrlProvider> =
        withContext(Dispatchers.IO) {
            val common = listOf(
                UrlProvider(
                    getText(R.string.file),
                    getText(R.string.import_from_file),
                    getDrawable(R.drawable.ic_file)!!,
                    Type.FILE,
                    null
                ),
                UrlProvider(
                    getText(R.string.url),
                    getText(R.string.import_from_url),
                    getDrawable(R.drawable.ic_download)!!,
                    Type.URL,
                    null
                )
            )

            val providers = packageManager.queryIntentActivities(
                Intent(Constants.URL_PROVIDER_INTENT_ACTION),
                0
            ).map {
                val activity = it.activityInfo

                val name = activity.applicationInfo.loadLabel(packageManager)
                val summary = activity.loadLabel(packageManager)
                val icon = activity.loadIcon(packageManager)
                val type = Type.EXTERNAL
                val intent = Intent(Constants.URL_PROVIDER_INTENT_ACTION)
                    .setComponent(
                        ComponentName.createRelative(
                            activity.packageName,
                            activity.name
                        )
                    )

                UrlProvider(name, summary, icon, type, intent)
            }

            common + providers
        }

    private data class UrlProvider(
        val name: CharSequence,
        val summary: CharSequence,
        val icon: Drawable,
        val type: Type,
        val intent: Intent?
    )

    private class Adapter(private val context: Context, private val providers: List<UrlProvider>) :
        BaseAdapter() {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val provider = providers[position]
            val view = convertView ?: LayoutInflater.from(context).inflate(
                R.layout.adapter_url_provider,
                parent,
                false
            )

            view.findViewById<TextView>(android.R.id.title).text = provider.name
            view.findViewById<TextView>(android.R.id.summary).text = provider.summary
            view.findViewById<View>(android.R.id.icon).background = provider.icon

            return view
        }

        override fun getItem(position: Int): Any = providers[position]
        override fun getItemId(position: Int): Long = providers[position].hashCode().toLong()
        override fun getCount(): Int = providers.size
    }
}