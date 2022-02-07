package com.raywenderlich.podplay.ui

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
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

class PodcastDetailsFragment: Fragment() {

    private lateinit var databinding: FragmentPodcastDetailsBinding
    private lateinit var episodeListAdapter: EpisodeListAdapter
    // activityViewModels() provides the same activity that was initialized in the parent activity
    private val podcastViewModel: PodcastViewModel by  activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Informs Android that this Fragment wants to add items to the options menu
        // This makes the Fragment receives a call to onCreateOptionsMenu
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        databinding = FragmentPodcastDetailsBinding.inflate(inflater,container,false)
        return databinding.root
    }

    // After the vire is created, the data is loaded
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
            }
        }

        // If there is changes in podcastViewData data, the UI is updated
        podcastViewModel.podcastLiveData.observe(viewLifecycleOwner,podcastViewDataObserver)
    }

    // Inflates the menu details options menu so its items are added to
    // the Podcast Activity menu
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_details,menu)
    }

    // Setting thr UI data
    private fun updateControls() {
        // Gets the data from the view model and populates the layout
        if (podcastViewModel.activePodcastViewData != null) {
            val viewData = podcastViewModel.activePodcastViewData as PodcastViewModel.PodcastViewData
            databinding.feedTitleTextView.text = viewData.feedTitle
            databinding.feedDescTextView.text = viewData.feedDesc
            // Gets the parent activity to associate with Glide
            if (activity != null) { Glide.with(activity as FragmentActivity).load(viewData.imageUrl).into(databinding.feedImageView)}
        } else {
            return
        }

    }

    companion object {
        // That's a static function
        fun newInstance(): PodcastDetailsFragment { return PodcastDetailsFragment() }
    }
}