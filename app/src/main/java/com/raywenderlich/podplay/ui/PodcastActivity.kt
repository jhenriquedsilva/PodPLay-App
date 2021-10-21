package com.raywenderlich.podplay.ui

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.raywenderlich.podplay.R
import com.raywenderlich.podplay.adapter.PodcastListAdapter
import com.raywenderlich.podplay.databinding.ActivityPodcastBinding
import com.raywenderlich.podplay.repository.ItunesRepo
import com.raywenderlich.podplay.service.ItunesService
import com.raywenderlich.podplay.viewmodel.SearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PodcastActivity : AppCompatActivity(), PodcastListAdapter.PodcastListAdapterListener {

    private val TAG = javaClass.simpleName
    private lateinit var binding: ActivityPodcastBinding
    private val searchViewModel by viewModels<SearchViewModel>()
    private lateinit var podcastListAdapter: PodcastListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // How to create a view using binding
        binding = ActivityPodcastBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        setupViewModels()
        updateControls()
        handleIntent(intent)
    }

    private fun setupToolbar() {
        // This method makes a toolbar act as an action bar
        // for this Activity
        setSupportActionBar(binding.toolbar)
    }

    private fun setupViewModels() {
        val service = ItunesService.instance
        searchViewModel.iTunesRepo = ItunesRepo(service)
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

    // Should be called when a user taps on a podcast in the recycler view
    override fun onShowDetails(podcastSummaryViewData: SearchViewModel.PodcastSummaryViewData) {
        TODO("Not yet implemented")
    }

    private fun showProgressBar() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        binding.progressBar.visibility = View.INVISIBLE
    }


    // Searching a podcast feature

    // Creates the search icon
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)

        val searchMenuItem = menu?.findItem(R.id.search_item)
        val searchView = searchMenuItem?.actionView as SearchView

        var searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager

        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))

        return true
    }

    // Create the network request
    private fun performSearch(term: String) {
        // Starts fetching data
        showProgressBar()
        GlobalScope.launch {
            // List with ready-to-use podcasts
            val results = searchViewModel.searchPodcasts(term)
            // Changes to the main thread again
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
            performSearch(query)
        }
    }

    // Called when the Intent is sent from the search widget
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // This saves the intent to be used again
        // when there is a configuration change
        setIntent(intent)
        handleIntent(intent)
    }

    // End of searching a podcast feature
}