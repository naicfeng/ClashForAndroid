package com.github.kr328.clash.weight

import android.content.Context
import android.util.TypedValue
import android.view.ViewGroup
import androidx.annotation.ColorInt
import com.github.kr328.clash.R
import com.github.kr328.clash.design.view.CommonUiLayout
import com.github.kr328.clash.service.model.Profile
import com.google.android.material.bottomsheet.BottomSheetDialog

class ProfilesMenu(
    context: Context,
    private val entity: Profile,
    private val callback: Callback
) : BottomSheetDialog(context) {
    interface Callback {
        fun onOpenEditor(entity: Profile)
        fun onUpdate(entity: Profile)
        fun onOpenProperties(entity: Profile)
        fun onDuplicate(entity: Profile)
        fun onResetProvider(entity: Profile)
        fun onDelete(entity: Profile)
    }

    init {
        val menu = CommonUiLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        @ColorInt
        val errorColor = TypedValue().run {
            context.theme.resolveAttribute(R.attr.colorError, this, true)
            data
        }

        menu.build {
            if (entity.type != Profile.Type.FILE) {
                option(
                    title = context.getString(R.string.update),
                    icon = context.getDrawable(R.drawable.ic_update)
                ) {
                    onClick {
                        callback.onUpdate(entity)

                        dismiss()
                    }
                }
            } else {
                option(
                    title = context.getString(R.string.edit),
                    icon = context.getDrawable(R.drawable.ic_edit)
                ) {
                    onClick {
                        callback.onOpenEditor(entity)

                        dismiss()
                    }
                }
            }
            option(
                title = context.getString(R.string.properties),
                icon = context.getDrawable(R.drawable.ic_properties)
            ) {
                onClick {
                    callback.onOpenProperties(entity)

                    dismiss()
                }
            }
            option(
                title = context.getString(R.string.duplicate),
                icon = context.getDrawable(R.drawable.ic_copy)
            ) {
                onClick {
                    callback.onDuplicate(entity)

                    dismiss()
                }
            }
            option(
                title = context.getString(R.string.reset_provider),
                icon = context.getDrawable(R.drawable.ic_clear)
            ) {
                onClick {
                    callback.onResetProvider(entity)

                    dismiss()
                }
            }
            option(
                title = context.getString(R.string.delete),
                icon = context.getDrawable(R.drawable.ic_delete_colorful)
            ) {
                textColor = errorColor

                onClick {
                    callback.onDelete(entity)

                    dismiss()
                }
            }
        }

        dismissWithAnimation = true
        setContentView(menu)
    }
}