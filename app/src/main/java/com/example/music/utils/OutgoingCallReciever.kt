package com.example.music.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import com.example.music.fragments.SongPlayingFragment
import com.example.music.player.MainActivity

class OutgoingCallReciever : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_NEW_OUTGOING_CALL) {
            try {
                MainActivity.Staticated.notificationManager?.cancel(1978)

                if (SongPlayingFragment.Statified.mediaPlayer?.isPlaying as Boolean) {
                    (SongPlayingFragment.Statified.mediaPlayer as MediaPlayer).pause()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}