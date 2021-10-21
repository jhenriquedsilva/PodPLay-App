package com.raywenderlich.podplay.adapter

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.raywenderlich.podplay.databinding.SearchItemBinding
import com.raywenderlich.podplay.viewmodel.SearchViewModel.PodcastSummaryViewData

class PodcastListAdapter(
    private var podcastSummaryViewList: List<PodcastSummaryViewData>?,
    private val podcastListAdapterListener: PodcastListAdapterListener,
    private val parentActivity: Activity
): RecyclerView.Adapter<PodcastListAdapter.PodcastViewHolder>() {

    interface PodcastListAdapterListener {
        fun onShowDetails(podcastSummaryViewData: PodcastSummaryViewData)
    }


    // It can access the members of the outer class
    inner class PodcastViewHolder(
        databinding: SearchItemBinding,
        private val podcastListAdapterListener: PodcastListAdapterListener
    ): RecyclerView.ViewHolder(databinding.root) {

        var podcastSummaryViewData: PodcastSummaryViewData? = null
        val nameTextView: TextView = databinding.podcastNameTextView
        val lastUpdatedTextView: TextView = databinding.podcastLastUpdatedTextView
        val podcastImageView: ImageView = databinding.podcastImage

        init {
            // Sets a listener on each item in the recycler view
            databinding.searchItem.setOnClickListener {
                podcastSummaryViewData?.let { podcastSummaryViewData ->
                    podcastListAdapterListener.onShowDetails(podcastSummaryViewData)
                }
            }
        }
    }




    // Every time there is a new search, the recycler view should be updated
    fun setSearchData(podcastSummaryViewData: List<PodcastSummaryViewData>) {
        podcastSummaryViewList = podcastSummaryViewData
        // Populates the recycler view again
        this.notifyDataSetChanged()
    }
                                    // This is the recycler view
    override fun onCreateViewHolder(parent: ViewGroup,  viewType: Int): PodcastViewHolder {

        return PodcastViewHolder(
            SearchItemBinding.inflate(LayoutInflater.from(parent.context),  parent, false),
            podcastListAdapterListener
        )
    }

    override fun onBindViewHolder(holder: PodcastViewHolder, position: Int) {
        // Gets the whole list with all data
        val searchViewList = podcastSummaryViewList ?: return
        // Gets each data in the list
        val searchView = searchViewList[position]
        // Populates the fields of the layouts
        holder.podcastSummaryViewData = searchView
        holder.nameTextView.text = searchView.name
        holder.lastUpdatedTextView.text = searchView.lastUpdated

        // Specific to load the images
        Glide.with(parentActivity) // Tied to the parent activity lifecycle
            .load(searchView.imageUrl)
            .into(holder.podcastImageView)
    }

    override fun getItemCount(): Int {
        // If it is null, the size is actually zero
        return podcastSummaryViewList?.size ?: 0
    }

}