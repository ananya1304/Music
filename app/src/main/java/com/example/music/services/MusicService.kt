package com.example.music.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Handler
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.util.Log
import android.widget.RemoteViews
import com.example.music.R
import com.example.music.fragments.SongPlayingFragment
import com.example.music.player.MainActivity
import com.example.music.utils.MusicConstants

class MusicService : Service()
{
    companion object {

        private val FOREGROUND_CHANNEL_ID = "foreground_channel_id"
        private val TAG = MusicService::class.java.simpleName
        var state = MusicConstants.STATE_SERVICE.NOT_INIT
            private set
    }
    private var mPlayer = SongPlayingFragment.Statified.mediaPlayer
    private var mNotificationManager: NotificationManager? = null
    private val mTimerUpdateHandler = Handler()


    override fun onBind(p0: Intent?): IBinder? { return null }

    override fun onCreate() {
        super.onCreate()
        state = MusicConstants.STATE_SERVICE.NOT_INIT
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent == null)
        {
            stopForeground(true)
            stopSelf()
            return Service.START_STICKY
        }

        when(intent.action){
            MusicConstants.ACTION.START_ACTION -> {
                Log.i(TAG, "Received start Intent ")

                state = MusicConstants.STATE_SERVICE.PREPARE
                startForeground(MusicConstants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification())
                if(mPlayer?.isPlaying as Boolean)
                {
                    (mPlayer as MediaPlayer).start()

                }
            }
            MusicConstants.ACTION.PAUSE_ACTION -> {
                state = MusicConstants.STATE_SERVICE.PAUSE
                mNotificationManager!!.notify(MusicConstants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification())
                Log.i(TAG, "Clicked Pause")
                if(mPlayer?.isPlaying as Boolean)
                {
                    (mPlayer as MediaPlayer).pause()
                }
            }
            MusicConstants.ACTION.PLAY_ACTION -> {
                state = MusicConstants.STATE_SERVICE.PREPARE
                mNotificationManager!!.notify(MusicConstants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification())
                Log.i(TAG, "Clicked Play")
                if(!(mPlayer?.isPlaying as Boolean))
                {
                    (mPlayer as MediaPlayer).start()

                }
            }
            MusicConstants.ACTION.STOP_ACTION -> {
                Log.i(TAG, "Received Stop Intent")
                if(mPlayer?.isPlaying as Boolean)
                {
                    (mPlayer as MediaPlayer).pause()
                }
                stopForeground(true)
                stopSelf()
            }

            else -> {
                stopForeground(true)
                stopSelf()
            }
        }
        return Service.START_NOT_STICKY
    }
    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        state = MusicConstants.STATE_SERVICE.NOT_INIT
        try {
            mTimerUpdateHandler.removeCallbacksAndMessages(null)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        super.onDestroy()
    }

    private fun prepareNotification(): Notification {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && mNotificationManager!!.getNotificationChannel(FOREGROUND_CHANNEL_ID) == null) {
            val name = getString(R.string.text_value_player_notification)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val mChannel = NotificationChannel(FOREGROUND_CHANNEL_ID, name, importance)
            mChannel.enableVibration(false)
            mNotificationManager!!.createNotificationChannel(mChannel)
        }
        val notificationIntent = Intent(this, MainActivity::class.java)
        notificationIntent.action = MusicConstants.ACTION.MAIN_ACTION
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        } else {
            notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val lPauseIntent = Intent(this, MusicService::class.java)
        lPauseIntent.action = MusicConstants.ACTION.PAUSE_ACTION
        val lPendingPauseIntent = PendingIntent.getService(this, 0, lPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val playIntent = Intent(this, MusicService::class.java)
        playIntent.action = MusicConstants.ACTION.PLAY_ACTION
        val lPendingPlayIntent = PendingIntent.getService(this, 0, playIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val lStopIntent = Intent(this, MusicService::class.java)
        lStopIntent.action = MusicConstants.ACTION.STOP_ACTION
        val lPendingStopIntent = PendingIntent.getService(this, 0, lStopIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val lRemoteViews = RemoteViews(packageName, R.layout.music_notification)
        lRemoteViews.setTextViewText(R.id.file_description, SongPlayingFragment.Statified.currentSongHelper.songTitle)
        lRemoteViews.setOnClickPendingIntent(R.id.ui_notification_close_button, lPendingStopIntent)

        when (state) {

            MusicConstants.STATE_SERVICE.PAUSE -> {
                lRemoteViews.setOnClickPendingIntent(R.id.ui_notification_player_button, lPendingPlayIntent)
                lRemoteViews.setImageViewResource(R.id.ui_notification_player_button, R.drawable.ic_play_small)
            }

            MusicConstants.STATE_SERVICE.PLAY -> {
                lRemoteViews.setOnClickPendingIntent(R.id.ui_notification_player_button, lPendingPauseIntent)
                lRemoteViews.setImageViewResource(R.id.ui_notification_player_button, R.drawable.ic_pause_small)
            }

            MusicConstants.STATE_SERVICE.PREPARE -> {
                lRemoteViews.setOnClickPendingIntent(R.id.ui_notification_player_button, lPendingPauseIntent)
                lRemoteViews.setImageViewResource(R.id.ui_notification_player_button, R.drawable.ic_pause_small)

            }
        }

        val lNotificationBuilder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
        } else {
            NotificationCompat.Builder(this)
        }
        lNotificationBuilder
            .setContent(lRemoteViews)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            lNotificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        }
        return lNotificationBuilder.build()

    }


}