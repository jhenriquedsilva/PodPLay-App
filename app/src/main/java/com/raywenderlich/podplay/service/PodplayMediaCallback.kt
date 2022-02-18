package com.raywenderlich.podplay.service

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log


class PodplayMediaCallback(
    val context: Context,
    val mediaSession: MediaSessionCompat,
    var mediaPlayer: MediaPlayer? = null
): MediaSessionCompat.Callback() {

    val TAG = "PodplayMediaCallback"
    // Sets the current state of the media session
    // Method used to update the state as playback
    // commands are processed
    private fun setState(state: Int) {
        var position: Long = -1

        val playbackState = PlaybackStateCompat.Builder()
            // Specifies all the states the Media Session will allow
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_STOP or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_PAUSE
            )
            .setState(state,position,1.0f)
            .build()

        // If there is a state change, this method should be called
        mediaSession.setPlaybackState(playbackState)
    }


    override fun onPlayFromUri(uri: Uri?, extras: Bundle?) {
        super.onPlayFromUri(uri, extras)
        println("Playing ${uri.toString()}")
        onPlay()
        // If there is a state change, this should ba called
        mediaSession.setMetadata(
            // Describes the material that is playing
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, uri.toString())
                .build()
        )
    }


    // Should call startService()
    override fun onPlay() {
        super.onPlay()
        Log.d(TAG,"onPlay called")
        setState(PlaybackStateCompat.STATE_PLAYING)
    }

    // Should call stopSelf()
    override fun onStop() {
        super.onStop()
        println("onStop called")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG,"onPause called")
        setState(PlaybackStateCompat.STATE_PAUSED)
    }

}