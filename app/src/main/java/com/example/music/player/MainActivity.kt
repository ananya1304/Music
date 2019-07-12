package com.example.music.player

import android.app.PendingIntent
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle

import android.support.v7.widget.Toolbar
import android.view.MenuItem
import com.example.music.R
import com.example.music.fragments.FavoriteFragment
import com.example.music.fragments.MainScreenFragment
import com.example.music.fragments.SongPlayingFragment.Statified.activity
import com.example.music.services.BackgroundAudioService
import com.example.music.services.MusicService
import com.example.music.utils.MusicConstants

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var mDrawer: DrawerLayout
    private lateinit var nvDrawer: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        mDrawer = findViewById(R.id.drawer_layout)
        nvDrawer = findViewById(R.id.nvView)
        val toggle = ActionBarDrawerToggle(
            this@MainActivity,
            mDrawer,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        mDrawer.addDrawerListener(toggle)
        toggle.isDrawerIndicatorEnabled = true
        toggle.syncState()

        nvDrawer.setNavigationItemSelectedListener(this)
        val audioIntent = Intent(this@MainActivity, BackgroundAudioService::class.java)
        startService(audioIntent)

        val mainScreenFragment = MainScreenFragment()
        this.supportFragmentManager
            .beginTransaction()
            .add(R.id.flContent, mainScreenFragment, "RecyclerScreenFragment")
            .commit()
    }

    override fun onStop() {
        super.onStop()
        val lState = MusicService.state
        if (lState == MusicConstants.STATE_SERVICE.NOT_INIT) {
            Intent(activity, MusicService::class.java).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                action = MusicConstants.ACTION.START_ACTION
                activity!!.startService(this)
            }
        } else if (lState == MusicConstants.STATE_SERVICE.PREPARE || lState == MusicConstants.STATE_SERVICE.PLAY) {
            val lPlayIntent = Intent(activity, MusicService::class.java)
            lPlayIntent.action = MusicConstants.ACTION.PLAY_ACTION
            val lPendingPauseIntent = PendingIntent.getService(activity, 0, lPlayIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            try {
                lPendingPauseIntent.send()
            } catch (e: PendingIntent.CanceledException) {
                e.printStackTrace()
            }

        } else if (lState == MusicConstants.STATE_SERVICE.PAUSE) {
            val lPauseIntent = Intent(activity, MusicService::class.java)
            lPauseIntent.action = MusicConstants.ACTION.PAUSE_ACTION
            val lPendingPauseIntent = PendingIntent.getService(activity, 0, lPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            try {
                lPendingPauseIntent.send()
            } catch (e: PendingIntent.CanceledException) {
                e.printStackTrace()
            }
        }

    }

    override fun onStart() {
        super.onStart()
        try {
            val lStopIntent = Intent(activity, MusicService::class.java)
            lStopIntent.action = MusicConstants.ACTION.STOP_ACTION
            val lPendingStopIntent = PendingIntent.getService(activity, 0, lStopIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            try {
                lPendingStopIntent.send()
            } catch (e: PendingIntent.CanceledException) {
                e.printStackTrace()
            }        } catch (ee: Exception) {
            ee.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val audioIntent = Intent(this@MainActivity, BackgroundAudioService::class.java)
        stopService(audioIntent)
        try {
            val lStopIntent = Intent(activity, MusicService::class.java)
            lStopIntent.action = MusicConstants.ACTION.STOP_ACTION
            val lPendingStopIntent = PendingIntent.getService(activity, 0, lStopIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            try {
                lPendingStopIntent.send()
            } catch (e: PendingIntent.CanceledException) {
                e.printStackTrace()
            }        } catch (ee: Exception) {
            ee.printStackTrace()
        }
    }

    override fun onNavigationItemSelected(p0: MenuItem): Boolean {
        when (p0.itemId) {
            R.id.nav_first_fragment -> {
                val mainScreenFragment = MainScreenFragment()
                this.supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.flContent, mainScreenFragment)
                    .commit()
            }

            R.id.nav_second_fragment -> {
                val favoriteFragment = FavoriteFragment()
                this.supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.flContent, favoriteFragment)
                    .commit()
            }
        }
        mDrawer.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        if (mDrawer.isDrawerOpen(GravityCompat.START)) {
            mDrawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

}

