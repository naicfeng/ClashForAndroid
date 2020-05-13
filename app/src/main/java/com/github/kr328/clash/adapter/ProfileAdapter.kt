package com.github.kr328.clash.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.github.kr328.clash.R
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.utils.IntervalUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProfileAdapter(private val context: Context, private val callback: Callback) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    interface Callback {
        fun onProfileClicked(entity: Profile)
        fun onMenuClicked(entity: Profile)
        fun onNewProfile()
    }

    private var entities: List<Profile> = emptyList()

    class EntityHolder(view: View) : RecyclerView.ViewHolder(view) {
        val root: View = view.findViewById(R.id.root)
        val menu: View = view.findViewById(R.id.menu)
        val radio: RadioButton = view.findViewById(R.id.radio)
        val name: TextView = view.findViewById(R.id.name)
        val type: TextView = view.findViewById(R.id.type)
        val interval: TextView = view.findViewById(R.id.interval)
    }

    class FooterHolder(view: View) : RecyclerView.ViewHolder(view) {
        val root: View = view.findViewById(R.id.root)
    }

    suspend fun setEntitiesAsync(new: List<Profile>) {
        val old = withContext(Dispatchers.Main) {
            entities
        }

        val result = withContext(Dispatchers.Default) {
            DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                    old[oldItemPosition].id == new[newItemPosition].id

                override fun areContentsTheSame(
                    oldItemPosition: Int,
                    newItemPosition: Int
                ): Boolean = old[oldItemPosition] == new[newItemPosition]

                override fun getOldListSize(): Int = old.size
                override fun getNewListSize(): Int = new.size
            }, false)
        }

        withContext(Dispatchers.Main) {
            entities = new
            result.dispatchUpdatesTo(this@ProfileAdapter)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == entities.size)
            Int.MAX_VALUE
        else
            super.getItemViewType(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == Int.MAX_VALUE) {
            return FooterHolder(
                LayoutInflater.from(context).inflate(
                    R.layout.adapter_profile_footer,
                    parent,
                    false
                )
            )
        }
        return EntityHolder(
            LayoutInflater.from(context).inflate(
                R.layout.adapter_profile_entity,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return entities.size + 1
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is EntityHolder -> {
                val current = entities[position]

                holder.radio.isChecked = current.active
                holder.name.text = current.name
                holder.type.text = getTypeName(current.type)
                holder.interval.text = offsetDate(current.lastModified)

                holder.root.setOnClickListener {
                    callback.onProfileClicked(current)
                }
                holder.menu.setOnClickListener {
                    callback.onMenuClicked(current)
                }
            }
            is FooterHolder -> {
                holder.root.setOnClickListener {
                    callback.onNewProfile()
                }
            }
        }
    }

    private fun getTypeName(type: Profile.Type): CharSequence {
        return when (type) {
            Profile.Type.FILE ->
                context.getText(R.string.file)
            Profile.Type.URL ->
                context.getText(R.string.url)
            Profile.Type.EXTERNAL ->
                context.getText(R.string.external)
            else ->
                context.getText(R.string.unknown)
        }
    }

    private fun offsetDate(date: Long): CharSequence {
        return IntervalUtils.intervalString(context, System.currentTimeMillis() - date)
    }
}