package com.raywenderlich.podplay.ui

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.Html
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
import com.raywenderlich.podplay.util.HtmlUtils
import com.raywenderlich.podplay.viewmodel.PodcastViewModel
import com.raywenderlich.podplay.viewmodel.PodcastViewModel.EpisodeViewData
import java.lang.RuntimeException

class PodcastDetailsFragment: Fragment(), EpisodeListAdapter.EpisodeListAdapterListener {

    interface OnPodcastDetailsListener {
        fun onSubscribe()
        fun onUnsubscribe()
        fun onShowEpisodePlayer(episodeViewData: EpisodeViewData)
    }

    private lateinit var layout: FragmentPodcastDetailsBinding
    private lateinit var episodeListAdapter: EpisodeListAdapter
    // activityViewModels() provides the same activity that was initialized in the parent activity
    private val podcastViewModel: PodcastViewModel by  activityViewModels()
    private var listener: OnPodcastDetailsListener? = null


    // When the fragment is created, it creates a media browser compat
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Informs Android that this Fragment wants to add items to the options menu
        // This makes the Fragment receives a call to onCreateOptionsMenu
        setHasOptionsMenu(true)
    }

    override fun onSelectedEpisode(episodeViewData: EpisodeViewData) {

        listener?.onShowEpisodePlayer(episodeViewData)
        /*
        // Assign activity to a local variable because it can change to null between calls
        val fragmentActivity = activity as FragmentActivity
        // Get the media controller previously assigned to the media controller
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)

        if (controller.playbackState != null) {
            if (controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
                controller.transportControls.pause()
            } else {
                startPlaying(episodeViewData)
            }

        } else {
            startPlaying(episodeViewData)
        }
        */
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
        layout = FragmentPodcastDetailsBinding.inflate(inflater,container,false)
        return layout.root
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
                layout.feedTitleTextView.text = podcastViewData.feedTitle
                val description = podcastViewData.feedDesc ?: ""
                layout.feedDescTextView.text = HtmlUtils.htmlToSpannable(description)
                activity?.let { activity ->
                    Glide.with(activity).load(podcastViewData.imageUrl).into(layout.feedImageView)
                }

                layout.feedDescTextView.movementMethod = ScrollingMovementMethod()

                layout.episodeRecyclerView.setHasFixedSize(true)
                val layoutManager = LinearLayoutManager(activity)
                layout.episodeRecyclerView.layoutManager = layoutManager

                val dividerItemDecoration = DividerItemDecoration(
                    layout.episodeRecyclerView.context,
                    layoutManager.orientation
                )
                layout.episodeRecyclerView.addItemDecoration(dividerItemDecoration)

                episodeListAdapter = EpisodeListAdapter(podcastViewData.episodes, this)
                layout.episodeRecyclerView.adapter = episodeListAdapter

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
        const val CMD_CHANGE_SPEED = "change_speed"
        const val CMD_EXTRA_SPEED = "speed"
        // That's a static function
        fun newInstance(): PodcastDetailsFragment { return PodcastDetailsFragment() }
    }



}