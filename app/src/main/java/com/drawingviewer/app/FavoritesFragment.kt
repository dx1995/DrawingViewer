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

// 收藏 Fragment
class FavoritesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: ImageGridAdapter

    companion object {
        fun newInstance(): FavoritesFragment {
            return FavoritesFragment()
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
            openViewer(position)
        }

        // 平板用更多列
        val columns = getGridColumns()
        recyclerView.layoutManager = GridLayoutManager(requireContext(), columns)
        recyclerView.adapter = adapter

        loadFavorites()
    }

    private fun getGridColumns(): Int {
        val displayMetrics = resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
        return max(2, (screenWidthDp / 120).toInt())
    }

    private fun openViewer(position: Int) {
        val prefs = (activity as? MainActivity)?.getPrefsManager() ?: return
        val favorites = prefs.getFavoriteFiles()
        (activity as? MainActivity)?.openImageViewer(favorites, position)
    }

    private fun loadFavorites() {
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = (activity as? MainActivity)?.getPrefsManager()
            val files = prefs?.getFavoriteFiles() ?: emptyList()

            withContext(Dispatchers.Main) {
                adapter.submitList(files)
                tvEmpty.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
                tvEmpty.setText(R.string.no_favorites)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadFavorites()
    }
}
