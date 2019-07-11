package com.example.music.player

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle

import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.MenuItem
import com.example.music.R
import com.example.music.fragments.FavoriteFragment
import com.example.music.fragments.MainScreenFragment
import com.example.music.fragments.SongPlayingFragment
import com.example.music.player.MainActivity.Staticated.notificationManager
import com.example.music.services.BackgroundAudioService

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var mDrawer: DrawerLayout
    private lateinit var nvDrawer: NavigationView
    var trackNotificationBuilder: Notification? = null


    object Staticated {
        var notificationManager: NotificationManager? = null
    }
    override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)

            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)
        mDrawer = findViewById(R.id.drawer_layout)
        nvDrawer = findViewById(R.id.nvView)
        val toggle = ActionBarDrawerToggle(this@MainActivity, mDrawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        toggle.isDrawerIndicatorEnabled = true

            val audioIntent = Intent(this@MainActivity, BackgroundAudioService::class.java)
            startService(audioIntent)

         val mainScreenFragment = MainScreenFragment()
            this.supportFragmentManager
                .beginTransaction()
                .add(R.id.flContent, mainScreenFragment, "RecyclerScreenFragment")
                .commit()

        val intent = Intent(this@MainActivity, MainActivity::class.java)
        val pIntent = PendingIntent.getActivity(this@MainActivity, System.currentTimeMillis().toInt(), intent, 0)

            trackNotificationBuilder = Notification.Builder(this)
                .setContentTitle("A track is playing in background")
                .setSmallIcon(R.drawable.ic_play)
                .setContentIntent(pIntent)
                .setOngoing(true)
                .setAutoCancel(true).build()
            notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
    }

    override fun onStop() {
        super.onStop()
        //notification handler
        try {
            if (SongPlayingFragment.Statified.mediaPlayer?.isPlaying as Boolean) {
                notificationManager?.notify(1978, trackNotificationBuilder)
            }
        } catch (ee: Exception) {
            ee.printStackTrace()
        }

    }

    override fun onStart() {
        super.onStart()
        //notification handler
        try {
            notificationManager?.cancel(1978)
        } catch (ee: Exception) {
            ee.printStackTrace()
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        val audioIntent = Intent(this@MainActivity,BackgroundAudioService::class.java)
        stopService(audioIntent)
    }

    override fun onNavigationItemSelected(p0: MenuItem): Boolean {
        when(p0.itemId){
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
        if(mDrawer.isDrawerOpen(GravityCompat.START)){
            mDrawer.closeDrawer(GravityCompat.START)
        }
        else{
            super.onBackPressed()
        }
    }

}
