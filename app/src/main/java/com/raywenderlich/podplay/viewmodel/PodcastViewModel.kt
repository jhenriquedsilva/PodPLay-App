package com.raywenderlich.podplay.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.raywenderlich.podplay.db.PodPlayDatabase
import com.raywenderlich.podplay.db.PodcastDao
import com.raywenderlich.podplay.model.Episode
import com.raywenderlich.podplay.model.Podcast
import com.raywenderlich.podplay.repository.PodcastRepo
import com.raywenderlich.podplay.util.DateUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Date

/**
 * This is the view model for the detail fragment
 */

class PodcastViewModel(application: Application): AndroidViewModel(application) {

    var podcastRepo: PodcastRepo? = null // Set by the caller
    val podcastDao: PodcastDao = PodPlayDatabase.getInstance(application,viewModelScope).podcastDao()
    // var activePodcastViewData: PodcastViewData? = null // Holds the most recently loaded podcast view data
    private var activePodcast: Podcast? = null // Holds the currently loaded podcast
    var activeEpisodeViewData: EpisodeViewData? = null
    // Holds all the podcasts that are stored in the database
    var livePodcastSummaryData: LiveData<List<SearchViewModel.PodcastSummaryViewData>>? = null
    // MutableLiveData is used to set the value, because it is the only one
    // with public methods for that. LiveData has no public methods for that
    private val _podcastLiveData = MutableLiveData<PodcastViewData?>()
    // LiveData is always within a ViewModel and is exposed to the UI controller
    val podcastLiveData: LiveData<PodcastViewData?> = _podcastLiveData
    private var isVideo: Boolean = false


    suspend fun setActivePodcast(feedUrl: String): SearchViewModel.PodcastSummaryViewData? {
        val repo = podcastRepo ?: return null
        val podcast = repo.getPodcast(feedUrl)
        if (podcast == null) {
            return null
        } else {
            _podcastLiveData.value = podcastToPodcastView(podcast)
            activePodcast = podcast
            return podcastToSummaryView(podcast)
        }
    }

    fun saveActivePodcast() {
        // These null check is done everywhere
        val repo = podcastRepo ?: return
        activePodcast?.let { activePodcast ->
            // Drops the first episode from the Podcast you are
            // subscribing to. Written just to test things out
            // activePodcast.episodes = activePodcast.episodes.drop(1)
            repo.save(activePodcast)
        }
    }

    fun deleteActivePodcast() {
        val repo = podcastRepo ?: return
        activePodcast?.let { activePodcast ->
            repo.delete(activePodcast)
        }
    }

    // This method returns the podcasts that are stored to be shown on screen
    fun getPodcasts(): LiveData<List<SearchViewModel.PodcastSummaryViewData>>? {
        val repo = podcastRepo ?: return null

        if (livePodcastSummaryData == null) {
            val liveData = repo.getAll()
            // LiveData has no map function. So it is necessary to use Transformations
            // to map the LiveData, and the return value still is a LiveData.
            livePodcastSummaryData = Transformations.map(liveData) { podcastList ->
                podcastList.map { podcast ->
                    podcastToSummaryView(podcast)
                }
            }
        }
        return livePodcastSummaryData
    }

    // All subscribed podcasts are returned
    private fun podcastToSummaryView(podcast: Podcast): SearchViewModel.PodcastSummaryViewData {
        return SearchViewModel.PodcastSummaryViewData(
            podcast.feedTitle,
            podcast.lastUpdated,
            podcast.imageUrl,
            podcast.feedUrl
        )
    }
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
                    // The new loaded podcast is always updated
                    activePodcast = parsedPodcast
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
            // If the id is different from null,
            // that means it is stored in the database
            podcast.id != null,
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

            // Checks mime type on each episode to see if it starts with "video"
            val isVideo = episode.mimeType.startsWith("video")

            EpisodeViewData(
                episode.guid,
                episode.title,
                episode.description,
                episode.mediaUrl,
                episode.mimeType,
                episode.releaseDate,
                episode.duration,
                isVideo
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
        var isVideo: Boolean = false
    )
}