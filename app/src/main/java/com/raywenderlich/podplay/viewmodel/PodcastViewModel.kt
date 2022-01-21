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
    private val _podcastLiveData = MutableLiveData<PodcastViewData?>()
    val podcastLiveData: LiveData<PodcastViewData?> = _podcastLiveData

    // Retrieves the podcast from the repo
    fun getPodcast(podcastSummaryViewData: SearchViewModel.PodcastSummaryViewData) {

        val feedUrl = podcastSummaryViewData.feedUrl?.let { url ->
            viewModelScope.launch {
                podcastRepo?.getPodcast(url)?.let { podcast ->
                    podcast.feedTitle = podcastSummaryViewData.name ?: ""
                    podcast.imageUrl = podcastSummaryViewData.imageUrl ?: ""
                    _podcastLiveData.value = podcastToPodcastView(podcast)
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
     * from Podcast to PodcastViewData is necessary
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
     * from Episode to EpisodeViewData is necessary
     */
    private fun episodesToEpisodesView(episodes: List<Episode>): List<EpisodeViewData> {
        return episodes.map { episode ->
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
        var releaseDate: Date = Date(),
        var duration: String = "",
    )
}