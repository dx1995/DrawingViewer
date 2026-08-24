package com.drawingviewer.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var prefsManager: PrefsManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted || hasManageStoragePermission()) {
            showHomeFragment()
        } else {
            Toast.makeText(this, R.string.permission_needed, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_main)
        } catch (e: Throwable) {
            showError("界面加载失败", e)
            return
        }

        try {
            prefsManager = PrefsManager(this)
        } catch (e: Throwable) {
            showError("偏好设置初始化失败", e)
            return
        }

        try {
            setupToolbar()
        } catch (e: Throwable) {
            showError("工具栏初始化失败", e)
            return
        }

        try {
            setupDrawer()
        } catch (e: Throwable) {
            showError("侧边栏初始化失败", e)
            return
        }

        try {
            if (checkPermissions()) {
                if (savedInstanceState == null) {
                    showHomeFragment()
                }
            } else {
                requestPermissions()
            }
        } catch (e: Throwable) {
            showError("权限检查失败", e)
        }
    }

    private fun showError(stage: String, e: Throwable) {
        val msg = "$stage\n${e.javaClass.simpleName}: ${e.message}\n\n${e.stackTrace.take(5).joinToString("\n") { it.toString() }}"
        val tv = TextView(this).apply {
            text = msg
            setPadding(32, 32, 32, 32)
            textSize = 12f
        }
        setContentView(tv)
        Toast.makeText(this, stage, Toast.LENGTH_LONG).show()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu)
    }

    private fun setupDrawer() {
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout,
            R.string.app_name, R.string.app_name
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> showHomeFragment()
                R.id.nav_favorites -> showFavoritesFragment()
                R.id.nav_recent -> showRecentFragment()
                R.id.nav_all -> showAllImagesFragment()
            }
            drawerLayout.closeDrawers()
            true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                drawerLayout.open()
                true
            }
            R.id.action_search -> {
                showSearchFragment()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // ========== 权限相关 ==========

    private fun checkPermissions(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun hasManageStoragePermission(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = android.net.Uri.parse("package:$packageName")
                startActivity(intent)
            } catch (e: Exception) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                } catch (e2: Exception) {
                    Toast.makeText(this, "请在设置中手动授予存储权限", Toast.LENGTH_LONG).show()
                }
            }
            Toast.makeText(this, "请授予\"所有文件访问\"权限", Toast.LENGTH_LONG).show()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            )
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            if (checkPermissions()) {
                val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
                if (currentFragment == null) {
                    showHomeFragment()
                }
            }
        } catch (e: Exception) {
            // 忽略
        }
    }

    // ========== Fragment 切换 ==========

    private fun showHomeFragment() {
        try {
            val folder = FileManager.getDefaultDrawingFolder()
            val fragment = FolderFragment.newInstance(folder.absolutePath, true)
            switchFragmentAsRoot(fragment, getString(R.string.home))
            navigationView.setCheckedItem(R.id.nav_home)
        } catch (e: Exception) {
            Toast.makeText(this, "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showFavoritesFragment() {
        try {
            val fragment = FavoritesFragment.newInstance()
            switchFragmentAsRoot(fragment, getString(R.string.favorites))
            navigationView.setCheckedItem(R.id.nav_favorites)
        } catch (e: Exception) {
            Toast.makeText(this, "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showRecentFragment() {
        try {
            val fragment = RecentFragment.newInstance()
            switchFragmentAsRoot(fragment, getString(R.string.recent))
            navigationView.setCheckedItem(R.id.nav_recent)
        } catch (e: Exception) {
            Toast.makeText(this, "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAllImagesFragment() {
        try {
            val fragment = AllImagesFragment.newInstance()
            switchFragmentAsRoot(fragment, getString(R.string.all_drawings))
            navigationView.setCheckedItem(R.id.nav_all)
        } catch (e: Exception) {
            Toast.makeText(this, "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSearchFragment() {
        try {
            val fragment = SearchFragment.newInstance()
            switchFragment(fragment, getString(R.string.search))
        } catch (e: Exception) {
            Toast.makeText(this, "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // 作为根页面切换（清空返回栈）
    private fun switchFragmentAsRoot(fragment: Fragment, title: String) {
        supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commitAllowingStateLoss()
        supportActionBar?.title = title
    }

    // 普通切换（加入返回栈）
    fun switchFragment(fragment: Fragment, title: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commitAllowingStateLoss()
        supportActionBar?.title = title
    }

    fun navigateToFolder(folderPath: String) {
        try {
            val fragment = FolderFragment.newInstance(folderPath, false)
            val folderName = File(folderPath).name
            switchFragment(fragment, folderName)
        } catch (e: Exception) {
            Toast.makeText(this, "打开失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openImageViewer(images: List<FileItem>, position: Int) {
        try {
            val paths = images.map { it.path }.toTypedArray()
            val intent = Intent(this, ImageViewerActivity::class.java).apply {
                putExtra(ImageViewerActivity.EXTRA_IMAGE_PATHS, paths)
                putExtra(ImageViewerActivity.EXTRA_POSITION, position)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "打开失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        try {
            if (drawerLayout.isOpen) {
                drawerLayout.closeDrawers()
            } else if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
                supportFragmentManager.addOnBackStackChangedListener(object : androidx.fragment.app.FragmentManager.OnBackStackChangedListener {
                    override fun onBackStackChanged() {
                        val fragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
                        when (fragment) {
                            is FolderFragment -> supportActionBar?.title = fragment.getTitle()
                            is FavoritesFragment -> supportActionBar?.title = getString(R.string.favorites)
                            is RecentFragment -> supportActionBar?.title = getString(R.string.recent)
                            is AllImagesFragment -> supportActionBar?.title = getString(R.string.all_drawings)
                        }
                        supportFragmentManager.removeOnBackStackChangedListener(this)
                    }
                })
            } else {
                super.onBackPressed()
            }
        } catch (e: Exception) {
            super.onBackPressed()
        }
    }

    fun getPrefsManager(): PrefsManager = prefsManager
}
