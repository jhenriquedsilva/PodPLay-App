package com.raywenderlich.podplay.ui

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.method.ScrollingMovementMethod
import android.util.Log
import com.raywenderlich.podplay.service.PodplayMediaService

import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.raywenderlich.podplay.R
import com.raywenderlich.podplay.adapter.EpisodeListAdapter
import com.raywenderlich.podplay.databinding.FragmentPodcastDetailsBinding
import com.raywenderlich.podplay.viewmodel.PodcastViewModel
import java.lang.RuntimeException

class PodcastDetailsFragment: Fragment() {

    interface OnPodcastDetailsListener {
        fun onSubscribe()
        fun onUnsubscribe()
    }

    val TAG = "PodcastDetailsFragment"
    private lateinit var databinding: FragmentPodcastDetailsBinding
    private lateinit var episodeListAdapter: EpisodeListAdapter
    // activityViewModels() provides the same activity that was initialized in the parent activity
    private val podcastViewModel: PodcastViewModel by  activityViewModels()
    private var listener: OnPodcastDetailsListener? = null

    private lateinit var mediaBrowser: MediaBrowserCompat
    private var mediaControllerCallback: MediaControllerCallback? = null

    // Receive callbacks from the media session every time its state or metadata changes
    inner class MediaControllerCallback: MediaControllerCompat.Callback() {

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            println("metadata changes to ${metadata?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI)}")
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            println("state changed to $state")
        }

    }

    // Handle connection messages
    inner class MediaBrowserCallBacks: MediaBrowserCompat.ConnectionCallback() {
        // If the connection is successful, this method is called
        // and it creates the MediaController, links it to the MediaSession
        override fun onConnected() {
            super.onConnected()
            registerMediaController(mediaBrowser.sessionToken)
            Log.d(TAG, "onConnected called")
        }

        override fun onConnectionSuspended() {
            super.onConnectionSuspended()
            println("onConnectionSuspended")
            // "Disable transport controls"
        }

        override fun onConnectionFailed() {
            super.onConnectionFailed()
            Log.i(TAG,"OnConnectionFailed")
            // "Fatal error handling"
        }
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

    override fun onStop() {
        super.onStop()
        // The media controller will not receive callbacks anymore
        val fragmentActivity = activity as FragmentActivity
        if (MediaControllerCompat.getMediaController(fragmentActivity) != null) {
            mediaControllerCallback?.let { mediaControllerCallback ->
                MediaControllerCompat.getMediaController(fragmentActivity)
                    .unregisterCallback(mediaControllerCallback)
            }
        }
        // MediaBrowser is disconnected
        // I CREATED THIS LINE. MAYBE IT IS WRONG
        mediaBrowser.disconnect()
    }

    // When the media browser is created, it asynchronously connects
    // th the browser service
    private fun initMediaBrowser() {
        // That's the current activity hosting the fragment
        val fragmentActivity = activity as FragmentActivity
        // A MediaBrowserCompat object is instantiated
        mediaBrowser = MediaBrowserCompat(
            // Current activity hosting the fragment
            fragmentActivity,
            // Which component the media browser should connect to
            ComponentName(fragmentActivity,PodplayMediaService::class.java),
            // The callback object to receive connection events
            MediaBrowserCallBacks(),
            null
        )
    }

    // When the fragment is created, it creates a media browser compat
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Informs Android that this Fragment wants to add items to the options menu
        // This makes the Fragment receives a call to onCreateOptionsMenu
        setHasOptionsMenu(true)
        initMediaBrowser()
    }


    // When the fragment is attached to its parent activity,
    // this method is called. The context is a reference to
    // the parent activity
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnPodcastDetailsListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() +
                    " must implement OnPodcastDetailsListener")
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        databinding = FragmentPodcastDetailsBinding.inflate(inflater,container,false)
        return databinding.root
    }

    // After the view is created, the data is loaded
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        /**
        // This method is called here to make sure that the podcast view data
        /// is already loaded by the main activity
        updateControls()
        */

        // Everytime there is a data change, the fragment UI is updated
        val podcastViewDataObserver = Observer<PodcastViewModel.PodcastViewData?> { podcastViewData ->
            if (podcastViewData != null) {
                databinding.feedTitleTextView.text = podcastViewData.feedTitle
                databinding.feedDescTextView.text = podcastViewData.feedDesc
                activity?.let { activity ->
                    Glide.with(activity).load(podcastViewData.imageUrl).into(databinding.feedImageView)
                }

                databinding.feedDescTextView.movementMethod = ScrollingMovementMethod()

                databinding.episodeRecyclerView.setHasFixedSize(true)
                val layoutManager = LinearLayoutManager(activity)
                databinding.episodeRecyclerView.layoutManager = layoutManager

                val dividerItemDecoration = DividerItemDecoration(
                    databinding.episodeRecyclerView.context,
                    layoutManager.orientation
                )
                databinding.episodeRecyclerView.addItemDecoration(dividerItemDecoration)

                episodeListAdapter = EpisodeListAdapter(podcastViewData.episodes)
                databinding.episodeRecyclerView.adapter = episodeListAdapter

                // Declares the option menu has changes,
                // so it needs to be recreated
                activity?.invalidateOptionsMenu()
            }
        }

        // If there is changes in podcastViewData data, the UI is updated
        podcastViewModel.podcastLiveData.observe(viewLifecycleOwner,podcastViewDataObserver)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        // Observes the podcast view data to display the correct data
        // in the toolbar: subscribed and unsubscribe
        val podcastViewDataObserver = Observer<PodcastViewModel.PodcastViewData?> { podcastViewData ->
            if (podcastViewData != null) {
                menu.findItem(R.id.menu_feed_action).title =
                    if (podcastViewData.subscribed) getString(R.string.unsubscribe)
                else getString(R.string.subscribe)
            }
        }
        podcastViewModel.podcastLiveData.observe(viewLifecycleOwner, podcastViewDataObserver)

        super.onPrepareOptionsMenu(menu)
    }

    // Inflates the menu details options menu so its items are added to
    // the Podcast Activity menu
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_details,menu)
    }

    // The activity is always who acts
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.menu_feed_action -> {
                if (item.title == getString(R.string.unsubscribe)) {
                    listener?.onUnsubscribe()
                } else {
                    listener?.onSubscribe()
                }
                return true
            }

            else -> return super.onOptionsItemSelected(item)

            }

    }

    companion object {
        // That's a static function
        fun newInstance(): PodcastDetailsFragment { return PodcastDetailsFragment() }
    }


}