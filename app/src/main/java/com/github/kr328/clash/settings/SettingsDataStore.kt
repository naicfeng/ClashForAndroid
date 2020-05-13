package com.github.kr328.clash.settings

import com.github.kr328.clash.common.settings.BaseSettings
import moe.shizuku.preference.PreferenceDataStore

class SettingsDataStore : PreferenceDataStore() {
    interface Source {
        fun set(value: Any?)
        fun get(): Any?
    }

    private val sources: MutableMap<String, Source> = mutableMapOf()
    var applyListener: () -> Unit = {}

    fun on(key: String, source: Source) {
        sources[key] = source
    }

    fun onApply(block: () -> Unit) {
        this.applyListener = block
    }

    inline fun <reified T> BaseSettings.Entry<T>.asSource(settings: BaseSettings): Source {
        return object : Source {
            override fun set(value: Any?) {
                val v = value ?: throw NullPointerException()

                settings.commit {
                    put(this@asSource, v as T)
                }

                applyListener()
            }

            override fun get(): Any? {
                return settings.get(this@asSource)
            }
        }
    }

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        val source = sources[key] ?: return defValue

        return (source.get() as Boolean?) ?: defValue
    }

    override fun putLong(key: String?, value: Long) {
        val source = sources[key] ?: throw NullPointerException()

        source.set(value)
    }

    override fun putInt(key: String?, value: Int) {
        val source = sources[key] ?: throw NullPointerException()

        source.set(value)
    }

    override fun getInt(key: String?, defValue: Int): Int {
        val source = sources[key] ?: return defValue

        return (source.get() as Int?) ?: defValue
    }

    override fun putBoolean(key: String?, value: Boolean) {
        val source = sources[key] ?: throw NullPointerException()

        source.set(value)
    }

    override fun getLong(key: String?, defValue: Long): Long {
        val source = sources[key] ?: return defValue

        return (source.get() as Long?) ?: defValue
    }

    override fun getFloat(key: String?, defValue: Float): Float {
        val source = sources[key] ?: return defValue

        return (source.get() as Float?) ?: defValue
    }

    override fun putFloat(key: String?, value: Float) {
        val source = sources[key] ?: throw NullPointerException()

        source.set(value)
    }

    override fun getString(key: String?, defValue: String?): String? {
        val source = sources[key] ?: return defValue

        return (source.get() as String?) ?: defValue
    }

    override fun putString(key: String?, value: String?) {
        val source = sources[key] ?: throw NullPointerException()

        source.set(value)
    }
}