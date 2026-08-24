package com.drawingviewer.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.viewpager2.widget.ViewPager2
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView

// 图片查看器 ViewPager 适配器
class ImagePagerAdapter(
    private val imagePaths: List<String>,
    private val onPhotoTap: () -> Unit
) : RecyclerView.Adapter<ImagePagerAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image_pager, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(imagePaths[position])
    }

    override fun getItemCount(): Int = imagePaths.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val photoView: PhotoView = itemView.findViewById(R.id.photoView)

        fun bind(path: String) {
            Glide.with(itemView.context)
                .load(path)
                .fitCenter()
                .into(photoView)

            // 单击切换工具栏显示/隐藏
            photoView.setOnPhotoTapListener { _, _, _ ->
                onPhotoTap()
            }
        }

        fun getPhotoView(): PhotoView = photoView
    }
}
