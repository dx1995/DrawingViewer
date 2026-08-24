package com.drawingviewer.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// 文件夹浏览 Fragment
class FolderFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: FileListAdapter
    private lateinit var folderPath: String
    private var isRoot = false

    companion object {
        private const val ARG_FOLDER_PATH = "folder_path"
        private const val ARG_IS_ROOT = "is_root"

        fun newInstance(folderPath: String, isRoot: Boolean = false): FolderFragment {
            return FolderFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_FOLDER_PATH, folderPath)
                    putBoolean(ARG_IS_ROOT, isRoot)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        folderPath = arguments?.getString(ARG_FOLDER_PATH) ?: ""
        isRoot = arguments?.getBoolean(ARG_IS_ROOT, false) ?: false
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

        adapter = FileListAdapter { item ->
            if (item.isDirectory) {
                (activity as? MainActivity)?.navigateToFolder(item.path)
            } else {
                // 打开图片查看器 - 收集当前文件夹所有图片
                openImageViewerForCurrentFolder(item)
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        loadFiles()
    }

    private fun openImageViewerForCurrentFolder(selectedItem: FileItem) {
        CoroutineScope(Dispatchers.IO).launch {
            val folder = File(folderPath)
            val allItems = FileManager.getFilesInFolder(folder)
            val imageItems = allItems.filter { it.isImage }
            val position = imageItems.indexOfFirst { it.path == selectedItem.path }

            withContext(Dispatchers.Main) {
                if (imageItems.isNotEmpty()) {
                    // 记录到最近浏览
                    (activity as? MainActivity)?.getPrefsManager()?.addToRecent(selectedItem.path)
                    (activity as? MainActivity)?.openImageViewer(imageItems, position)
                }
            }
        }
    }

    private fun loadFiles() {
        CoroutineScope(Dispatchers.IO).launch {
            val folder = File(folderPath)
            val files = FileManager.getFilesInFolder(folder)

            withContext(Dispatchers.Main) {
                adapter.submitList(files)
                if (files.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.setText(
                        if (isRoot) R.string.no_files else R.string.no_files
                    )
                } else {
                    tvEmpty.visibility = View.GONE
                }
            }
        }
    }

    fun getTitle(): String {
        return if (isRoot) getString(R.string.home) else File(folderPath).name
    }

    override fun onResume() {
        super.onResume()
        loadFiles() // 刷新数据
    }
}
