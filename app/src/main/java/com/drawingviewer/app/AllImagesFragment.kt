package com.drawingviewer.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

// 全部图纸 Fragment - 递归显示所有图片
class AllImagesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: ImageGridAdapter
    private var allImages: List<FileItem> = emptyList()

    companion object {
        fun newInstance(): AllImagesFragment {
            return AllImagesFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_folder, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)
        tvEmpty = view.findViewById(R.id.tvEmpty)

        adapter = ImageGridAdapter { item, position ->
            (activity as? MainActivity)?.getPrefsManager()?.addToRecent(item.path)
            (activity as? MainActivity)?.openImageViewer(allImages, position)
        }

        val columns = getGridColumns()
        recyclerView.layoutManager = GridLayoutManager(requireContext(), columns)
        recyclerView.adapter = adapter

        loadAllImages()
    }

    private fun getGridColumns(): Int {
        val displayMetrics = resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
        return max(3, (screenWidthDp / 120).toInt())
    }

    private fun loadAllImages() {
        CoroutineScope(Dispatchers.IO).launch {
            val folder = FileManager.getDefaultDrawingFolder()
            val images = FileManager.getAllImages(folder)

            withContext(Dispatchers.Main) {
                allImages = images
                adapter.submitList(images)
                tvEmpty.visibility = if (images.isEmpty()) View.VISIBLE else View.GONE
                tvEmpty.setText(R.string.no_files)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadAllImages()
    }
}
