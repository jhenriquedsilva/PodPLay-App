package com.raywenderlich.podplay.ui

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.raywenderlich.podplay.databinding.FragmentEpisodePlayerBinding
import com.raywenderlich.podplay.util.HtmlUtils
import com.raywenderlich.podplay.viewmodel.PodcastViewModel

class EpisodePlayerFragment: Fragment() {

    private val podcastViewModel: PodcastViewModel by activityViewModels()

    // That's the minimum code required to display a fragment
    private lateinit var layout: FragmentEpisodePlayerBinding



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

    // Data must be hooked up after the view is created
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateControls()
    }

    companion object {
        fun newInstance(): EpisodePlayerFragment {
            return EpisodePlayerFragment()
        }
    }
}