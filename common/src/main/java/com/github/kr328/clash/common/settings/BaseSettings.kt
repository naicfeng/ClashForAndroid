package com.github.kr328.clash.common.settings

import android.content.SharedPreferences

abstract class BaseSettings(private val preferences: SharedPreferences) {
    interface Entry<T> {
        fun get(preferences: SharedPreferences): T
        fun put(editor: SharedPreferences.Editor, value: T)
    }

    class StringEntry(private val key: String, private val defaultValue: String) :
        Entry<String> {
        override fun get(preferences: SharedPreferences): String {
            return preferences.getString(key, defaultValue)!!
        }

        override fun put(editor: SharedPreferences.Editor, value: String) {
            editor.putString(key, value)
        }
    }

    class BooleanEntry(private val key: String, private val defaultValue: Boolean) :
        Entry<Boolean> {
        override fun get(preferences: SharedPreferences): Boolean {
            return preferences.getBoolean(key, defaultValue)
        }

        override fun put(editor: SharedPreferences.Editor, value: Boolean) {
            editor.putBoolean(key, value)
        }
    }

    class StringSetEntry(private val key: String, private val defaultValue: Set<String>) :
        Entry<Set<String>> {
        override fun get(preferences: SharedPreferences): Set<String> {
            return preferences.getStringSet(key, defaultValue)!!
        }

        override fun put(editor: SharedPreferences.Editor, value: Set<String>) {
            editor.putStringSet(key, value)
        }
    }

    class Editor(private val editor: SharedPreferences.Editor) {
        fun <T> put(entry: Entry<T>, value: T) {
            entry.put(editor, value)
        }
    }

    fun <T> get(entry: Entry<T>): T {
        return entry.get(preferences)
    }

    fun commit(async: Boolean = true, block: Editor.() -> Unit) {
        val editor = preferences.edit()

        Editor(editor).apply(block)

        if (async)
            editor.apply()
        else
            editor.commit()
    }
}