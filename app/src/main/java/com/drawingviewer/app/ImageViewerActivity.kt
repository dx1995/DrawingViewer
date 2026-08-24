package com.drawingviewer.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import java.io.File

// 大图查看器 Activity
class ImageViewerActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var topBar: View
    private lateinit var tvTitle: TextView
    private lateinit var tvIndicator: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var btnFavorite: ImageButton
    private lateinit var btnRotate: ImageButton
    private lateinit var pagerAdapter: ImagePagerAdapter

    private lateinit var prefsManager: PrefsManager
    private var imagePaths: List<String> = emptyList()
    private var currentPosition: Int = 0
    private var isUiVisible = true
    private var currentRotation = 0f

    companion object {
        const val EXTRA_IMAGE_PATHS = "extra_image_paths"
        const val EXTRA_POSITION = "extra_position"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)

        prefsManager = PrefsManager(this)

        // 获取数据
        imagePaths = intent.getStringArrayExtra(EXTRA_IMAGE_PATHS)?.toList() ?: emptyList()
        currentPosition = intent.getIntExtra(EXTRA_POSITION, 0)

        initViews()
        setupViewPager()
        updateUI()
        hideSystemUI()
    }

    private fun initViews() {
        viewPager = findViewById(R.id.viewPager)
        topBar = findViewById(R.id.topBar)
        tvTitle = findViewById(R.id.tvTitle)
        tvIndicator = findViewById(R.id.tvIndicator)
        btnBack = findViewById(R.id.btnBack)
        btnFavorite = findViewById(R.id.btnFavorite)
        btnRotate = findViewById(R.id.btnRotate)

        btnBack.setOnClickListener { finish() }
        btnFavorite.setOnClickListener { toggleFavorite() }
        btnRotate.setOnClickListener { rotateImage() }
    }

    private fun setupViewPager() {
        pagerAdapter = ImagePagerAdapter(imagePaths) {
            toggleUiVisibility()
        }
        viewPager.adapter = pagerAdapter
        viewPager.currentItem = currentPosition

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentPosition = position
                currentRotation = 0f
                updateUI()
                // 记录到最近浏览
                prefsManager.addToRecent(imagePaths[position])
            }
        })
    }

    private fun updateUI() {
        if (imagePaths.isEmpty()) return

        val currentPath = imagePaths[currentPosition]
        val file = File(currentPath)
        tvTitle.text = file.name
        tvIndicator.text = "${currentPosition + 1} / ${imagePaths.size}"

        // 更新收藏按钮状态
        if (prefsManager.isFavorite(currentPath)) {
            btnFavorite.setImageResource(R.drawable.ic_favorite)
            btnFavorite.contentDescription = getString(R.string.remove_favorite)
        } else {
            btnFavorite.setImageResource(R.drawable.ic_favorite_border)
            btnFavorite.contentDescription = getString(R.string.add_favorite)
        }
    }

    private fun toggleFavorite() {
        val currentPath = imagePaths[currentPosition]
        val isFavorite = prefsManager.toggleFavorite(currentPath)

        if (isFavorite) {
            btnFavorite.setImageResource(R.drawable.ic_favorite)
        } else {
            btnFavorite.setImageResource(R.drawable.ic_favorite_border)
        }
    }

    private fun rotateImage() {
        currentRotation += 90f
        // 找到当前的 PhotoView 并旋转
        val viewHolder = findCurrentViewHolder()
        viewHolder?.getPhotoView()?.rotation = currentRotation
    }

    private fun findCurrentViewHolder(): ImagePagerAdapter.ViewHolder? {
        val recyclerView = viewPager.getChildAt(0) as? androidx.recyclerview.widget.RecyclerView
        return recyclerView?.findViewHolderForAdapterPosition(currentPosition) as? ImagePagerAdapter.ViewHolder
    }

    private fun toggleUiVisibility() {
        if (isUiVisible) {
            topBar.visibility = View.GONE
            tvIndicator.visibility = View.GONE
            hideSystemUI()
        } else {
            topBar.visibility = View.VISIBLE
            tvIndicator.visibility = View.VISIBLE
            showSystemUI()
        }
        isUiVisible = !isUiVisible
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                )
        }
    }

    private fun showSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.show(WindowInsets.Type.systemBars())
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
        }
    }
}
