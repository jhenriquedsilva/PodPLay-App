package com.raywenderlich.podplay.ui

import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.format.DateUtils
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.raywenderlich.podplay.databinding.FragmentEpisodePlayerBinding
import com.raywenderlich.podplay.service.PodplayMediaCallback
import com.raywenderlich.podplay.service.PodplayMediaCallback.Companion.CMD_CHANGE_SPEED
import com.raywenderlich.podplay.service.PodplayMediaCallback.Companion.CMD_EXTRA_SPEED
import com.raywenderlich.podplay.service.PodplayMediaService
import com.raywenderlich.podplay.util.HtmlUtils
import com.raywenderlich.podplay.viewmodel.PodcastViewModel

class EpisodePlayerFragment: Fragment() {

    val TAG = "EpisodePlayerFragment"
    private val podcastViewModel: PodcastViewModel by activityViewModels()
    private lateinit var mediaBrowser: MediaBrowserCompat
    private var mediaControllerCallback: MediaControllerCallback? = null
    // That's the minimum code required to display a fragment
    private lateinit var layout: FragmentEpisodePlayerBinding
    // Keeps track of the current speed
    private var playerSpeed: Float = 1.0f
    private var episodeDuration: Long = 0
    private var draggingScrubber: Boolean = false
    private var progressAnimator: ValueAnimator? = null
    // This media session is specific for videos
    private var mediaSession: MediaSessionCompat? = null
    private var mediaPlayer: MediaPlayer? = null
    // Needs to know if the user taps the play button
    // before the media is ready to play
    private var playOnPrepare: Boolean = false
    private var isVideo: Boolean = false

    // Create the animation and kick it off
    private fun animateScrubber(progress: Int, speed: Float) {
        // Normalization happens here                          // toInt() before
        val timeRemaining = ((episodeDuration - progress) / speed).toLong()

        // Podcast finished
        if (timeRemaining < 0) { return }

        // Creates a new ValueAnimator with an starting and ending value
        progressAnimator = ValueAnimator.ofInt(progress, episodeDuration.toInt())

        progressAnimator?.let { progressAnimator ->
            progressAnimator.duration = timeRemaining
            progressAnimator.interpolator = LinearInterpolator()
            // Listener called by the animator on each step of the animation
            progressAnimator.addUpdateListener {
                if (draggingScrubber) {
                    progressAnimator.cancel()
                } else {
                    layout.seekBar.progress = progressAnimator.animatedValue as Int
                }
            }
            progressAnimator.start()
        }
    }

    // Updates the endTimeTextView
    private fun updateControlsFromMetadata(metadata: MediaMetadataCompat) {
        // Returns 0 if there is no number for this key
        episodeDuration = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)

        // Pass the time in seconds and it will be formatted beautifully
        layout.endTimeTextView.text = DateUtils.formatElapsedTime((episodeDuration / 1000))

        layout.seekBar.max = episodeDuration.toInt()
    }

    // Hides everything on the screen except the video controls
    // The video playback should use the maximum size at the screen
    private fun setupVideoUI() {
        layout.episodeImageView.visibility = View.INVISIBLE
        layout.headerView.visibility = View.INVISIBLE
        // Gets the activity ti hide its action bar
        val activity = activity as AppCompatActivity
        activity.supportActionBar?.hide()

        layout.playerControls.setBackgroundColor(Color.argb(255/2,0,0,0))
    }

    private fun handleStateChange(state: Int, position: Long, speed: Float) {
        // Stop the animation when the playback stops
        progressAnimator?.let { animator ->
            animator.cancel()
            progressAnimator = null
        }
        val isPlaying = (state == PlaybackStateCompat.STATE_PLAYING)
        // If the media is playing, sets the button state to activated.
        // Otherwise, sets it to not activated
        layout.playToggleButton.isActivated = isPlaying

        // Sets the scrubber to the current progress position
        val progress = position.toInt()
        layout.seekBar.progress = progress
        // Updates the speed control label
        val speedButtonText = "${playerSpeed}x"
        layout.speedButton.text = speedButtonText

        // If it is playing, create the animation
        if (isPlaying) {
            if (isVideo) {
                setupVideoUI()
            }
            animateScrubber(progress, speed)
        }
    }

    // Receive callbacks from the media session every time its state or metadata changes
    inner class MediaControllerCallback: MediaControllerCompat.Callback() {

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            Log.d(TAG,"Metadata changed to ${metadata?.getString(MediaMetadataCompat.METADATA_KEY_TITLE)}")

            metadata?.let { metadata -> updateControlsFromMetadata(metadata) }
        }

        // The toggle button is changed over here to be in sync even though
        // the state is changed from outside the app
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            Log.d(TAG, "ONPLAYBACKSTATECHANGED CALLED")
            Log.d(TAG,"State changed to $state")

            // Changes the toggle's image
            val state = state ?: return
            handleStateChange(state.state, state.position, state.playbackSpeed)

        }

    }

    private fun updateControlsFromController() {
        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)

        if (controller != null) {
            val metadata = controller.metadata
            if (metadata != null) {
                handleStateChange(
                    controller.playbackState.state,
                    controller.playbackState.position,
                    playerSpeed
                )
                updateControlsFromMetadata(controller.metadata)
            }
        }
    }

    // Handle connection messages
    inner class MediaBrowserCallBacks: MediaBrowserCompat.ConnectionCallback() {
        // If the connection is successful, this method is called
        // and it creates the MediaController, links it to the MediaSession
        override fun onConnected() {
            super.onConnected()
            registerMediaController(mediaBrowser.sessionToken)
            // If there is a screen rotation, the screen keeps updated
            updateControlsFromController()
            Log.d(TAG, "ONCONNECTED CALLED")
        }

        override fun onConnectionSuspended() {
            super.onConnectionSuspended()
            Log.d(TAG, "ONCONNECTIONSUSPENDED CALLED")
            // "Disable transport controls"
        }

        override fun onConnectionFailed() {
            super.onConnectionFailed()
            Log.d(TAG,"ONCONNECTIONFAILED CALLED")
            // "Fatal error handling"
        }
    }

    private fun startPlaying(episodeViewData: PodcastViewModel.EpisodeViewData) {
        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)

        // Pass in the additional episode details and add them to the media session metadata
        // ViewData is the podcast data and episodeViewData is the episode data
        val viewData = podcastViewModel.podcastLiveData.value ?: return
        // Bundle is a way send data
        val bundle = Bundle().apply {
            putString(MediaMetadataCompat.METADATA_KEY_TITLE, episodeViewData.title)
            putString(MediaMetadataCompat.METADATA_KEY_ARTIST, viewData.feedTitle)
            putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, viewData.imageUrl)
        }

        // The call to playFromUri() triggers the onPlayFromUri callback in PodplayMediaService
        controller.transportControls.playFromUri(Uri.parse(episodeViewData.mediaUrl), bundle)
    }

    // Media browser is created
    private fun initializeMediaBrowser() {
        // That's the current activity hosting the fragment
        val fragmentActivity = activity as FragmentActivity
        // A MediaBrowserCompat object is instantiated
        mediaBrowser = MediaBrowserCompat(
            // Current activity hosting the fragment
            fragmentActivity,
            // Which component the media browser should connect to
            ComponentName(fragmentActivity, PodplayMediaService::class.java),
            // The callback object to receive connection events
            MediaBrowserCallBacks(),
            null
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If the SDK version supports video playing and
        // isVideo is not equal to null, so there is a video to play
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            isVideo = podcastViewModel.activeEpisodeViewData?.isVideo ?: false
        } else {
            isVideo = false
        }

        // Only initialize the media browser if there is no
        // video to play
        if (!isVideo) {
            initializeMediaBrowser()
        }
    }

    // Register controller to receive callbacks from the MediaSession
    private fun registerMediaController(token: MediaSessionCompat.Token) {

        val fragmentActivity = activity as FragmentActivity
        // Creates the MediaController and connects it to the MediaSession
        // whose token was passed in
        val mediaController = MediaControllerCompat(fragmentActivity, token)
        // Links the UI control to the MediaController
        MediaControllerCompat.setMediaController(fragmentActivity,mediaController)

        mediaControllerCallback = MediaControllerCallback()
        // Register the controller to receive callbacks from the media session
        mediaController.registerCallback(mediaControllerCallback!!)
    }


    // Connects the media browser
    override fun onStart() {
        super.onStart()

        // The media browser logic is implemented if
        // it was initialized before
        if (!isVideo) {
            // If a configuration change occurs, the media browser remains connected
            // but it is necessary to create the media controller again, if it is not
            // null obviously
            if (mediaBrowser.isConnected) {
                val fragmentActivity = activity as FragmentActivity
                if (MediaControllerCompat.getMediaController(fragmentActivity) == null) {
                    registerMediaController(mediaBrowser.sessionToken)
                }
                // Keeps the UI in sync if there is a screen rotation
                updateControlsFromController()
            } else {
                // If not connected, connect
                // It will register the media controller automatically
                // by calling onConnected()
                mediaBrowser.connect()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // super.onCreateView(inflater, container, savedInstanceState)

        layout = FragmentEpisodePlayerBinding.inflate(inflater, container, false)
        return layout.root

    }

    // Controls the start and stop playback
    private fun togglePlayPause() {
        // If the button was tapped at least once,
        // set this variable to true
        playOnPrepare = true

        val fragmentActivity = activity as FragmentActivity

        val controller = MediaControllerCompat.getMediaController(fragmentActivity)

        // Always be careful with the null check
        if (controller.playbackState != null) {

            if (controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
                controller.transportControls.pause()
            } else {
                // The media player needs data to start playing.
                // So call startPlaying() instead of onPlay()
                podcastViewModel.activeEpisodeViewData?.let { episodeViewData ->
                    startPlaying(episodeViewData)
                }
            }

        } else {

            // Starts the podcast even though thr the current state is equal null?
            podcastViewModel.activeEpisodeViewData?.let { episodeViewData ->
                startPlaying(episodeViewData)
            }
        }
    }

    // Make the video view match the size of the podcast video and keep
    // the video aspect ratio intact
    private fun setSurfaceSize() {
        // Impossible without the media player
        val mediaPlayer = mediaPlayer ?: return

        // Video size
        val videoWidth = mediaPlayer.videoWidth
        val videoHeight = mediaPlayer.videoHeight

        // Layout size
        val parent = layout.videoSurfaceView.parent as View
        val containerWidth = parent.width
        val containerHeight = parent.height

        // Ratio view
        val layoutAspectRatio = containerWidth.toFloat() / containerHeight
        // Ratio video
        val videoAspectRatio = videoWidth.toFloat() / videoHeight

        // View parameters
        val layoutParams = layout.videoSurfaceView.layoutParams

        if (videoAspectRatio > layoutAspectRatio) {
            layoutParams.height = (containerWidth / videoAspectRatio).toInt()
        } else {
            layoutParams.width = (containerHeight * videoAspectRatio).toInt()
        }

        layout.videoSurfaceView.layoutParams = layoutParams
    }

    private fun initializeMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
            mediaPlayer?.let { mediaPlayer ->
                mediaPlayer.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )

                // Data source of the episode media playback
                mediaPlayer.setDataSource(podcastViewModel.activeEpisodeViewData?.mediaUrl)

                // Registers a callback to be invoked when the media source is
                // ready for playback
                mediaPlayer.setOnPreparedListener {
                    val fragmentActivity = activity as FragmentActivity
                    mediaSession?.let { mediaSession ->
                        // Activity is passed over here
                        val episodeMediaCallback = PodplayMediaCallback(fragmentActivity, mediaSession)
                        mediaSession.setCallback(episodeMediaCallback)
                    }

                    setSurfaceSize()

                    // If the user has already tapped the play button,
                    // then the video is started
                    if (playOnPrepare) {
                        togglePlayPause()
                    }
                }

                // Prepares player for playback in the background
                mediaPlayer.prepareAsync()
            }

        } else {
            // If there is a screen rotation
            // Then, adjust the screen again
            setSurfaceSize()
        }
    }

    // Initialize the video surface and call the initializeMediaPlayer method
    private fun initializeVideoPlayer() {
        // When the surface view is made visible,
        // Android must prepare it for use
        layout.videoSurfaceView.visibility = View.VISIBLE

        // Control the surface characteristics
        // This object is used to determine the surface availability
        val surfaceHolder = layout.videoSurfaceView.holder

        surfaceHolder.addCallback(
            object : SurfaceHolder.Callback {

                // The surface view is only available when this method is called
                override fun surfaceCreated(holder: SurfaceHolder) {
                    initializeMediaPlayer()
                    // The surface is assigned as the display object for the
                    // media player
                    mediaPlayer?.setDisplay(holder)
                }

                override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int
                ) {

                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {

                }

            }
        )
    }

    // Set up the data on screen
    private fun updateControls() {
        // Sets the title of the episode
        layout.episodeTitleTextView.text = podcastViewModel.activeEpisodeViewData?.title

        // If the description equal null, empty string
        val htmlDesc = podcastViewModel.activeEpisodeViewData?.description ?: ""
        val descSpan = HtmlUtils.htmlToSpannable(htmlDesc)
        // Sets the description of the episode
        layout.episodeDescTextView.text = descSpan
        layout.episodeDescTextView.movementMethod = ScrollingMovementMethod()

        // Loads the podcast image into the image view
        val fragmentActivity = activity as FragmentActivity
        Glide.with(fragmentActivity).load(podcastViewModel.podcastLiveData.value?.imageUrl)
            .into(layout.episodeImageView)

        var speedButtonText = "${playerSpeed}x"
        layout.speedButton.text = speedButtonText

        mediaPlayer?.let {
            updateControlsFromController()
        }
    }

    // Initialize mediaSession to playback video podcast
    private fun initializeMediaSession() {
        if (mediaSession == null) {
            mediaSession = MediaSessionCompat(activity as Context, "EpisodePlayerFragment")
            mediaSession?.setMediaButtonReceiver(null)
        }

        // The session token need to be passed to the controller
        // for the communication to happen
        mediaSession?.let { mediaSession ->
            registerMediaController(mediaSession.sessionToken)
        }
    }

    private fun seekBy(seconds: Int) {
        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)
        // The position should be passed to milliseconds
        val newPosition = controller.playbackState.position + seconds * 1000
        controller.transportControls.seekTo(newPosition)
    }

    private fun changeSpeed() {
        playerSpeed += 0.25f

        if (playerSpeed > 2.0f) {
            playerSpeed = 0.75f
        }

        val bundle = Bundle()
        bundle.putFloat(CMD_EXTRA_SPEED, playerSpeed)

        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)
        controller.sendCommand(CMD_CHANGE_SPEED, bundle, null)

        val speedButtonText = "${playerSpeed}x"
        layout.speedButton.text = speedButtonText
    }

    // Sets a listener on toggle button
    private fun setupControls() {
        layout.playToggleButton.setOnClickListener {
            togglePlayPause()
        }

        // The button is only set up if the Android is greater than
        // Android Marshmallow
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            layout.speedButton.setOnClickListener {
                changeSpeed()
            }
        } else {
            // If the phone does not support the feature,
            // make the button invisible
            layout.speedButton.visibility = View.INVISIBLE
        }

        layout.forwardButton.setOnClickListener { seekBy(30) }
        layout.replayButton.setOnClickListener { seekBy(-10) }

        layout.seekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {

                // If the scrubber position changes, this method is called
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    layout.currentTimeTextView.text =
                        DateUtils.formatElapsedTime((progress / 1000).toLong())
                }

                // If the scrubber indicator is dragged, this method is called
                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    draggingScrubber = true
                }

                // The user stops dragging the scrubber indicator
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    draggingScrubber = false

                    val fragmentActivity = activity as FragmentActivity
                    val controller = MediaControllerCompat.getMediaController(fragmentActivity)
                    if (controller.playbackState != null) {
                        // Gets the position at the seekBar and sets the position in the media controller
                        // Seek directly to the new position playback
                        controller.transportControls.seekTo(seekBar?.progress?.toLong() as Long)
                    } else {
                        seekBar?.progress = 0
                    }
                }

            }
        )
    }


    // Data must be hooked up after the view is created
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // I think the order is irrelevant here
        setupControls()
        if (isVideo) {
            initializeMediaSession()
            initializeVideoPlayer()
        }
        updateControls()
    }

    override fun onStop() {
        super.onStop()

        progressAnimator?.cancel()

        // The media controller will not receive callbacks anymore
        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)
        if (controller != null) {
            // controller.transportControls.stop()
            mediaControllerCallback?.let { mediaControllerCallback ->
                MediaControllerCompat.getMediaController(fragmentActivity)
                    .unregisterCallback(mediaControllerCallback)
            }
        }

        // Clears the display surface when the screen is rotated
        if (isVideo) {
            mediaPlayer?.setDisplay(null)
        }

        // Stops the playback when the fragment UI is exited
        if (!fragmentActivity.isChangingConfigurations) {
            // The media player will not be used no more
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    companion object {
        fun newInstance(): EpisodePlayerFragment {
            return EpisodePlayerFragment()
        }
    }
}