package com.example.music.fragments

import android.app.Activity
import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.provider.MediaStore
import android.support.constraint.ConstraintLayout
import android.support.v4.app.Fragment
import android.support.v7.widget.CardView
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.*
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import com.example.music.R
import com.example.music.adapters.MainScreenAdapter
import com.example.music.data.Songs
import com.example.music.utils.CurrentSongHelper
import java.util.*
import kotlin.collections.ArrayList
import com.example.music.R.id.action_search
import com.example.music.R.id.action_sort_recent


class MainScreenFragment : Fragment() {

    var getSongsList: ArrayList<Songs>? = null
    var nowPlayingBottomBar: CardView? = null
    var playPauseButton: ImageButton? = null
    var playPauseHelper= CurrentSongHelper()
    var songTitle: TextView? = null
    var recyclerView: RecyclerView? = null
    var _mainScreenAdapter: MainScreenAdapter? = null
    var visibleLayout: ConstraintLayout? = null
    var noSongs: RelativeLayout? = null

    var myActivity: Activity? = null

    object Statified {
        var mMediaPlayer: MediaPlayer? = null
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        myActivity = context as Activity
    }

    override fun onAttach(activity: Activity?) {
        super.onAttach(activity)
        myActivity = activity
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        playPauseHelper = CurrentSongHelper()
        val prefs = activity?.getSharedPreferences("action_sort", Context.MODE_PRIVATE)
        val action_sort_ascending = prefs?.getString("action_sort_ascending", "true")
        val action_sort_recent = prefs?.getString("action_sort_recent", "false")
        if (getSongsList != null) {
            if (action_sort_ascending!!.equals("true", ignoreCase = true)) {
                Collections.sort(getSongsList, Songs.Statified.nameComparator)
                _mainScreenAdapter?.notifyDataSetChanged()
            } else if (action_sort_recent!!.equals("true", ignoreCase = true)) {
                Collections.sort(getSongsList, Songs.Statified.dateComparator)
                _mainScreenAdapter?.notifyDataSetChanged()
            }
        }
        bottomBar_setup()
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_main, container, false)
        setHasOptionsMenu(true)
        activity?.title = "All songs"
        visibleLayout = (view?.findViewById(R.id.visible_layout) as ConstraintLayout)
        noSongs = view.findViewById(R.id.noSongs) as RelativeLayout
        nowPlayingBottomBar = view.findViewById(R.id.bottomBar) as CardView
        songTitle = view.findViewById(R.id.textView) as TextView
        playPauseButton = view.findViewById(R.id.imageButton5) as ImageButton
        playPauseButton?.setBackgroundResource(R.drawable.ic_play_small)
        (nowPlayingBottomBar as CardView).isClickable = false
        recyclerView = view.findViewById(R.id.mainRecyclerView) as RecyclerView


        playPauseHelper?.isPlaying = false
        //make the no songs layout visible
        if (getSongsList == null) {
            getSongsList = getSongsfromPhone()
            if (getSongsList == null) {
                visibleLayout?.visibility = View.INVISIBLE
                noSongs?.visibility = View.VISIBLE
            }
        } else {
            Log.d(MainScreenFragment::class.java.simpleName, " Data already there")
        }

        _mainScreenAdapter = MainScreenAdapter(getSongsList as ArrayList<Songs>, activity)
        val mLayoutManager = LinearLayoutManager(activity)
        (recyclerView as RecyclerView).layoutManager = mLayoutManager
        (recyclerView as RecyclerView).itemAnimator = DefaultItemAnimator()
        (recyclerView as RecyclerView).adapter = _mainScreenAdapter
        return view

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainScreen", " onCreate")
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainScreen", " onResume")
    }


    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        menu!!.clear()
        inflater!!.inflate(R.menu.main, menu)
        return
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val switcher = item?.itemId
        if (switcher == R.id.action_sort_ascending) {
            val editor = myActivity?.getSharedPreferences("action_sort", Context.MODE_PRIVATE)?.edit()
            editor?.putString("action_sort_ascending", "true")
            editor?.putString("action_sort_recent", "false")
            editor?.apply()
            if (getSongsList != null) {
                Collections.sort(getSongsList, Songs.Statified.nameComparator)
            }
            _mainScreenAdapter?.notifyDataSetChanged()
            return false
        } else if (switcher == action_sort_recent) {
            if (getSongsList != null) {
                Collections.sort(getSongsList, Songs.Statified.dateComparator)
            }
            _mainScreenAdapter?.notifyDataSetChanged()
            val editortwo = myActivity?.getSharedPreferences("action_sort", Context.MODE_PRIVATE)?.edit()
            editortwo?.putString("action_sort_recent", "true")
            editortwo?.putString("action_sort_ascending", "false")
            editortwo?.apply()
            return false
        }
        else if(switcher == action_search){
            val searchFragment = SearchFragment()
            fragmentManager?.beginTransaction()
                ?.replace(R.id.flContent, searchFragment)
                ?.commit()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getSongsfromPhone(): ArrayList<Songs> {
        val arrayList = ArrayList<Songs>()
        val contentResolver = activity?.contentResolver
        val songUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val songCursor = contentResolver?.query(songUri, null, null, null, null)
        try {
            if (songCursor != null && songCursor.moveToFirst()) {
                val songId = songCursor.getColumnIndex(MediaStore.Audio.Media._ID)
                val songTitle = songCursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val songArtist = songCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val songPathInt = songCursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val dateIndex = songCursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                while (songCursor.moveToNext()) {
                    val currentId = songCursor.getLong(songId)
                    val currentTitle = songCursor.getString(songTitle)
                    val currentArtist = songCursor.getString(songArtist)
                    val fullpath = songCursor.getString(songPathInt)
                    val dateadded = songCursor.getLong(dateIndex)
                    arrayList.add(Songs(currentId, currentTitle, currentArtist, fullpath, dateadded))
                }
            }
        }
        catch(e:Exception){}
        return arrayList
    }

    fun bottomBar_setup() {
        nowPlayingBottomBar?.isClickable = false
        bottomBarOnClickHandlers()
        try {
            songTitle?.text = SongPlayingFragment.Statified.currentTrackHelper
            SongPlayingFragment.Statified.mediaPlayer?.setOnCompletionListener {
                songTitle?.text = SongPlayingFragment.Statified.currentTrackHelper
                SongPlayingFragment.Staticated.on_song_complete()
            }
            if (SongPlayingFragment.Statified.mediaPlayer?.isPlaying as Boolean) {
                playPauseHelper.isPlaying = true
                nowPlayingBottomBar?.visibility = View.VISIBLE
            } else {
                playPauseHelper.isPlaying = false

            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun bottomBarOnClickHandlers() {

        nowPlayingBottomBar?.setOnClickListener {
            try {
                val fr = SongPlayingFragment()
                FavoriteFragment.Statified.meediaPlayer = SongPlayingFragment.Statified.mediaPlayer
                val _fetch_from_Songs_Fragment = SongPlayingFragment.Statified.fetchSongs
                val bundle_to_be_sent = Bundle()
                bundle_to_be_sent.putString("songTitle", SongPlayingFragment.Statified.currentSongHelper.songTitle)
                bundle_to_be_sent.putString("songArtist", SongPlayingFragment.Statified.currentSongHelper.songArtist)
                bundle_to_be_sent.putInt("songPosition", SongPlayingFragment.Statified.currentSongHelper.currentPosition)
                bundle_to_be_sent.putInt("SongId", (SongPlayingFragment.Statified.currentSongHelper).songId.toInt())
                bundle_to_be_sent.putString("FavBottomBar", "success")
                bundle_to_be_sent.putParcelableArrayList("songsData", _fetch_from_Songs_Fragment)

                fr.arguments = bundle_to_be_sent
                fragmentManager?.beginTransaction()
                    ?.replace(R.id.flContent, fr)
                    ?.addToBackStack("SongPlayingFragment")
                    ?.commit()

            } catch (e: Exception) {
                Toast.makeText(myActivity, "Something went wrong", Toast.LENGTH_SHORT).show()
            }
        }
        playPauseButton?.setOnClickListener {
            if (playPauseHelper.isPlaying as Boolean) {
                playPauseHelper.isPlaying = false

                playPauseButton?.setBackgroundResource(R.drawable.ic_play_small)
                SongPlayingFragment.Statified.mediaPlayer?.pause()
                playPauseHelper.TrackPosition = SongPlayingFragment.Statified.mediaPlayer?.getCurrentPosition() as Int
            } else {
                SongPlayingFragment.Statified.mediaPlayer?.seekTo(playPauseHelper.TrackPosition)
                SongPlayingFragment.Statified.mediaPlayer?.start()
                playPauseHelper.isPlaying = true
                playPauseButton?.setBackgroundResource(R.drawable.ic_pause_small)
            }
        }

    }

}