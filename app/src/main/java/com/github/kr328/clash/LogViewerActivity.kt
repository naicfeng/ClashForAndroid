package com.github.kr328.clash

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import androidx.core.net.toFile
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kr328.clash.adapter.LiveLogAdapter
import com.github.kr328.clash.adapter.LogAdapter
import com.github.kr328.clash.common.utils.intent
import com.github.kr328.clash.core.event.LogEvent
import kotlinx.android.synthetic.main.activity_log_viewer.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import java.io.File

class LogViewerActivity : BaseActivity() {
    private val pauseMutex = Mutex()
    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            finish()
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val logcat =
                requireNotNull(service?.queryLocalInterface(LogcatService::class.java.name)) as LogcatService

            startLogcatPoll(logcat)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_log_viewer)

        setSupportActionBar(toolbar)

        val file = intent?.data

        if (file == null)
            startLiveMode()
        else
            startFileMode(file.toFile())
    }

    override fun onStop() {
        super.onStop()

        launch {
            pauseMutex.lock()
        }
    }

    override fun onStart() {
        super.onStart()

        launch {
            if (pauseMutex.isLocked)
                pauseMutex.unlock()
        }
    }

    private fun startLiveMode() {
        mainList.layoutManager = LinearLayoutManager(this)
        mainList.adapter = LiveLogAdapter(this)
        mainList.itemAnimator?.addDuration = 100
        mainList.itemAnimator?.removeDuration = 100

        stop.setOnClickListener {
            unbindService(connection)
            stopService(LogcatService::class.intent)
            finish()
        }

        bindService(LogcatService::class.intent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun startFileMode(file: File) {
        stop.visibility = View.GONE

        launch {
            val items = withContext(Dispatchers.IO) {
                try {
                    file.bufferedReader().useLines { lines ->
                        lines
                            .map { it.trim() }
                            .filter { it.isNotEmpty() && !it.startsWith("#") }
                            .map { it.split(" ", limit = 3) }
                            .filter { it.size == 3 }
                            .map { LogEvent(LogEvent.Level.valueOf(it[1]), it[2], it[0].toLong()) }
                            .toList()
                    }
                } catch (e: Exception) {
                    showSnackbarException(getString(R.string.open_log_failure), e.message)

                    throw CancellationException()
                }
            }

            mainList.layoutManager = LinearLayoutManager(this@LogViewerActivity)
            mainList.adapter = LogAdapter(this@LogViewerActivity, items)
            mainList.adapter!!.notifyItemRangeInserted(0, items.size)
        }
    }

    private fun startLogcatPoll(service: LogcatService) {
        launch {
            var offset = 0L

            while (isActive) {
                pauseMutex.lock()

                val response = service.pollLogEvent(offset).await()

                (mainList.adapter as LiveLogAdapter).insertItems(response.logs)

                mainList.apply {
                    if (computeVerticalScrollOffset() < 30)
                        scrollToPosition(0)
                }

                offset = response.offset + response.logs.size

                pauseMutex.unlock()

                delay(200)
            }
        }
    }
}