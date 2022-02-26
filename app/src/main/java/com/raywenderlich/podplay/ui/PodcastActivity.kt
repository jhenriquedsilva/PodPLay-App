package com.raywenderlich.podplay.ui

import androidx.appcompat.app.AlertDialog
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.*
import com.raywenderlich.podplay.R
import com.raywenderlich.podplay.adapter.PodcastListAdapter
import com.raywenderlich.podplay.databinding.ActivityPodcastBinding
import com.raywenderlich.podplay.repository.ItunesRepo
import com.raywenderlich.podplay.repository.PodcastRepo
import com.raywenderlich.podplay.service.ItunesService
import com.raywenderlich.podplay.service.RssFeedService
import com.raywenderlich.podplay.viewmodel.PodcastViewModel
import com.raywenderlich.podplay.viewmodel.SearchViewModel
import com.raywenderlich.podplay.worker.EpisodeUpdateWorker
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

class PodcastActivity : AppCompatActivity(),
    PodcastListAdapter.PodcastListAdapterListener,
PodcastDetailsFragment.OnPodcastDetailsListener {

    private lateinit var layout: ActivityPodcastBinding
    private val searchViewModel by viewModels<SearchViewModel>()
    // Used to hold the podcast view data
    private val podcastViewModel by viewModels<PodcastViewModel>()
    private lateinit var podcastListAdapter: PodcastListAdapter
    // Saves a reference to the search icon to hide it when the activity
    // is created and and retrieve it back later when the activity is destroyed
    // That's the only reason for this property
    private lateinit var searchMenuItem: MenuItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // How to create a view using binding
        layout = ActivityPodcastBinding.inflate(layoutInflater)
        setContentView(layout.root)
        setupToolbar()
        setupViewModels()
        updateControls()
        setupPodcastListView()
        handleIntent(intent)
        // Always makes sure that the first Recycler View is shown
        addBackStackListener()
        createSubscription()
        scheduleJobs()
    }


    // Searching a podcast feature 1

    private fun showSubscribedPodcasts() {
        val podcasts = podcastViewModel.getPodcasts()?.value

        if (podcasts != null) {
            layout.toolbar.title = getString(R.string.subscribed_podcasts)
            podcastListAdapter.setSearchData(podcasts)
        }
    }

    private fun setupPodcastListView() {
        val podcastSummaryViewDataListObserver =
            Observer<List<SearchViewModel.PodcastSummaryViewData>> { podcastSummaryViewDataList ->
                if (podcastSummaryViewDataList != null) {
                    showSubscribedPodcasts()
                }
            }
        podcastViewModel.getPodcasts()?.observe(this, podcastSummaryViewDataListObserver)
    }

    // Creates the search icon
    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        menuInflater.inflate(R.menu.menu_search, menu)
        // The search action menu is found
        searchMenuItem = menu.findItem(R.id.search_item)

        searchMenuItem.setOnActionExpandListener(
            object : MenuItem.OnActionExpandListener {
                override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                    return true
                }
                // This action is taken when a search is made in the PodcastActivity
                // and the user wants to see the subscribed podcasts again
                override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                    showSubscribedPodcasts()
                    return true
                }
            }
        )
        // The search view is taken from the item's action view
        val searchView = searchMenuItem.actionView as SearchView

        // Used to load the searchable info XML file created
        var searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager

        // searchManager is used to set the search configuration
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))

        // The podcast recycler view needs to be hidden when there is a
        // configuration change. If the user is in the details fragment and
        // and they rotate the device, the recycler view keeps hidden
        if (supportFragmentManager.backStackEntryCount > 0) {
            layout.podcastRecyclerView.visibility = View.INVISIBLE
        }

        /**
         * As the fragment adds an action button to the menu, the menu is recreated
         * when the fragment is added. So it is necessary to make sure that the
         * searchMenuItem remains invisible while the recyclerView is invisible
         */
        if (layout.podcastRecyclerView.visibility == View.INVISIBLE) {
            searchMenuItem.isVisible = false
        }

        return true
    }

    // These methods perform the actual search

    // Create the network request
    private fun performSearch(term: String) {
        // Starts fetching data
        showProgressBar()
        // Coroutine attached to the lifecycle of this activity
        lifecycleScope.launch {
            // List with ready-to-use podcasts
            val results = searchViewModel.searchPodcasts(term)
            // withContext is used to change which thread the coroutine is running in
            // in this case, changindd to UI thread
            withContext(Dispatchers.Main) {
                hideProgressBar()
                layout.toolbar.title = term
                // The recycler view is populated again
                podcastListAdapter.setSearchData(results)
            }
            // Log.i(TAG, "Results = ${results.body()}")
        }

    }

    // Gets the search term
    private fun handleIntent(intent: Intent) {
        if (Intent.ACTION_SEARCH == intent.action) {
            // SearchManager.QUERY returns the text entered by the user
            val query = intent.getStringExtra(SearchManager.QUERY) ?: return
            // If the search term is not null, make the call
            performSearch(query)
        }


        val podcastFeedUrl = intent.getStringExtra(EpisodeUpdateWorker.EXTRA_FEED_URL)
        if (podcastFeedUrl != null) {
            podcastViewModel.viewModelScope.launch {
                val podcastSummaryViewData = podcastViewModel.setActivePodcast(podcastFeedUrl)
                podcastSummaryViewData?.let { podcastSummaryView ->
                    onShowDetails(podcastSummaryView)
                }
            }
        }

    }

    // Called when the Intent is sent from the search widget, that is,
    // when there is a search
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // This saves the intent to be used again
        // when the language is changed or when the
        // device is rotated
        setIntent(intent)
        handleIntent(intent)
    }
    // End of searching a podcast feature





    // Showing the podcast results to the user feature 2
    private fun setupToolbar() {
        // This method makes a toolbar act as an action bar for this Activity
        setSupportActionBar(layout.toolbar)
    }

    private fun setupViewModels() {
        // The repos are set up when the activity is created
        val service = ItunesService.instance
        searchViewModel.iTunesRepo = ItunesRepo(service)

        val rssFeedService = RssFeedService.instance
        val podcastDao = podcastViewModel.podcastDao
        podcastViewModel.podcastRepo = PodcastRepo(rssFeedService, podcastDao)
    }

    // Sets up the recycler view
    private fun updateControls() {

        layout.podcastRecyclerView.setHasFixedSize(true)

        val layoutManager = LinearLayoutManager(this)
        layout.podcastRecyclerView.layoutManager = layoutManager

        val dividerItemDecoration = DividerItemDecoration(
            layout.podcastRecyclerView.context,
            layoutManager.orientation
        )
        layout.podcastRecyclerView.addItemDecoration(dividerItemDecoration)

        podcastListAdapter = PodcastListAdapter(null, this, this)
        layout.podcastRecyclerView.adapter = podcastListAdapter
    }

    // It's called when a user taps on a podcast in the recycler view
    override fun onShowDetails(podcastSummaryViewData: SearchViewModel.PodcastSummaryViewData) {
        // This implementation probably will be changed
        /*
        podcastSummaryViewData.feedUrl?.let {
            showProgressBar()
            podcastViewModel.getPodcast(podcastSummaryViewData)
        }
        */


        podcastSummaryViewData.feedUrl ?: return
            showProgressBar()
            podcastViewModel.viewModelScope.launch(Dispatchers.Main) {
                podcastViewModel.getPodcast(podcastSummaryViewData)
                hideProgressBar()
                // There is a problem with this call
                showDetailsFragment()
            }

    }

    // Subscribing to the LiveData
    private fun createSubscription() {

        // When the data changes, always execute this function with the new data
        val podcastViewDataObserver = Observer<PodcastViewModel.PodcastViewData?> { podcastViewData ->
            hideProgressBar()
            if (podcastViewData != null) {
                showDetailsFragment()
            } else {
                showError("Error loading feed")
            }
        }
        // The owner informs which lifecycle to respect
        podcastViewModel.podcastLiveData.observe(this, podcastViewDataObserver)
    }

    // If there is an error, this method gets called
    // to handle all error cases
    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok_button), null)
            .create()
            .show()
    }

    private fun showProgressBar() {
        layout.progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        layout.progressBar.visibility = View.INVISIBLE
    }

    // End of showing the podcasts to the user




    // Showing the details screen to the user 3
    // It is necessary to know when the fragment is closed
    // to make the recyclerView visible again
    private fun addBackStackListener() {

        // This lambda expression is called everytime fragments are added or removed from the stack
        supportFragmentManager.addOnBackStackChangedListener {
            // When backStackEntryCount is 0, all Fragments have been removed
            if (supportFragmentManager.backStackEntryCount == 0) {
                layout.podcastRecyclerView.visibility = View.VISIBLE
            }
        }
    }

    // OOnly creates a Fragment
    private fun createPodcastDetailsFragment(): PodcastDetailsFragment {

        // This checks if the fragment already exists
        var podcastDetailsFragment  = supportFragmentManager
            .findFragmentByTag(TAG_DETAILS_FRAGMENT) as PodcastDetailsFragment?

        // If there's no no existing Fragment, create a new one
        if (podcastDetailsFragment == null) {
            // Create one if it does not exist
            podcastDetailsFragment = PodcastDetailsFragment.newInstance()
        }

        return podcastDetailsFragment
    }

    // Displays the details fragment
    private fun showDetailsFragment() {

        val podcastDetailsFragment = createPodcastDetailsFragment()

        // THIS "if" SAVED MY LIFE
        if (!podcastDetailsFragment.isAdded) {
            supportFragmentManager.beginTransaction().add(
                R.id.podcastDetailsContainer,
                podcastDetailsFragment,
                TAG_DETAILS_FRAGMENT
            )   // Calling addToBackStack() makes sure that the back button works to close the fragment
                // If you do not add the fragment to back stack and press the back button, the app will close
                .addToBackStack("DetailsFragment")
                .commit()
        }


        // Hides the Recycler View
        layout.podcastRecyclerView.visibility =  View.INVISIBLE
        // Hides the search menu
        searchMenuItem.isVisible = false
    }

    // End of showing the details screen to the user 3

    override fun onSubscribe() {
        // The podcast data is already saved
        // in a variable inside podcastViewModel.
        // So there is no need to pass any data to this function
        podcastViewModel.saveActivePodcast()
        // Remove the podcast details fragment from the backstack
        supportFragmentManager.popBackStack()
    }

    override fun onUnsubscribe() {
        podcastViewModel.deleteActivePodcast()
        supportFragmentManager.popBackStack()
    }

    private fun createEpisodePlayerFragment(): EpisodePlayerFragment {

        // First of all, try to find it. If it already exists, just return it
        // otherwise create a new one
        var episodePlayerFragment = supportFragmentManager.findFragmentByTag(TAG_PLAYER_FRAGMENT)
                as EpisodePlayerFragment?

        if (episodePlayerFragment == null) {
            episodePlayerFragment = EpisodePlayerFragment.newInstance()
        }
        return episodePlayerFragment
    }

    private fun showPlayerFragment() {
        val episodePlayerFragment = createEpisodePlayerFragment()

        if (!(episodePlayerFragment.isAdded)) {

        supportFragmentManager.beginTransaction().replace(
            R.id.podcastDetailsContainer,
            episodePlayerFragment,
            TAG_PLAYER_FRAGMENT
        )
            .addToBackStack("PlayerFragment")
            .commit()

        }

        layout.podcastRecyclerView.visibility = View.INVISIBLE
        searchMenuItem.isVisible = false
    }

    override fun onShowEpisodePlayer(episodeViewData: PodcastViewModel.EpisodeViewData) {
        // Probably this value will be accessed by the Fragment later
        podcastViewModel.activeEpisodeViewData = episodeViewData
        showPlayerFragment()
    }

    private fun scheduleJobs() {
        // Constraints for when the worker should run
        val constraints: Constraints = Constraints.Builder().apply {
            // Execute only when the device is connected to the internet
            setRequiredNetworkType(NetworkType.CONNECTED)
            // Only executes worker when the device is plugged into a power source
            // setRequiresCharging(true)
        }.build()

        // Work to be repeated at set intervals
        val request = PeriodicWorkRequestBuilder<EpisodeUpdateWorker>(
            1, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        // Schedules the work request
        // Unique work is a powerful concept that guarantees that you
        // only have one instance of work with a particular name at a time
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            // Identifies the work request
            TAG_EPISODE_UPDATE_JOB,
            // Replace the existing work with with the new work
            ExistingPeriodicWorkPolicy.REPLACE,
            // The request itself
            request
        )
    }

    companion object {
        // This tag uniquely identifies the details Fragment in the Fragment Manager
        private const val TAG_DETAILS_FRAGMENT = "DetailsFragment"
        private const val TAG_EPISODE_UPDATE_JOB = "com.raywenderlich.podplay.episodes"
        private const val TAG_PLAYER_FRAGMENT = "PlayerFragment"
    }

}