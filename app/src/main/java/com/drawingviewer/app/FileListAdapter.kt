package com.drawingviewer.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

// 文件列表适配器 - 用于显示文件夹和图片混合列表
class FileListAdapter(
    private val onItemClick: (FileItem) -> Unit
) : ListAdapter<FileItem, FileListAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.ivIcon)
        private val name: TextView = itemView.findViewById(R.id.tvName)
        private val count: TextView = itemView.findViewById(R.id.tvCount)

        fun bind(item: FileItem) {
            name.text = item.name

            if (item.isDirectory) {
                icon.setImageResource(R.drawable.ic_folder)
                count.visibility = View.VISIBLE
                count.text = "${item.childCount} ${itemView.context.getString(R.string.items)}"
            } else {
                count.visibility = View.GONE
                Glide.with(itemView.context)
                    .load(item.file)
                    .centerCrop()
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .into(icon)
            }

            itemView.setOnClickListener { onItemClick(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<FileItem>() {
        override fun areItemsTheSame(oldItem: FileItem, newItem: FileItem): Boolean {
            return oldItem.path == newItem.path
        }

        override fun areContentsTheSame(oldItem: FileItem, newItem: FileItem): Boolean {
            return oldItem == newItem
        }
    }
}
