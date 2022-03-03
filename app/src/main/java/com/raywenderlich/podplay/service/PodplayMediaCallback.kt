package com.raywenderlich.podplay.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import java.lang.Exception

// Used to get information about the playback
class PodplayMediaCallback(
    val context: Context,
    private val mediaSession: MediaSessionCompat,
): MediaSessionCompat.Callback() {

    // Methods that the MediaBrowserService will execute
    interface PodplayMediaListener {
        fun onStateChanged()
        fun onStopPlaying()
        fun onPausePLaying()
    }

    private val TAG = "Testing"
    // Keeps track of the currently playing media item
    private var mediaPlayer: MediaPlayer? = null
    // This is the MediaBrowserService
    var listener: PodplayMediaListener? = null

    private var mediaUri: Uri? = null
    // Indicates if the item is new
    private var newMedia: Boolean = false
    // Keeps track of the media information passed into onPlayFromUri()
    private var mediaExtras: Bundle? = null
    // Used to store an audio focus request when running Android 8.0 ot higher
    private var focusRequest: AudioFocusRequest? = null
    // It is set to true if the mediaPlayer is created by PodplayMediaCallback
    private var mediaNeedsPrepare: Boolean = false

    // Makes sure that the app has audio focus
    private fun hasAudioFocus(): Boolean {

        // That's the audio manager system service
        val audioManager = this.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // How to request audio focus in newer versions of Android

                                                     // Want to gain focus
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
            /**
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
            */
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

    override fun onPlayFromUri(uri: Uri, extras: Bundle) {
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
        Log.d(TAG, "ONPLAYFROMURI CALLED")
    }

    // Sets the current state of the media session
    // Method used to update the state as playback
    // commands are processed
    private fun setState(state: Int, newSpeed:  Float? = null) {
        var position: Long = -1

        mediaPlayer?.let { mediaPlayer ->
            position = mediaPlayer.currentPosition.toLong()
        }

        // Default speed
        var speed = 1.0f

        // Speed control was possible from Android Marshmallow
        // So the code is executed only in this circumstances
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (newSpeed == null) {
                // Speed keeps the same if a new one is not supplied
                speed = mediaPlayer?.playbackParams?.speed ?: 1.0f
            } else {
                speed = newSpeed
            }
            mediaPlayer?.let { mediaPlayer ->
                // The assignment can throw an exception.
                // So it should be surrounded by a try block
                try {
                    // Is it necessary to do an assignment? Yes, it is
                    // If you do not assign a new object, the speed will not change
                    mediaPlayer.playbackParams = mediaPlayer.playbackParams.setSpeed(speed)
                } catch (e: Exception) {
                    // If an exception occurs, the player needs to be reset to clear the state
                    mediaPlayer.reset()
                    // The data source should be set again
                    mediaUri?.let { mediaUri ->
                        mediaPlayer.setDataSource(this.context, mediaUri)
                    }
                    mediaPlayer.prepare()
                    // Update the playbackParams again
                    mediaPlayer.playbackParams = mediaPlayer.playbackParams.setSpeed(speed)
                    // When the player is reset, its position go back to 0
                    // So it is necessary to go to the correct position again
                    mediaPlayer.seekTo(position.toInt())

                    // If the state were set to playing, then the player is
                    // started after the reset. That is, the user was listening
                    // to a podcast
                    if (state == PlaybackStateCompat.STATE_PLAYING) {
                        mediaPlayer.start()
                    }
                }
            }
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
                // This speed parameter does not change
                // the playback speed. It is necessary to change it
                // directly in the media player
            .setState(state, position, speed)
            .build()

        // If there is a state change, this method should be called
        mediaSession.setPlaybackState(playbackState)

        // onStateChanged is called when the state changes to playing or paused
        if (state == PlaybackStateCompat.STATE_PAUSED ||
            state == PlaybackStateCompat.STATE_PLAYING) {
            listener?.onStateChanged()
        }
    }

    // Extracts the speed and calls speed
    private fun changeSpeed(extras: Bundle) {
        // Makes sure that the playbackState is not changed
        var playbackState = PlaybackStateCompat.STATE_PAUSED
        if (mediaSession.controller.playbackState != null) {
            playbackState = mediaSession.controller.playbackState.state
        }
        setState(playbackState, extras.getFloat(CMD_EXTRA_SPEED))
    }

    // Called by media session when a custom command is received
    override fun onCommand(command: String?, extras: Bundle?, cb: ResultReceiver?) {
        super.onCommand(command, extras, cb)

        when (command) {
            CMD_CHANGE_SPEED -> extras?.let { extras -> changeSpeed(extras) }
        }
    }

    private fun initializeMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
            // When the playback is finished, this code is executed
            mediaPlayer?.setOnCompletionListener { setState(PlaybackStateCompat.STATE_PAUSED) }

            mediaNeedsPrepare = true
        }
    }

    private fun prepareMedia() {
        // Only the data is new
        if (newMedia) {
            newMedia = false
            mediaPlayer?.let { mediaPlayer ->
                mediaUri?.let { mediaUri ->
                    if (mediaNeedsPrepare) {
                        // Back to uninitialized state
                        mediaPlayer.reset()
                        // The media that will be played
                        mediaPlayer.setDataSource(this.context, mediaUri)
                        // Puts media player on an initialized state
                        // Blocks until media player is ready for playback
                        mediaPlayer.prepare()
                    }

                    // Everytime the metadata changes,
                    // you should set it again to the media session
                    mediaExtras?.let { mediaExtras ->
                        // This metadata is used by the notification and the
                        // other media players to display details about the currently
                        // playing podcast episode
                        mediaSession.setMetadata(
                            MediaMetadataCompat.Builder()
                                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION,
                                mediaPlayer.duration.toLong())
                                .putString(MediaMetadataCompat.METADATA_KEY_TITLE,
                                mediaExtras.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
                                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST,
                                    mediaExtras.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
                                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
                                    mediaExtras.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI))
                                .build()
                        )
                    }


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
    private fun pausePlaying() {
        // First thing to do is removing ths audio focus
        removeAudioFocus()
        mediaPlayer?.let { mediaPlayer ->
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                setState(PlaybackStateCompat.STATE_PAUSED)
            }
        }
        // onPausePlaying is called when the podcast pause playing
        listener?.onPausePLaying()
    }

    override fun onPause() {
        super.onPause()
        pausePlaying()
        Log.d(TAG, "ONPAUSE CALLED")
    }

    // Method called when the seekTo command is received
    override fun onSeekTo(pos: Long) {
        super.onSeekTo(pos)

        mediaPlayer?.seekTo(pos.toInt())

        val playbackState: PlaybackStateCompat? =
            mediaSession.controller.playbackState

        // Any media browser client should know about the change
        // in the position
        if (playbackState != null)  {
            setState(playbackState.state)
        } else {
            setState(PlaybackStateCompat.STATE_PAUSED)
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
        // onStopPlaying() is called when playback stops
        listener?.onStopPlaying()
    }

    // Should call stopSelf()
    override fun onStop() {
        super.onStop()
        stopPlaying()

        Log.d(TAG, "ONSTOP CALLED")
    }

    companion object {
        const val CMD_CHANGE_SPEED = "change_speed"
        const val CMD_EXTRA_SPEED = "speed"
    }
}
