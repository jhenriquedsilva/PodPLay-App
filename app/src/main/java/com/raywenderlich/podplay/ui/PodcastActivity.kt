package com.raywenderlich.podplay.ui

import androidx.appcompat.app.AlertDialog
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.raywenderlich.podplay.R
import com.raywenderlich.podplay.adapter.PodcastListAdapter
import com.raywenderlich.podplay.databinding.ActivityPodcastBinding
import com.raywenderlich.podplay.repository.ItunesRepo
import com.raywenderlich.podplay.repository.PodcastRepo
import com.raywenderlich.podplay.service.FeedService
import com.raywenderlich.podplay.service.ItunesService
import com.raywenderlich.podplay.service.RssFeedService
import com.raywenderlich.podplay.viewmodel.PodcastViewModel
import com.raywenderlich.podplay.viewmodel.SearchViewModel
import kotlinx.coroutines.*

class PodcastActivity : AppCompatActivity(), PodcastListAdapter.PodcastListAdapterListener {

    private val TAG = javaClass.simpleName
    private lateinit var binding: ActivityPodcastBinding
    private val searchViewModel by viewModels<SearchViewModel>()
    private lateinit var podcastListAdapter: PodcastListAdapter
    // Saves a reference to the search icon to hide it and retrieve it back later
    private lateinit var searchMenuItem: MenuItem
    private val podcastViewModel by viewModels<PodcastViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // How to create a view using binding
        binding = ActivityPodcastBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        setupViewModels()
        updateControls()
        handleIntent(intent)
        addBackStackListener()
    }


    // Searching a podcast feature 1

    // Creates the search icon
    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        menuInflater.inflate(R.menu.menu_search, menu)
        // The search action menu is found
        searchMenuItem = menu.findItem(R.id.search_item)
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
            binding.podcastRecyclerView.visibility = View.INVISIBLE
        }

        /**
         * As the fragment adds an action button to the menu, the menu is recreated
         * when the fragment is added. So it is necessary to make sure that the
         * searchMenuItem remains invisible while the recyclerView is invisible
         */
        if (binding.podcastRecyclerView.visibility == View.INVISIBLE) {
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
                binding.toolbar.title = term
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
        setSupportActionBar(binding.toolbar)
    }

    private fun setupViewModels() {
        // The repos are set up when the activity is created
        val service = ItunesService.instance
        searchViewModel.iTunesRepo = ItunesRepo(service)

      //  podcastViewModel.podcastRepo = PodcastRepo(FeedService.instance)
    }

    // Sets up the recycler view
    private fun updateControls() {

        binding.podcastRecyclerView.setHasFixedSize(true)

        val layoutManager = LinearLayoutManager(this)
        binding.podcastRecyclerView.layoutManager = layoutManager

        val dividerItemDecoration = DividerItemDecoration(
            binding.podcastRecyclerView.context,
            layoutManager.orientation
        )
        binding.podcastRecyclerView.addItemDecoration(dividerItemDecoration)

        podcastListAdapter = PodcastListAdapter(null, this, this)
        binding.podcastRecyclerView.adapter = podcastListAdapter
    }

    // It's called when a user taps on a podcast in the recycler view
    override fun onShowDetails(podcastSummaryViewData: SearchViewModel.PodcastSummaryViewData) {

        val feedUrl = podcastSummaryViewData.feedUrl?.let {
            showProgressBar()
            podcastViewModel.getPodcast(podcastSummaryViewData)
        }
    }

    private fun createSubscription() {
        podcastViewModel.podcastLiveData.observe(this, {
            hideProgressBar()
            if (it != null) {
                showDetailsFragment()
            } else {
                showError("Error loading feed")
            }
        })
    }

    // If there is an error, this method gets called
    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok_button), null)
            .create()
            .show()
    }

    private fun showProgressBar() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        binding.progressBar.visibility = View.INVISIBLE
    }

    // End of showing the podcasts to the user




    // Showing the details screen to the user 3

    // It is necessary to know when the fragment is closed
    // to make the recyclerView visible again
    private fun addBackStackListener() {
        // This lambda responds to changes inn the Fragment backstack
        // It's called when fragments are added or removed from the stack
        supportFragmentManager.addOnBackStackChangedListener {
            // When backStackEntryCount is 0, all Fragments have been
            // removed
            if (supportFragmentManager.backStackEntryCount == 0) {
                binding.podcastRecyclerView.visibility = View.VISIBLE
            }
        }
    }

    private fun createPodcastDetailsFragment(): PodcastDetailsFragment {

        // This checks if the fragment already exists
        var podcastDetailsFragment  = supportFragmentManager
            .findFragmentByTag(TAG_DETAILS_FRAGMENT) as PodcastDetailsFragment?

        if (podcastDetailsFragment == null) {
            // Create one if it does not exist
            podcastDetailsFragment = PodcastDetailsFragment.newInstance()
        }

        return podcastDetailsFragment
    }

    // Displays the details fragment
    private fun showDetailsFragment() {

        val podcastDetailsFragment = createPodcastDetailsFragment()

        supportFragmentManager.beginTransaction().add(
            R.id.podcastDetailsContainer,
            podcastDetailsFragment,
            TAG_DETAILS_FRAGMENT
        )   // Calling this function makes sure that the back button works to close the fragment
            // If you do not add this and press the back button, the app will close
            .addToBackStack("DetailsFragment").commit()

        // Still takes up space
        binding.podcastRecyclerView.visibility =  View.INVISIBLE
        // Hides the search menu
        searchMenuItem.isVisible = false
    }

    // End of showing the details screen to the user 3

    companion object {
        // This tag uniquely identifies the details Fragment in the Fragment Manager
        private const val TAG_DETAILS_FRAGMENT = "DetailsFragment"
    }
}