package com.example.ddiysec

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var pagerAdapter: FragmentPagerAdapter

    // API Status Views (ONLY THESE REMAIN IN MAIN ACTIVITY)
    private lateinit var statusDotLive: View
    private lateinit var statusDotCsv: View
    private lateinit var tvApiLastChecked: TextView

    private var statusCheckHandler: Handler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        supportActionBar?.hide()

        initViews()
        setupViewPager()
        setupTabLayout()

        // Only check API status now (status card moved to LiveDetectionFragment)
        checkApiStatus()
        statusCheckHandler = Handler(Looper.getMainLooper())
        startPeriodicApiCheck()
    }

    private fun initViews() {
        // Fragment views
        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)

        statusDotLive = findViewById(R.id.statusDotLive)
        statusDotCsv = findViewById(R.id.statusDotCsv)
        tvApiLastChecked = findViewById(R.id.tvApiLastChecked)
    }

    private fun setupViewPager() {
        pagerAdapter = FragmentPagerAdapter(this)
        viewPager.adapter = pagerAdapter
    }

    // memilih fragment view
    private fun setupTabLayout() {
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = "Live Detection"
                    tab.setIcon(R.drawable.ic_radar)
                }
                1 -> {
                    tab.text = "Upload Files"
                    tab.setIcon(R.drawable.ic_upload)
                }
            }
        }.attach()
    }

    //Get status Update
    fun updateLiveDetectionStatus(ddosPercentage: Int, intrusionCount: Int, totalConnections: Int) {
        val fragments = supportFragmentManager.fragments
        val liveFragment = fragments.find { it is LiveDetectionFragment } as? LiveDetectionFragment
        liveFragment?.updateStatusFromUpload(ddosPercentage, intrusionCount, totalConnections)
    }

    // Method to switch to Live Detection tab (useful for upload results)
    fun switchToLiveDetectionTab() {
        viewPager.currentItem = 0
    }

    private fun checkApiStatus() {
        CoroutineScope(Dispatchers.IO).launch {
            var liveOnline = false
            var csvOnline = false

            try {
                ApiClient.liveApiService.getStatistics(ApiClient.getApiKey("live"))
                liveOnline = true
            } catch (e: Exception) {
                println("Live API offline: ${e.message}")
            }

            try {
                csvOnline = true
            } catch (e: Exception) {
                println("CSV API offline: ${e.message}")
            }

            withContext(Dispatchers.Main) {
                statusDotLive.setBackgroundResource(
                    if (liveOnline) R.drawable.status_dot_online else R.drawable.status_dot_offline
                )
                statusDotCsv.setBackgroundResource(
                    if (csvOnline) R.drawable.status_dot_online else R.drawable.status_dot_offline
                )
                tvApiLastChecked.text = "Last checked: ${getCurrentTime()}"
            }
        }
    }

    private fun startPeriodicApiCheck() {
        statusCheckHandler?.postDelayed({
            checkApiStatus()
            startPeriodicApiCheck()
        }, 120000) // Check every 2 minutes
    }

    private fun getCurrentTime(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }

    override fun onDestroy() {
        super.onDestroy()
        statusCheckHandler?.removeCallbacksAndMessages(null)
    }

    private class FragmentPagerAdapter(fragmentActivity: FragmentActivity) :
        FragmentStateAdapter(fragmentActivity) {

        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> LiveDetectionFragment.newInstance()
                1 -> UploadFilesFragment.newInstance()
                else -> LiveDetectionFragment.newInstance()
            }
        }
    }
}