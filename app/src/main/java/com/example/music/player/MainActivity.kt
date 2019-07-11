package com.example.music.player

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle

import android.support.v7.widget.Toolbar
import com.example.music.R
import com.example.music.fragments.MainScreenFragment
import com.example.music.services.BackgroundAudioService

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)

            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)

            val audioIntent = Intent(this@MainActivity, BackgroundAudioService::class.java)
            startService(audioIntent)


            val mainScreenFragment = MainScreenFragment()
            this.supportFragmentManager
                .beginTransaction()
                .add(R.id.details_fragment, mainScreenFragment, "RecyclerScreenFragment")
                .commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        val audioIntent = Intent(this@MainActivity,BackgroundAudioService::class.java)
        stopService(audioIntent)
    }
}
