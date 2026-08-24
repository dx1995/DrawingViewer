package com.drawingviewer.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

// 搜索 Fragment
class SearchFragment : Fragment() {

    private lateinit var searchView: SearchView
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: ImageGridAdapter
    private var searchResults: List<FileItem> = emptyList()
    private var searchJob: Job? = null

    companion object {
        fun newInstance(): SearchFragment {
            return SearchFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchView = view.findViewById(R.id.searchView)
        recyclerView = view.findViewById(R.id.recyclerView)
        tvEmpty = view.findViewById(R.id.tvEmpty)

        adapter = ImageGridAdapter { item, position ->
            (activity as? MainActivity)?.getPrefsManager()?.addToRecent(item.path)
            (activity as? MainActivity)?.openImageViewer(searchResults, position)
        }

        val columns = getGridColumns()
        recyclerView.layoutManager = GridLayoutManager(requireContext(), columns)
        recyclerView.adapter = adapter

        setupSearchView()

        // 默认显示空状态
        tvEmpty.visibility = View.VISIBLE
        tvEmpty.setText(R.string.search_hint)
    }

    private fun getGridColumns(): Int {
        val displayMetrics = resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
        return max(3, (screenWidthDp / 120).toInt())
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                performSearch(query ?: "")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // 防抖搜索
                searchJob?.cancel()
                searchJob = CoroutineScope(Dispatchers.Main).launch {
                    delay(300)
                    performSearch(newText ?: "")
                }
                return true
            }
        })

        // 自动弹出键盘
        searchView.isIconified = false
        searchView.requestFocus()
    }

    private fun performSearch(query: String) {
        if (query.isBlank()) {
            searchResults = emptyList()
            adapter.submitList(emptyList())
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.setText(R.string.search_hint)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val folder = FileManager.getDefaultDrawingFolder()
            val results = FileManager.searchImages(folder, query)

            withContext(Dispatchers.Main) {
                searchResults = results
                adapter.submitList(results)
                if (results.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.setText(R.string.search_no_result)
                } else {
                    tvEmpty.visibility = View.GONE
                }
            }
        }
    }
}
