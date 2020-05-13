package com.github.kr328.clash

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.github.kr328.clash.adapter.ProxyAdapter
import com.github.kr328.clash.adapter.ProxyChipAdapter
import com.github.kr328.clash.core.model.General
import com.github.kr328.clash.pipeline.Pipeline
import com.github.kr328.clash.pipeline.mergePrefix
import com.github.kr328.clash.pipeline.sort
import com.github.kr328.clash.pipeline.toAdapterElement
import com.github.kr328.clash.preference.UiSettings
import com.github.kr328.clash.remote.withClash
import com.github.kr328.clash.utils.ScrollBinding
import kotlinx.android.synthetic.main.activity_proxies.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

class ProxiesActivity : BaseActivity(), ScrollBinding.Callback {
    private val refreshMutex = Mutex()
    private val scrollBinding = ScrollBinding(this, this)
    private var scrollToLast = true

    private val mainListAdapter: ProxyAdapter
        get() = mainList.adapter as ProxyAdapter
    private val chipListAdapter: ProxyChipAdapter
        get() = chipList.adapter as ProxyChipAdapter
    private val urlTesting: MutableSet<String> = mutableSetOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_proxies)
        setSupportActionBar(toolbar)

        mainList.adapter = ProxyAdapter(this, this::setGroupSelected, this::startUrlTesting)
        mainList.layoutManager = mainListAdapter.layoutManager

        chipList.adapter = ProxyChipAdapter(this, this::chipClicked)
        chipList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        chipList.itemAnimator?.changeDuration = 0

        launch {
            mainList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    scrollBinding.sendMasterScrolled()
                }
            })

            scrollBinding.exec()
        }

        refreshList()
    }

    override fun onStop() {
        uiSettings.commit {
            put(
                UiSettings.PROXY_LAST_SELECT_GROUP,
                (chipList.adapter!! as ProxyChipAdapter).selected
            )
        }

        super.onStop()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (!super.onCreateOptionsMenu(menu))
            return false

        menuInflater.inflate(R.menu.proxies, menu)

        setupMenu()

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (super.onOptionsItemSelected(item))
            return true

        if (item.itemId == R.id.menuRefresh) {
            refreshList()
            return true
        }

        launch {
            var scrollTop = false

            when (item.itemId) {
                R.id.modeDirect -> {
                    withClash {
                        setProxyMode(General.Mode.DIRECT)
                    }
                }
                R.id.modeGlobal -> {
                    withClash {
                        setProxyMode(General.Mode.GLOBAL)

                        scrollTop = true
                    }
                }
                R.id.modeRule -> {
                    withClash {
                        setProxyMode(General.Mode.RULE)
                    }
                }
                R.id.groupDefault -> {
                    uiSettings.commit {
                        put(UiSettings.PROXY_GROUP_SORT, UiSettings.PROXY_SORT_DEFAULT)
                    }
                }
                R.id.groupName -> {
                    uiSettings.commit {
                        put(UiSettings.PROXY_GROUP_SORT, UiSettings.PROXY_SORT_NAME)
                    }
                }
                R.id.groupDelay -> {
                    uiSettings.commit {
                        put(UiSettings.PROXY_GROUP_SORT, UiSettings.PROXY_SORT_DELAY)
                    }
                }
                R.id.proxyDefault -> {
                    uiSettings.commit {
                        put(UiSettings.PROXY_PROXY_SORT, UiSettings.PROXY_SORT_DEFAULT)
                    }
                }
                R.id.proxyName -> {
                    uiSettings.commit {
                        put(UiSettings.PROXY_PROXY_SORT, UiSettings.PROXY_SORT_NAME)
                    }
                }
                R.id.proxyDelay -> {
                    uiSettings.commit {
                        put(UiSettings.PROXY_PROXY_SORT, UiSettings.PROXY_SORT_DELAY)
                    }
                }
                R.id.utilsMergePrefix -> {
                    item.isChecked = !item.isChecked

                    uiSettings.commit {
                        put(UiSettings.PROXY_MERGE_PREFIX, item.isChecked)
                    }

                    refreshList()

                    return@launch
                }
                else -> return@launch
            }

            item.isChecked = true

            refreshList(scrollTop)
        }

        return true
    }

    override suspend fun onClashStopped(reason: String?) {
        finish()
    }

    private fun setupMenu() {
        launch {
            val general = withClash {
                queryGeneral()
            }

            menu?.apply {
                when (general.mode) {
                    General.Mode.DIRECT ->
                        findItem(R.id.modeDirect).isChecked = true
                    General.Mode.GLOBAL ->
                        findItem(R.id.modeGlobal).isChecked = true
                    General.Mode.RULE ->
                        findItem(R.id.modeRule).isChecked = true
                }
                when (uiSettings.get(UiSettings.PROXY_GROUP_SORT)) {
                    UiSettings.PROXY_SORT_DEFAULT ->
                        findItem(R.id.groupDefault).isChecked = true
                    UiSettings.PROXY_SORT_NAME ->
                        findItem(R.id.groupName).isChecked = true
                    UiSettings.PROXY_SORT_DELAY ->
                        findItem(R.id.proxyDelay).isChecked = true
                }
                when (uiSettings.get(UiSettings.PROXY_PROXY_SORT)) {
                    UiSettings.PROXY_SORT_DEFAULT ->
                        findItem(R.id.proxyDefault).isChecked = true
                    UiSettings.PROXY_SORT_NAME ->
                        findItem(R.id.proxyName).isChecked = true
                    UiSettings.PROXY_SORT_DELAY ->
                        findItem(R.id.proxyDelay).isChecked = true
                }

                findItem(R.id.utilsMergePrefix).isChecked =
                    uiSettings.get(UiSettings.PROXY_MERGE_PREFIX)
            }
        }
    }

    private fun setGroupSelected(group: String, select: String) {
        launch {
            withClash {
                setSelectProxy(group, select)
            }
        }
    }

    private fun startUrlTesting(group: String) {
        launch {
            urlTesting.add(group)

            withClash {
                startHealthCheck(group)
            }

            urlTesting.remove(group)

            refreshList()
        }
    }

    private fun chipClicked(name: String) {
        launch {
            scrollBinding.scrollMaster(name)
        }
    }

    private fun refreshList(scrollTop: Boolean = false) {
        launch {
            if (!refreshMutex.tryLock())
                return@launch

            val general = withClash {
                queryGeneral()
            }
            val proxies = withClash {
                queryAllProxyGroups()
            }

            val merged = Pipeline(proxies, uiSettings).mergePrefix()
            val sorted = Pipeline(proxies, uiSettings).sort()

            val newList = sorted.toAdapterElement(merged.input, general)

            mainListAdapter.applyChange(newList, urlTesting)

            (chipList.adapter!! as ProxyChipAdapter).apply {
                chips = newList.map { it.name }
                notifyDataSetChanged()
            }

            if (scrollTop)
                mainList.smoothScrollToPosition(0)
            else if (scrollToLast) {
                scrollToLast = false

                val selected = uiSettings.get(UiSettings.PROXY_LAST_SELECT_GROUP)

                scrollBinding.scrollMaster(selected)
            }

            delay(500)

            refreshMutex.unlock()
        }
    }

    override fun getCurrentMasterToken(): String {
        return mainListAdapter.getCurrentGroup()
    }

    override fun onMasterTokenChanged(token: String) {
        chipListAdapter.selected = token
        val position = chipListAdapter.chips.indexOf(token)

        if (position < 0)
            return

        chipList.smoothScrollToPosition(position)
    }

    override fun getMasterTokenPosition(token: String): Int {
        return mainListAdapter.getGroupPosition(token)
    }

    override fun doMasterScroll(scroller: LinearSmoothScroller, target: Int) {
        mainListAdapter.layoutManager.startSmoothScroll(scroller)
    }
}