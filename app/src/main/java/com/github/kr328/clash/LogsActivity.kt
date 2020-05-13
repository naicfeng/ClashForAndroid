package com.github.kr328.clash

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kr328.clash.adapter.LogFileAdapter
import com.github.kr328.clash.common.utils.intent
import com.github.kr328.clash.common.utils.startForegroundServiceCompat
import com.github.kr328.clash.core.event.LogEvent
import com.github.kr328.clash.design.common.Category
import com.github.kr328.clash.design.view.CommonUiLayout
import com.github.kr328.clash.model.LogFile
import com.github.kr328.clash.utils.format
import com.github.kr328.clash.utils.logsDir
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_logs.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class LogsActivity : BaseActivity() {
    companion object {
        const val REQUEST_CODE = 50000

        private val LOG_EXPORT_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
        private val LOG_EXPORT_TIME_FORMAT = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)
    }

    private var lastWriteFile: LogFile? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logs)

        setSupportActionBar(toolbar)

        if (LogcatService.isServiceRunning) {
            startActivity(LogViewerActivity::class.intent)
            finish()
            return
        }

        commonUi.build {
            option(
                title = getString(R.string.clash_logcat),
                summary = getString(R.string.tap_to_start),
                icon = getDrawable(R.drawable.ic_adb)
            ) {
                onClick {
                    startForegroundServiceCompat(LogcatService::class.intent)

                    startActivity(LogViewerActivity::class.intent)

                    finish()
                }
            }
            category(text = getString(R.string.history), id = "history", showTopSeparator = true)
        }

        clearAll.setOnClickListener {
            showClearAllDialog()
        }

        val adapter = LogFileAdapter(
            this@LogsActivity,
            onItemClicked = {
                startActivity(
                    LogViewerActivity::class.intent
                        .setData(Uri.fromFile(logsDir.resolve(it.fileName)))
                )
            },
            onMenuClicked = this::showMenu
        )
        val layoutManager = LinearLayoutManager(this@LogsActivity)

        mainList.layoutManager = layoutManager
        mainList.adapter = adapter
    }

    override fun onStart() {
        super.onStart()

        if (LogcatService.isServiceRunning)
            return

        refreshList()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                val url = data?.data ?: return
                val file = lastWriteFile ?: return

                lastWriteFile = null

                launch {
                    withContext(Dispatchers.IO) {
                        contentResolver.openOutputStream(url)?.bufferedWriter()?.use { output ->
                            output.write("# Logcat on " + LOG_EXPORT_DATE_FORMAT.format(Date(file.date)) + "\n")

                            logsDir.resolve(file.fileName).bufferedReader().useLines { lines ->
                                lines.map { it.trim() }
                                    .filter { it.isNotEmpty() && !it.startsWith("#") }
                                    .map { it.split(" ", limit = 3) }
                                    .filter { it.size == 3 }
                                    .map {
                                        LogEvent(
                                            LogEvent.Level.valueOf(it[1]),
                                            it[2],
                                            it[0].toLong()
                                        )
                                    }
                                    .forEach {
                                        output.write(
                                            String.format(
                                                "%s |%s| %s\n",
                                                LOG_EXPORT_TIME_FORMAT.format(Date(it.time)),
                                                it.level.toString(),
                                                it.message
                                            )
                                        )
                                    }
                            }
                        }
                    }

                    Snackbar.make(rootView, R.string.file_exported, Snackbar.LENGTH_LONG).show()
                }
            }
            return
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun refreshList() {
        launch {
            val files = withContext(Dispatchers.IO) {
                (logsDir.listFiles() ?: emptyArray())
                    .asSequence()
                    .filter { it.name.endsWith(".log") }
                    .map { LogFile.parseFromFileName(it.name) }
                    .filterNotNull()
                    .toList()
            }

            if (files.isEmpty())
                commonUi.screen.requireElement<Category>("history").isHidden = true

            val adapter = mainList.adapter as LogFileAdapter
            val old = adapter.fileList

            val result = withContext(Dispatchers.Default) {
                DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                    override fun areItemsTheSame(
                        oldItemPosition: Int,
                        newItemPosition: Int
                    ): Boolean {
                        return old[oldItemPosition].fileName == files[newItemPosition].fileName
                    }

                    override fun getOldListSize(): Int {
                        return old.size
                    }

                    override fun getNewListSize(): Int {
                        return files.size
                    }

                    override fun areContentsTheSame(
                        oldItemPosition: Int,
                        newItemPosition: Int
                    ): Boolean {
                        return old[oldItemPosition] == files[newItemPosition]
                    }
                })
            }

            adapter.fileList = files
            result.dispatchUpdatesTo(adapter)
        }
    }

    private fun showClearAllDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_all_logs)
            .setMessage(R.string.delete_all_logs_warn)
            .setPositiveButton(R.string.ok) { _, _ -> deleteAllLogs() }
            .setNegativeButton(R.string.cancel) { _, _ -> }
            .show()
    }

    private fun showMenu(logFile: LogFile) {
        val dialog = BottomSheetDialog(this)
        val menu = CommonUiLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        @ColorInt
        val errorColor = TypedValue().run {
            theme.resolveAttribute(R.attr.colorError, this, true)
            data
        }

        menu.build {
            option(
                icon = getDrawable(R.drawable.ic_save),
                title = getString(R.string.export)
            ) {
                onClick {
                    export(logFile)

                    dialog.dismiss()
                }
            }
            option(
                icon = getDrawable(R.drawable.ic_delete_colorful),
                title = getString(R.string.delete)
            ) {
                textColor = errorColor

                onClick {
                    delete(logFile)

                    dialog.dismiss()
                }
            }
        }

        dialog.dismissWithAnimation = true
        dialog.setContentView(menu)
        dialog.show()
    }

    private fun deleteAllLogs() {
        launch {
            withContext(Dispatchers.IO) {
                logsDir.deleteRecursively()
            }

            refreshList()
        }
    }

    private fun export(file: LogFile) {
        if (lastWriteFile != null)
            return

        val d = Date(file.date)

        val exportName = getString(R.string.format_export_log_name, d.format(this))

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TITLE, exportName)

        lastWriteFile = file

        startActivityForResult(intent, REQUEST_CODE)
    }

    private fun delete(file: LogFile) {
        val d = {
            launch {
                withContext(Dispatchers.IO) {
                    logsDir.resolve(file.fileName).delete()
                }

                refreshList()
            }
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.delete_log)
            .setMessage(getString(R.string.delete_log_warn, file.fileName))
            .setPositiveButton(R.string.ok) { _, _ -> d() }
            .setNegativeButton(R.string.cancel) { _, _ -> }
            .show()
    }
}