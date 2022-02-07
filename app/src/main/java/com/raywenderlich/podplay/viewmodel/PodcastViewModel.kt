package com.raywenderlich.podplay.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.raywenderlich.podplay.model.Episode
import com.raywenderlich.podplay.model.Podcast
import com.raywenderlich.podplay.repository.PodcastRepo
import kotlinx.coroutines.launch
import java.util.Date

/**
 * This is the view model for the detail fragment
 */

class PodcastViewModel(application: Application): AndroidViewModel(application) {

    var podcastRepo: PodcastRepo? = null // Set by the caller
    var activePodcastViewData: PodcastViewData? = null // Holds the most recently loaded podcast view data
    // MutableLiveData is used to set the value, because it is the only one
    // with public methods for that. LiveData has no public methods for that
    private val _podcastLiveData = MutableLiveData<PodcastViewData?>()
    // LiveData is always within a ViewModel and is exposed to the UI controller
    val podcastLiveData: LiveData<PodcastViewData?> = _podcastLiveData

    // Retrieves the podcast from the repo
    fun getPodcast(podcastSummaryViewData: SearchViewModel.PodcastSummaryViewData) {

        /**
         * Elvis operator is like a null else
         * If a value is equal to null, then execute what
         * comes after ?:
         */
        podcastSummaryViewData.feedUrl?.let { url ->
            // run is equal to apply, but returns the last line instead

            viewModelScope.launch {

                val parsedPodcast = podcastRepo?.getPodcast(url)
                parsedPodcast?.let { parsedPodcast ->
                    parsedPodcast.feedTitle = podcastSummaryViewData.name ?: ""
                    parsedPodcast.imageUrl = podcastSummaryViewData.imageUrl ?: ""
                    // When these assignment happens, the observers are notified
                    _podcastLiveData.value = podcastToPodcastView(parsedPodcast)
                } ?: run {
                    _podcastLiveData.value = null
                }

            }

        } ?: run {
            _podcastLiveData.value = null
        }
    }

    /**
     * As the repo returns a Podcast model, a function to convert
     * from Podcast to PodcastViewData is necessary to be shown on screen
     */
    private fun podcastToPodcastView(podcast: Podcast): PodcastViewData {
        return PodcastViewData(
            false,
            podcast.feedUrl,
            podcast.feedTitle,
            podcast.feedDesc,
            podcast.imageUrl,
            episodesToEpisodesView(podcast.episodes)
        )
    }

    /**
     * As the repo returns a list of Episode models, a function to convert
     * from Episode to EpisodeViewData is necessary to show on the screen
     */
    private fun episodesToEpisodesView(episodes: List<Episode>): List<EpisodeViewData> {
        val episodesViewDataList = episodes.map { episode ->
            EpisodeViewData(
                episode.guid,
                episode.title,
                episode.description,
                episode.mediaUrl,
                episode.mimeType,
                episode.releaseDate,
                episode.duration
            )
        }

        return episodesViewDataList
    }

    // Contains everything necessary to display the details of a podcast
    data class PodcastViewData(
        var subscribed: Boolean = false,
        var feedUrl: String? = "",
        var feedTitle: String? = "",
        var feedDesc: String? = "",
        var imageUrl: String? = "",
        var episodes: List<EpisodeViewData>
    )

    data class EpisodeViewData(
        var guid: String = "", // Unique identifier provided in the RSS feed for an episode
        var title: String = "",
        var description: String = "",
        var mediaUrl: String = "", // The location of the episode media. Either an audio or video file
        var mimeType: String = "", // Determines the type of file located at mediaUrl
        var releaseDate: String = "",
        var duration: String = "",
    )
}