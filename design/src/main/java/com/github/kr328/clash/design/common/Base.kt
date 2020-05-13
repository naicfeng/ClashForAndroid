package com.github.kr328.clash.design.common

import android.content.Context
import android.os.Bundle
import android.view.View

@Suppress("MemberVisibilityCanBePrivate")
abstract class Base(val screen: CommonUiScreen) {
    val context: Context
        get() = screen.layout.context

    var id: String? = null

    var dependOn: Base? = null

    var isEnabled: Boolean = true
        get() = field && (dependOn?.isEnabled ?: true)
        set(value) {
            field = value
            screen.postReapplyAttribute()
        }
    var isHidden: Boolean = false
        get() = field || (dependOn?.isHidden ?: false)
        set(value) {
            field = value

            reapplyAttribute()

            screen.postReapplyAttribute()
        }

    fun reapplyAttribute() {
        applyAttribute(isEnabled, isHidden)
    }

    abstract val view: View
    abstract fun saveState(bundle: Bundle)
    abstract fun restoreState(bundle: Bundle)
    protected open fun applyAttribute(enabled: Boolean, hidden: Boolean) {
        view.isEnabled = enabled
        view.visibility = if (hidden) View.GONE else View.VISIBLE
    }
}