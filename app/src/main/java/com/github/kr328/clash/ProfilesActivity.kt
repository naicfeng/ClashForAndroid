package com.github.kr328.clash

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kr328.clash.adapter.ProfileAdapter
import com.github.kr328.clash.common.utils.intent
import com.github.kr328.clash.remote.withProfile
import com.github.kr328.clash.service.ProfileReceiver
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.service.util.sendBroadcastSelf
import com.github.kr328.clash.weight.ProfilesMenu
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_profiles.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import java.io.FileNotFoundException
import java.util.*

class ProfilesActivity : BaseActivity(), ProfileAdapter.Callback, ProfilesMenu.Callback {
    companion object {
        private const val EDITOR_REQUEST_CODE = 30000
    }

    private var backgroundJob: Job? = null
    private val reloadMutex = Mutex()
    private val editorStack = Stack<Profile>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profiles)
        setSupportActionBar(toolbar)

        mainList.layoutManager = LinearLayoutManager(this)
        mainList.adapter = ProfileAdapter(this, this)
    }

    override fun onStart() {
        super.onStart()

        backgroundJob = launch {
            reloadProfiles()

            while (isActive) {
                delay(1000 * 60)

                // Refresh without animation
                (mainList.adapter as ProfileAdapter).notifyDataSetChanged()
            }
        }
    }

    override fun onStop() {
        super.onStop()

        backgroundJob?.cancel()
        backgroundJob = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == EDITOR_REQUEST_CODE) {
            launch {
                val profile = editorStack.pop()

                withProfile {
                    update(profile.id, profile)
                    startUpdate(profile.id)
                }
            }

            return
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    override suspend fun onClashProfileChanged() {
        reloadProfiles()
    }

    private suspend fun reloadProfiles() {
        if (!reloadMutex.tryLock())
            return

        val profiles = withProfile {
            queryAll()
        }

        (mainList.adapter as ProfileAdapter).setEntitiesAsync(profiles.toList())

        reloadMutex.unlock()
    }

    override fun onProfileClicked(entity: Profile) {
        launch {
            withProfile {
                setActive(entity.id)
            }
        }
    }

    override fun onMenuClicked(entity: Profile) {
        ProfilesMenu(this, entity, this).show()
    }

    override fun onNewProfile() {
        startActivity(CreateProfileActivity::class.intent)
    }

    private fun openProperties(id: Long) {
        startActivity(
            ProfileEditActivity::class.intent
                .setData(Uri.fromParts("id", id.toString(), null))
        )
    }

    private fun openEditor(profile: Profile) = launch {
        try {
            val uri = withProfile {
                acquireTempUri(profile.id)
            } ?: throw FileNotFoundException()

            editorStack.push(profile.copy(uri = Uri.parse(uri)))

            startActivityForResult(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(Uri.parse(uri), "text/plain")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION),
                EDITOR_REQUEST_CODE
            )
        } catch (e: Exception) {
            Snackbar.make(rootView, getText(R.string.profile_not_found), Snackbar.LENGTH_LONG)
                .show()
        }
    }

    private fun startUpdate(id: Long) {
        sendBroadcastSelf(ProfileReceiver.buildUpdateIntentForId(id))
    }

    override fun onOpenEditor(entity: Profile) {
        openEditor(entity)
    }

    override fun onUpdate(entity: Profile) {
        startUpdate(entity.id)
    }

    override fun onOpenProperties(entity: Profile) {
        openProperties(entity.id)
    }

    override fun onDuplicate(entity: Profile) {
        launch {
            withProfile {
                openProperties(acquireCloned(entity.id))
            }
        }
    }

    override fun onResetProvider(entity: Profile) {
        launch {
            withProfile {
                clear(entity.id)
            }
        }
    }

    override fun onDelete(entity: Profile) {
        launch {
            withProfile {
                delete(entity.id)
            }
        }
    }
}