package com.github.kr328.clash.design.common

import android.content.Context
import android.graphics.drawable.Drawable

class CommonUiBuilder(val screen: CommonUiScreen) {
    val context: Context
        get() = screen.layout.context

    fun textInput(
        title: String = "",
        hint: CharSequence = "",
        icon: Drawable? = null,
        content: String = "",
        id: String? = null,
        dependOn: String? = null,
        setup: TextInput.() -> Unit = {}
    ) {
        val textInput = TextInput(screen)

        textInput.title = title
        textInput.hint = hint
        textInput.icon = icon
        textInput.content = content
        textInput.id = id
        textInput.dependOn = dependOn?.run { screen.requireElement(this) }

        screen.addElement(textInput)

        textInput.setup()
    }

    fun option(
        title: String = "",
        summary: String = "",
        icon: Drawable? = null,
        id: String? = null,
        dependOn: String? = null,
        setup: Option.() -> Unit = {}
    ) {
        val option = Option(screen)

        option.title = title
        option.summary = summary
        option.icon = icon
        option.id = id
        option.dependOn = dependOn?.run { screen.requireElement(this) }

        setup(option)

        screen.addElement(option)
    }

    fun category(
        text: String = "",
        showTopSeparator: Boolean = false,
        showBottomSeparator: Boolean = false,
        id: String? = null,
        setup: Category.() -> Unit = {}
    ) {
        val category = Category(screen)

        category.text = text
        category.showTopSeparator = showTopSeparator
        category.showBottomSeparator = showBottomSeparator
        category.id = id

        setup(category)

        screen.addElement(category)
    }

    fun tips(
        title: String = "",
        icon: Drawable? = null,
        setup: Tips.() -> Unit = {}
    ) {
        val tips = Tips(screen)

        tips.title = title
        tips.icon = icon

        setup(tips)

        screen.addElement(tips)
    }
}