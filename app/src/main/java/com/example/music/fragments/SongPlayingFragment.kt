package com.example.music.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.example.music.R
import com.example.music.data.EchoDatabase
import com.example.music.data.Songs
import com.example.music.fragments.SongPlayingFragment.Statified.MY_PREFS_LOOP
import com.example.music.fragments.SongPlayingFragment.Statified.MY_PREFS_SHUFFLE
import com.example.music.fragments.SongPlayingFragment.Statified.UpdateSongTime
import com.example.music.fragments.SongPlayingFragment.Statified._songArtist
import com.example.music.fragments.SongPlayingFragment.Statified._songTitle
import com.example.music.fragments.SongPlayingFragment.Statified.activity
import com.example.music.fragments.SongPlayingFragment.Statified.currentPosition
import com.example.music.fragments.SongPlayingFragment.Statified.currentSongHelper
import com.example.music.fragments.SongPlayingFragment.Statified.endTimeText
import com.example.music.fragments.SongPlayingFragment.Statified.fab
import com.example.music.fragments.SongPlayingFragment.Statified.fastforwardImageButton
import com.example.music.fragments.SongPlayingFragment.Statified.favouriteContent
import com.example.music.fragments.SongPlayingFragment.Statified.fetchSongs
import com.example.music.fragments.SongPlayingFragment.Statified.loopImageButton
import com.example.music.fragments.SongPlayingFragment.Statified.mediaPlayer
import com.example.music.fragments.SongPlayingFragment.Statified.playlistImageButton
import com.example.music.fragments.SongPlayingFragment.Statified.playpauseImageButton
import com.example.music.fragments.SongPlayingFragment.Statified.rewindImageButton
import com.example.music.fragments.SongPlayingFragment.Statified.seekbar
import com.example.music.fragments.SongPlayingFragment.Statified.shuffleImageButton
import com.example.music.fragments.SongPlayingFragment.Statified.songArtist
import com.example.music.fragments.SongPlayingFragment.Statified.songTitle
import com.example.music.fragments.SongPlayingFragment.Statified.startTimeText
import com.example.music.utils.CurrentSongHelper
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt

class SongPlayingFragment : Fragment()
{

    @SuppressLint("StaticFieldLeak")
    object Statified {

        var mSensorManager: SensorManager? = null
        var mSensorListener: SensorEventListener? = null

        var MY_PREFS_NAME = "ShakeFeature"
        var MY_PREFS_SHUFFLE = "ShuffleSave"
        var MY_PREFS_LOOP = "LoopSave"
        var seekbar: SeekBar? = null
        var mediaPlayer: MediaPlayer? = null
        var fetchSongs: ArrayList<Songs>? = arrayListOf()
        var currentTrackHelper: String? = null
        var favouriteContent: EchoDatabase? = null
        var currentSongHelper = CurrentSongHelper()
        var currentPosition: Int = 0
        var fab: ImageButton? = null
        var activity: Activity? = null
        var songArtist: TextView? = null
        var songTitle: TextView? = null
        var startTimeText: TextView? = null
        var endTimeText: TextView? = null
        var playpauseImageButton: ImageButton? = null
        var rewindImageButton: ImageButton? = null
        var fastforwardImageButton: ImageButton? = null
        var loopImageButton: ImageButton? = null
        var shuffleImageButton: ImageButton? = null
        var playlistImageButton: ImageButton? = null
        var _songTitle: String? = null
        var _songArtist: String? = null
        var UpdateSongTime = object : Runnable {
            override fun run() {
                val getCurrent = mediaPlayer?.currentPosition
                startTimeText!!.text = String.format("%d:%d",
                    TimeUnit.MILLISECONDS.toMinutes(getCurrent?.toLong()!!),
                    TimeUnit.MILLISECONDS.toSeconds(getCurrent.toLong()) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(getCurrent.toLong())))
                seekbar?.progress = getCurrent.toInt()

                Handler().postDelayed(this, 1000)
            }
        }

    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        Statified.activity = context as Activity?
    }

    override fun onAttach(activity: Activity?) {
        super.onAttach(activity)
        Statified.activity = activity
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.player_main, container, false)
        activity?.title = "Now playing"
        currentSongHelper.isLoop = false
        currentSongHelper.isShuffle = false
        currentSongHelper.isPlaying = true
        seekbar = view?.findViewById(R.id.sBar) as SeekBar
        startTimeText = view.findViewById(R.id.time_start) as TextView
        endTimeText = view.findViewById(R.id.time_end) as TextView
        playpauseImageButton = view.findViewById(R.id.play) as ImageButton
        fastforwardImageButton = (view.findViewById(R.id.next) as ImageButton)
        rewindImageButton = (view.findViewById(R.id.prev) as ImageButton)
        loopImageButton = (view.findViewById(R.id.repeat) as ImageButton)
        loopImageButton?.setBackgroundResource(R.drawable.ic_repeat_none)
        shuffleImageButton = (view.findViewById(R.id.shuffle) as ImageButton)
        shuffleImageButton?.setBackgroundResource(R.drawable.ic_shuffle_none)
        playlistImageButton = (view.findViewById(R.id.playlist) as ImageButton)

        fab = view.findViewById(R.id.favorite) as ImageButton

        songArtist = view.findViewById(R.id.song_name) as TextView
        songTitle = view.findViewById(R.id.artist) as TextView
        fab?.alpha = 0.8f
        val imm = activity?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(activity?.currentFocus?.windowToken, 0)

        return view
    }

    var mAcceleration: Float = 0f
    var mAccelerationCurrent: Float = 0f
    var mAccelerationLast: Float = 0f

    override fun onResume() {
        try {
            super.onResume()
            if (mediaPlayer?.isPlaying as Boolean) {
                currentSongHelper.isPlaying = true
                playpauseImageButton?.setBackgroundResource(R.drawable.ic_pause)
            } else {
                currentSongHelper.isPlaying = false
                playpauseImageButton?.setBackgroundResource(R.drawable.ic_play)
            }
            Statified.mSensorManager?.registerListener(Statified.mSensorListener,
                Statified.mSensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL)
        }
        catch (e:java.lang.Exception){
            e.printStackTrace()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        Statified.mSensorManager = Statified.activity?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAcceleration = 0.0f
        mAccelerationCurrent = SensorManager.GRAVITY_EARTH
        mAccelerationLast = SensorManager.GRAVITY_EARTH
        bindShakeListener()
    }

    override fun onPause() {
        super.onPause()
        Statified.mSensorManager?.unregisterListener(Statified.mSensorListener)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        try {
            super.onActivityCreated(savedInstanceState)
            favouriteContent = EchoDatabase(activity)
            var path: String? = null

            var songId: Long = 0
            try {
                path = arguments?.getString("path")
                currentPosition = arguments?.getInt("songPosition") as Int
                _songTitle = arguments?.getString("songTitle")
                _songArtist = arguments?.getString("songArtist")
                fetchSongs = arguments?.getParcelableArrayList("songsData")
                songId = arguments?.getInt("SongId")?.toLong() as Long
                if (_songArtist.equals("<unknown>", true)) {
                    _songArtist = "unknown"
                }
                currentSongHelper.songArtist = _songArtist
                currentSongHelper.songTitle = _songTitle
                currentSongHelper.songPath = path
                currentSongHelper.currentPosition = currentPosition
                currentSongHelper.songId = songId

            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (favouriteContent?.checkifIdExists(currentSongHelper.songId.toInt()) as Boolean) {
                fab?.setImageDrawable(ContextCompat.getDrawable(activity as Context, R.drawable.ic_fav_on))
            }
            songArtist?.text = currentSongHelper.songArtist
            songTitle?.text = currentSongHelper.songTitle
            Statified.currentTrackHelper = currentSongHelper.songTitle

            val fromBottomBar = arguments?.get("BottomBar") as? String
            val fromfavBottomBar = arguments?.get("FavBottomBar") as? String
            if (fromBottomBar != null) {
                mediaPlayer = MainScreenFragment.Statified.mMediaPlayer
            } else if (fromfavBottomBar != null) {
                mediaPlayer = FavoriteFragment.Statified.meediaPlayer
            } else {
                mediaPlayer = MediaPlayer()
                mediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)
                try {
                    if(path!=null){
                        mediaPlayer?.setDataSource(activity, Uri.parse(path))
                        mediaPlayer?.prepare()
                    }
                    else{
                        Toast.makeText(activity, "Something went wrong, Play song from Main List", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                mediaPlayer?.start()
            }
            if (mediaPlayer?.isPlaying as Boolean) {
                playpauseImageButton?.setBackgroundResource(R.drawable.ic_pause)
            } else {
                playpauseImageButton?.setBackgroundResource(R.drawable.ic_play)
            }
            Staticated.processInformation(mediaPlayer as MediaPlayer)
            clickHandler()

            mediaPlayer?.setOnCompletionListener {
                Staticated.on_song_complete()
            }
            var prefs = activity?.getSharedPreferences(MY_PREFS_SHUFFLE, Context.MODE_PRIVATE)
            var isShuffleAllowed = prefs?.getBoolean("feature", false)
            if (isShuffleAllowed as Boolean) {
                currentSongHelper.isShuffle = true
                currentSongHelper.isLoop = false
                shuffleImageButton?.setBackgroundResource(R.drawable.ic_shuffle_all)
                loopImageButton?.setBackgroundResource(R.drawable.ic_repeat_none)
            } else {
                shuffleImageButton?.setBackgroundResource(R.drawable.ic_shuffle_none)
                currentSongHelper.isShuffle = false
            }
            var prefsforLoop = activity?.getSharedPreferences(MY_PREFS_LOOP, Context.MODE_PRIVATE)
            var isLoopAllowed = prefsforLoop?.getBoolean("feature", false)
            if (isLoopAllowed as Boolean) {
                currentSongHelper.isLoop = true
                currentSongHelper.isShuffle = false
                loopImageButton?.setBackgroundResource(R.drawable.ic_repeat_all)
                shuffleImageButton?.setBackgroundResource(R.drawable.ic_shuffle_none)

            } else {
                currentSongHelper.isLoop = false
                loopImageButton?.setBackgroundResource(R.drawable.ic_repeat_none)
            }
        }
        catch (e:java.lang.Exception){
            e.printStackTrace()
        }
    }

    object Staticated {

        fun on_song_complete() {
            try {
                if (!(currentSongHelper.isShuffle)) {
                    if (currentSongHelper.isLoop) {
                        currentSongHelper.isPlaying = true
                        var nextSong = fetchSongs?.get(currentPosition)
                        SongPlayingFragment.Statified.currentTrackHelper = nextSong?.songTitle

                        if (nextSong?.artist.equals("<unknown>", true)) {
                            currentSongHelper.songArtist = "unknown"
                        } else {
                            currentSongHelper.songArtist = nextSong?.artist
                        }

                        currentSongHelper.songTitle = nextSong?.songTitle
                        currentSongHelper.songPath = nextSong?.songData
                        currentSongHelper.currentPosition = currentPosition
                        currentSongHelper.songId = nextSong?.songID as Long
                        if (favouriteContent?.checkifIdExists(currentSongHelper.songId.toInt()) as Boolean) {
                            fab?.setImageDrawable(ContextCompat.getDrawable(activity as Context, R.drawable.ic_fav_on))
                        }
                        mediaPlayer?.reset()
                        try {
                            mediaPlayer?.setDataSource(activity, Uri.parse(nextSong.songData))
                            mediaPlayer?.prepare()
                            mediaPlayer?.start()
                            songArtist?.text = nextSong.artist
                            songTitle?.text = nextSong.songTitle
                            processInformation(mediaPlayer as MediaPlayer)

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        currentSongHelper.isPlaying = true
                        SongPlayingFragment.Staticated.playNext("PlayNextNormal")
                    }
                } else {
                    currentSongHelper.isPlaying = true
                    playNext("playNextLikeNormalShuffle")
                }
            }
            catch (e:java.lang.Exception){
                e.printStackTrace()
            }
        }

        fun playNext(check: String) {
            try{
                if (!(currentSongHelper.isPlaying as Boolean)) {
                    playpauseImageButton?.setBackgroundResource(R.drawable.ic_play)
                } else {
                    playpauseImageButton?.setBackgroundResource(R.drawable.ic_pause)
                }
                if (check.equals("PlayNextNormal", true)) {

                    currentPosition += 1
                    if (currentPosition == fetchSongs?.size) {
                        currentPosition = 0
                    }

                } else if (check.equals("playNextLikeNormalShuffle", true)) {
                    var randomObject = Random()
                    var Randomposition = randomObject.nextInt((fetchSongs?.size)?.plus(1) as Int)
                    currentSongHelper.isLoop = false
                    currentPosition = Randomposition
                    if (currentPosition == fetchSongs?.size) {
                        currentPosition = 0
                    }
                }
                var nextSong = fetchSongs?.get(currentPosition)
                Statified.currentTrackHelper = nextSong?.songTitle
                if (nextSong?.artist.equals("<unknown>", true)) {
                    currentSongHelper.songArtist = "unknown"
                } else {
                    currentSongHelper.songArtist = nextSong?.artist
                }
                currentSongHelper.songTitle = nextSong?.songTitle
                currentSongHelper.songPath = nextSong?.songData
                currentSongHelper.currentPosition = currentPosition
                currentSongHelper.songId = nextSong?.songID as Long

                try {
                    if (favouriteContent?.checkifIdExists(currentSongHelper.songId.toInt()) as Boolean) {
                        fab?.setImageDrawable(ContextCompat.getDrawable(activity as Activity, R.drawable.ic_fav_on))
                    } else {
                        fab?.setImageDrawable(ContextCompat.getDrawable(activity as Activity, R.drawable.ic_favorite))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                mediaPlayer?.reset()
                try {
                    mediaPlayer?.setDataSource(activity, Uri.parse(currentSongHelper.songPath))
                    mediaPlayer?.prepare()
                    mediaPlayer?.start()
                    if (nextSong.artist.equals("<unknown>", true)) {
                        songArtist?.text = "unknown"
                    } else {
                        songArtist?.text = nextSong.artist
                    }
                    songTitle?.text = nextSong.songTitle
                    processInformation(mediaPlayer as MediaPlayer)

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            catch(e:Exception)
            {
                e.printStackTrace()
            }
        }

        fun processInformation(mediaPlayer: MediaPlayer) {
            try {
                val finalTime = mediaPlayer.duration
                val startTime = mediaPlayer.currentPosition
                seekbar?.max = finalTime
                startTimeText?.text = String.format("%d:%d",
                    TimeUnit.MILLISECONDS.toMinutes(startTime.toLong()),
                    TimeUnit.MILLISECONDS.toSeconds(startTime.toLong()) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(startTime.toLong())))
                endTimeText?.text = String.format("%d:%d",
                    TimeUnit.MILLISECONDS.toMinutes(finalTime.toLong()),
                    TimeUnit.MILLISECONDS.toSeconds(finalTime.toLong()) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(finalTime.toLong())))
                seekbar?.progress = startTime
                Handler().postDelayed(UpdateSongTime, 1000)
            }
            catch (e:java.lang.Exception){
                e.printStackTrace()
            }
        }
    }


    fun bindShakeListener() {
        try {
            Statified.mSensorListener = object : SensorEventListener {
                override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
                }

                override fun onSensorChanged(p0: SensorEvent) {
                    val x = p0.values[0]
                    val y = p0.values[1]
                    val z = p0.values[2]

                    mAccelerationLast = mAccelerationCurrent
                    mAccelerationCurrent = sqrt(((x * x + y * y + z * z).toDouble())).toFloat()
                    val delta = mAccelerationCurrent - mAccelerationLast
                    mAcceleration = mAcceleration * 0.9f + delta

                    if (mAcceleration > 12) {
                        val prefs = Statified.activity?.getSharedPreferences(Statified.MY_PREFS_NAME, Context.MODE_PRIVATE)
                        val isAllowed = prefs?.getBoolean("feature", false)


                        Statified.currentSongHelper.isPlaying = true
                        Statified.playpauseImageButton?.setBackgroundResource(R.drawable.ic_pause)
                        var editorLoop = Statified.activity?.getSharedPreferences(Statified.MY_PREFS_LOOP, Context.MODE_PRIVATE)?.edit()
                        if (Statified.currentSongHelper.isLoop) {
                            Statified.currentSongHelper.isLoop = false
                            editorLoop?.putBoolean("feature", false)
                            editorLoop?.apply()
                            Statified.loopImageButton?.setBackgroundResource(R.drawable.ic_repeat_none)
                            Toast.makeText(Statified.activity, "Loop Disabled", Toast.LENGTH_SHORT).show()
                        }

                        if (isAllowed as Boolean) {
                            if (Statified.currentSongHelper.isShuffle) {
                                Staticated.playNext("PlayNextLikeNormalShuffle")
                            } else {
                                Staticated.playNext("PlayNextNormal")
                            }
                        }
                    }

                }

            }
        }
        catch (e:java.lang.Exception){
            e.printStackTrace()
        }
    }


    private fun clickHandler() {

        try {
            fab?.setOnClickListener {

                if (favouriteContent?.checkifIdExists(currentSongHelper.songId.toInt()) as Boolean) {
                    favouriteContent?.deleteFavourite(currentSongHelper.songId.toInt())
                    Toast.makeText(Statified.activity, "Removed from favorites", Toast.LENGTH_SHORT).show()
                    fab?.setImageDrawable(ContextCompat.getDrawable(activity as Context, R.drawable.ic_favorite))

                } else {
                    Toast.makeText(Statified.activity, "Added to favorites", Toast.LENGTH_SHORT).show()
                    favouriteContent?.storeasFavourite(currentSongHelper.songId.toInt(), currentSongHelper.songArtist,
                        currentSongHelper.songTitle, currentSongHelper.songPath)
                    fab?.setImageDrawable(ContextCompat.getDrawable(activity as Context, R.drawable.ic_fav_on))

                }
            }


            shuffleImageButton?.setOnClickListener {

                val editorShuffle = Statified.activity?.getSharedPreferences(MY_PREFS_SHUFFLE, Context.MODE_PRIVATE)?.edit()
                val editorLoop = Statified.activity?.getSharedPreferences(MY_PREFS_LOOP, Context.MODE_PRIVATE)?.edit()
                if (currentSongHelper.isShuffle) {
                    shuffleImageButton?.setBackgroundResource(R.drawable.ic_shuffle_none)
                    currentSongHelper.isShuffle = false
                    editorShuffle?.putBoolean("feature", false)
                    editorShuffle?.apply()
                } else {
                    currentSongHelper.isShuffle = true
                    currentSongHelper.isLoop = false
                    shuffleImageButton?.setBackgroundResource(R.drawable.ic_shuffle_all)
                    loopImageButton?.setBackgroundResource(R.drawable.ic_repeat_none)
                    editorShuffle?.putBoolean("feature", true)
                    editorShuffle?.apply()
                    editorLoop?.putBoolean("feature", false)
                    editorLoop?.apply()
                }

            }
            fastforwardImageButton?.setOnClickListener {
                currentSongHelper.isPlaying = true
                if (currentSongHelper.isShuffle) {
                    Staticated.playNext("playNextLikeNormalShuffle")
                } else {
                    Staticated.playNext("PlayNextNormal")
                }
            }
            rewindImageButton?.setOnClickListener {
                currentSongHelper.isPlaying = true
                if (currentSongHelper.isLoop) {
                    loopImageButton?.setBackgroundResource(R.drawable.ic_repeat_none)
                }
                currentSongHelper.isLoop = false
                playPrevious()
            }
            loopImageButton?.setOnClickListener {

                val editorLoop = Statified.activity?.getSharedPreferences(MY_PREFS_LOOP, Context.MODE_PRIVATE)?.edit()
                val editorShuffle = Statified.activity?.getSharedPreferences(MY_PREFS_SHUFFLE, Context.MODE_PRIVATE)?.edit()
                if (currentSongHelper.isLoop) {
                    currentSongHelper.isLoop = false
                    loopImageButton?.setBackgroundResource(R.drawable.ic_repeat_none)
                    editorLoop?.putBoolean("feature", false)
                    editorLoop?.apply()
                } else {
                    currentSongHelper.isLoop = true
                    currentSongHelper.isShuffle = false
                    loopImageButton?.setBackgroundResource(R.drawable.ic_repeat_all)
                    shuffleImageButton?.setBackgroundResource(R.drawable.ic_shuffle_none)
                    editorLoop?.putBoolean("feature", true)
                    editorLoop?.apply()
                    editorShuffle?.putBoolean("feature", false)
                    editorShuffle?.apply()
                }

            }
            playpauseImageButton?.setOnClickListener {
                if (mediaPlayer?.isPlaying as Boolean) {
                    currentSongHelper.isPlaying = true
                    playpauseImageButton?.setBackgroundResource(R.drawable.ic_play)
                    mediaPlayer?.pause()
                } else {
                    currentSongHelper.isPlaying = false
                    playpauseImageButton?.setBackgroundResource(R.drawable.ic_pause)
                    mediaPlayer?.seekTo(seekbar?.progress as Int)
                    mediaPlayer?.start()
                }
            }

            seekbar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBarget: SeekBar?) {
                    seekbar?.progress = seekbar?.progress as Int
                    mediaPlayer?.seekTo(seekbar?.progress as Int)
                }
            })

            playlistImageButton?.setOnClickListener{
                activity?.onBackPressed()
            }
        }
        catch (e : java.lang.Exception){
            e.printStackTrace()
        }
    }

    private fun playPrevious() {
        try {
            currentPosition -= 1
            if (currentPosition == -1) {
                currentPosition = 0
            }
            if (currentSongHelper.isPlaying as Boolean) {
                playpauseImageButton?.setBackgroundResource(R.drawable.ic_pause)
            } else {
                playpauseImageButton?.setBackgroundResource(R.drawable.ic_play)
            }
            val nextSong = fetchSongs?.get(currentPosition)

            currentSongHelper.songTitle = nextSong?.songTitle
            currentSongHelper.songPath = nextSong?.songData

            currentSongHelper.songId = nextSong?.songID as Long

            currentSongHelper.currentPosition = currentPosition
            SongPlayingFragment.Statified.currentTrackHelper = currentSongHelper.songTitle

            if (nextSong.artist.equals("<unknown>", true)) {
                currentSongHelper.songArtist = "unknown"
            } else {
                currentSongHelper.songArtist = nextSong.artist
            }
            try {
                if (favouriteContent?.checkifIdExists(currentSongHelper.songId.toInt()) as Boolean) {
                    fab?.setImageDrawable(ContextCompat.getDrawable(activity as Context, R.drawable.ic_fav_on))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mediaPlayer?.reset()
            try {
                mediaPlayer?.setDataSource(activity, Uri.parse(nextSong.songData))
                mediaPlayer?.prepare()
                mediaPlayer?.start()

                songArtist?.text = nextSong.artist
                songTitle?.text = nextSong.songTitle
                Staticated.processInformation(mediaPlayer as MediaPlayer)
            } catch (e: IOException) {
                e.printStackTrace()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}