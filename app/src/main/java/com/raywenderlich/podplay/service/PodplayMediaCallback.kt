package com.raywenderlich.podplay.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log


class PodplayMediaCallback(
    val context: Context,
    private val mediaSession: MediaSessionCompat,
    private var mediaPlayer: MediaPlayer? = null
): MediaSessionCompat.Callback() {

    private val TAG = "Testing"
    // Keeps track of the currently playing media item
    private var mediaUri: Uri? = null
    // Indicates if the item is new
    private var newMedia: Boolean = false
    // Keeps track of the media information passed into onPlayFromUri()
    private var mediaExtras: Bundle? = null
    // Used to store an audio focus request when running Android 8.0 ot higher
    private var focusRequest: AudioFocusRequest? = null

    // Makes sure that the app has audio focus
    private fun hasAudioFocus(): Boolean {

        // That's the audio manager system service
        val audioManager = this.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // How to request audio focus in newer versions of Android

            /**                                            // Want to gain focus
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .run {

                    setAudioAttributes(
                            AudioAttributes.Builder()
                                .run {
                                    setUsage(AudioAttributes.USAGE_MEDIA)
                                    setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                    build()
                                }
                        )
                    build()
                }
            */
            // I removed "run" because it returns the last line. So it is possible
            // to do the same without it. Any problems, go back to the piece of code above

            // The parameter to the constructor informs that the app wants to gain focus
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                // Attributes inform which type of audio you're going to play
                .setAudioAttributes(
                        AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                    )
                .build()
            // The property is set to that request
            this.focusRequest = focusRequest
            // Makes the request
            val result = audioManager.requestAudioFocus(focusRequest)
            // True is returned if the focus was granted
            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED

        } else {
            // How to request audio focus in older versions of Android
            val result = audioManager.requestAudioFocus(
                null,
                // Content type in Android 8
                AudioManager.STREAM_MUSIC,
                // focusGain in Android 8
                AudioManager.AUDIOFOCUS_GAIN
            )
            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    // This method is called when the audio playback is paused or stopped
    private fun removeAudioFocus() {
        val audioManager = this.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { focusRequest ->
                audioManager.abandonAudioFocusRequest(focusRequest)
            }

        } else {
            audioManager.abandonAudioFocus(null)
        }
    }

    // Stores a new media item and set the metadata on the media session
    private fun setNewMedia(uri: Uri?) {
        newMedia = true
        mediaUri = uri
    }
    // Sets the current state of the media session
    // Method used to update the state as playback
    // commands are processed
    private fun setState(state: Int) {
        var position: Long = -1

        mediaPlayer?.let { mediaPlayer ->
            position = mediaPlayer.currentPosition.toLong()
        }

        val playbackState = PlaybackStateCompat.Builder()
            // Specifies all the states the Media Session will allow
            .setActions(
                // The valid controller actions
                // that can be handled in the present state
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_STOP or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_PAUSE
            )
            .setState(state, position,1.0f)
            .build()

        // If there is a state change, this method should be called
        mediaSession.setPlaybackState(playbackState)
    }

    private fun initializeMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
            // When the playback is finished, this code is executed
            mediaPlayer?.setOnCompletionListener { setState(PlaybackStateCompat.STATE_PAUSED) }
        }
    }

    private fun prepareMedia() {
        // Only the data is new
        if (newMedia) {
            newMedia = false
            mediaPlayer?.let { mediaPlayer ->
                mediaUri?.let { mediaUri ->
                    // Back to uninitialized state
                    mediaPlayer.reset()
                    // The media that will be played
                    mediaPlayer.setDataSource(context, mediaUri)
                    // Puts media player on an initialized state
                    // Blocks until media player is ready for playback
                    mediaPlayer.prepare()
                    // Everytime the metadata changes,
                    // you should set it again to the media session
                    mediaSession.setMetadata(
                        MediaMetadataCompat.Builder()
                            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, mediaUri.toString())
                            .build()
                    )
                }
            }
        }
    }

    // Start the playback of an audio media
    private fun startPlaying() {
        mediaPlayer?.let { mediaPlayer ->
            if (!mediaPlayer.isPlaying) {
                mediaPlayer.start()
                setState(PlaybackStateCompat.STATE_PLAYING)
            }
        }
    }

    private fun pausePlaying() {
        // First thing to do is removing ths audio focus
        removeAudioFocus()
        mediaPlayer?.let { mediaPlayer ->
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                setState(PlaybackStateCompat.STATE_PAUSED)
            }
        }
    }

    private fun stopPlaying() {
        removeAudioFocus()
        mediaSession.isActive = false
        mediaPlayer?.let { mediaPlayer ->
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
                setState(PlaybackStateCompat.STATE_STOPPED)
            }
        }
    }

    override fun onPlayFromUri(uri: Uri?, extras: Bundle?) {
        super.onPlayFromUri(uri, extras)
        // println("Playing ${uri.toString()}")
        if (mediaUri == uri) {
            newMedia = false
            mediaExtras = null
        } else {
            mediaExtras = extras
            setNewMedia(uri)
        }
        onPlay()
        // If there is a state change, this should ba called
        /*
        mediaSession.setMetadata(
            // Describes the material that is playing
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, uri.toString())
                .build()
        )
        */
        Log.d(TAG, "ONPLAYFROMURI CALLED")
    }


    // Should call startService()
    // These are only callbacks.
    // The actual logic to play the podcasts is my responsibility
    override fun onPlay() {
        super.onPlay()

        if (hasAudioFocus()) {
            mediaSession.isActive = true
            initializeMediaPlayer()
            // Media is prepared for playback
            prepareMedia()
            // Media player start playing
            startPlaying()
        }

        Log.d(TAG, "ONPLAY CALLED")
    }

    override fun onPause() {
        super.onPause()
        pausePlaying()
        Log.d(TAG, "ONPAUSE CALLED")
    }

    // Should call stopSelf()
    override fun onStop() {
        super.onStop()
        stopPlaying()

        Log.d(TAG, "ONSTOP CALLED")
    }


}