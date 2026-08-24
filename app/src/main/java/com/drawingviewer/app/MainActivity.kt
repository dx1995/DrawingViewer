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
        setContentView(R.layout.activity_main)
        prefsManager = PrefsManager(this)
        setupToolbar()
        setupDrawer()

        if (checkPermissions()) {
            if (savedInstanceState == null) {
                showHomeFragment()
            }
        } else {
            requestPermissions()
        }
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
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasManageStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = android.net.Uri.parse("package:$packageName")
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
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
        if (checkPermissions()) {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
            if (currentFragment == null) {
                showHomeFragment()
            }
        }
    }

    // ========== Fragment 切换 ==========

    private fun showHomeFragment() {
        val folder = FileManager.getDefaultDrawingFolder()
        val fragment = FolderFragment.newInstance(folder.absolutePath, true)
        switchFragmentAsRoot(fragment, getString(R.string.home))
        navigationView.setCheckedItem(R.id.nav_home)
    }

    private fun showFavoritesFragment() {
        val fragment = FavoritesFragment.newInstance()
        switchFragmentAsRoot(fragment, getString(R.string.favorites))
        navigationView.setCheckedItem(R.id.nav_favorites)
    }

    private fun showRecentFragment() {
        val fragment = RecentFragment.newInstance()
        switchFragmentAsRoot(fragment, getString(R.string.recent))
        navigationView.setCheckedItem(R.id.nav_recent)
    }

    private fun showAllImagesFragment() {
        val fragment = AllImagesFragment.newInstance()
        switchFragmentAsRoot(fragment, getString(R.string.all_drawings))
        navigationView.setCheckedItem(R.id.nav_all)
    }

    private fun showSearchFragment() {
        val fragment = SearchFragment.newInstance()
        switchFragment(fragment, getString(R.string.search))
    }

    private fun switchFragmentAsRoot(fragment: Fragment, title: String) {
        supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commitAllowingStateLoss()
        supportActionBar?.title = title
    }

    fun switchFragment(fragment: Fragment, title: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commitAllowingStateLoss()
        supportActionBar?.title = title
    }

    fun navigateToFolder(folderPath: String) {
        val fragment = FolderFragment.newInstance(folderPath, false)
        val folderName = File(folderPath).name
        switchFragment(fragment, folderName)
    }

    fun openImageViewer(images: List<FileItem>, position: Int) {
        val paths = images.map { it.path }.toTypedArray()
        val intent = Intent(this, ImageViewerActivity::class.java).apply {
            putExtra(ImageViewerActivity.EXTRA_IMAGE_PATHS, paths)
            putExtra(ImageViewerActivity.EXTRA_POSITION, position)
        }
        startActivity(intent)
    }

    override fun onBackPressed() {
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
    }

    fun getPrefsManager(): PrefsManager = prefsManager
}
