package com.raywenderlich.podplay.ui

import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.raywenderlich.podplay.databinding.FragmentEpisodePlayerBinding
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

    // Receive callbacks from the media session every time its state or metadata changes
    inner class MediaControllerCallback: MediaControllerCompat.Callback() {

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            Log.d(TAG,"Metadata changed to ${metadata?.getString(MediaMetadataCompat.METADATA_KEY_TITLE)}")
        }

        private fun handleStateChange(state: Int) {
            val isPlaying = (state == PlaybackStateCompat.STATE_PLAYING)
            // If the media is playing, sets the button state to activated.
            // Otherwise, sets it to not activated
            layout.playToggleButton.isActivated = isPlaying
        }

        // The toggle button is changed over here to be in sync even though
        // the state is changed from outside the app
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            Log.d(TAG, "ONPLAYBACKSTATECHANGED CALLED")
            Log.d(TAG,"State changed to $state")

            // Changes the toggle's image
            val state = state ?: return
            handleStateChange(state.state)
        }

    }

    // Handle connection messages
    inner class MediaBrowserCallBacks: MediaBrowserCompat.ConnectionCallback() {
        // If the connection is successful, this method is called
        // and it creates the MediaController, links it to the MediaSession
        override fun onConnected() {
            super.onConnected()
            registerMediaController(mediaBrowser.sessionToken)
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

        initializeMediaBrowser()
    }

    // Register controller to receive callbacks from the MediaSession
    private fun registerMediaController(token: MediaSessionCompat.Token) {

        val fragmentActivity = activity as FragmentActivity
        // Creates the MediaController and connects it to the MediaSession
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
        // If a configuration change occurs, the media browser remains connected
        // but it is necessary to create the media controller again, if it is not
        // null obviously
        if (mediaBrowser.isConnected) {
            val fragmentActivity = activity as FragmentActivity
            if (MediaControllerCompat.getMediaController(fragmentActivity) == null) {
                registerMediaController(mediaBrowser.sessionToken)
            }
        } else {
            // If not connected, connect
            // It will register the media controller automatically
            // by calling onConnected()
            mediaBrowser.connect()
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

    // Set up view controls
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
    }

    // Controls the start and stop playback
    private fun togglePlayPause() {
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

    // Sets a listener on toggle button
    private fun setupControls() {
        layout.playToggleButton.setOnClickListener {
            togglePlayPause()
        }
    }

    // Data must be hooked up after the view is created
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // I think the order is irrelevant here
        setupControls()
        updateControls()
    }

    override fun onStop() {
        super.onStop()
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
        // MediaBrowser is disconnected
        // I CREATED THIS LINE. MAYBE IT IS WRONG
        // mediaBrowser.disconnect()
    }

    companion object {
        fun newInstance(): EpisodePlayerFragment {
            return EpisodePlayerFragment()
        }
    }
}