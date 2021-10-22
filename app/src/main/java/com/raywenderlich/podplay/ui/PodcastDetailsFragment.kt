package com.raywenderlich.podplay.ui

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.raywenderlich.podplay.R
import com.raywenderlich.podplay.databinding.FragmentPodcastDetailsBinding
import com.raywenderlich.podplay.viewmodel.PodcastViewModel

class PodcastDetailsFragment: Fragment() {

    private lateinit var databinding: FragmentPodcastDetailsBinding
    // activityViewModels() is an extension function that allows the fragment
    // to access and share view models from the fragment's parent activity
    // They can share the same data. When the fragment is attached to the activity it
    // will automatically assign podcastViewModel to the already initialized parent
    // activity's podcastViewModel
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // This method is called here to make sure that the podcast view data
        /// is already loaded by the main activity
        updateControls()
    }

    // Inflates the menu details options menu so its items are added to
    // the Podcast Activity menu
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_details,menu)
    }

    private fun updateControls() {
        // Gets the data from the view model and populates the layout
        val viewData = podcastViewModel.activePodcastViewData ?: return
        databinding.feedTitleTextView.text = viewData.feedTitle
        databinding.feedDescTextView.text = viewData.feedDesc
        activity?.let { activity ->
            Glide.with(activity).load(viewData.imageUrl).into(databinding.feedImageView)
        }
    }

    companion object {
        fun newInstance(): PodcastDetailsFragment {
            return PodcastDetailsFragment()
        }
    }
}