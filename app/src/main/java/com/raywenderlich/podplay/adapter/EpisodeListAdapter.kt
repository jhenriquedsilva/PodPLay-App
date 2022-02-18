package com.raywenderlich.podplay.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.raywenderlich.podplay.databinding.EpisodeBinding
import com.raywenderlich.podplay.util.DateUtils
import com.raywenderlich.podplay.util.HtmlUtils
import com.raywenderlich.podplay.viewmodel.PodcastViewModel

// Adapter of each episode
class EpisodeListAdapter(
    private var episodeViewList: List<PodcastViewModel.EpisodeViewData>?,
    private val episodeListAdapterListener: EpisodeListAdapterListener
): RecyclerView.Adapter<EpisodeListAdapter.EpisodeViewHolder>() {

    interface EpisodeListAdapterListener {
        fun onSelectedEpisode(episodeViewData: PodcastViewModel.EpisodeViewData)
    }

    inner class EpisodeViewHolder(
        databinding: EpisodeBinding,
        val episodeListAdapterListener: EpisodeListAdapterListener
    ) : RecyclerView.ViewHolder(databinding.root) {

        init {
            databinding.root.setOnClickListener {
                // The episode view data cannot be null because
                // it is necessary to get the media url
                episodeViewData?.let { episodeViewData ->
                    episodeListAdapterListener.onSelectedEpisode(episodeViewData)
                }
            }
        }


        var episodeViewData: PodcastViewModel.EpisodeViewData? = null
        // With these lines of code, it is not necessary to access the databinding.
        // Just access the class instance proporties
        val titleTextView: TextView = databinding.titleView
        val descTextView: TextView = databinding.descView
        val durationTextView: TextView = databinding.durationView
        val releaseDateTextView: TextView = databinding.releaseDateView

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        return EpisodeViewHolder(
            EpisodeBinding.inflate(LayoutInflater.from(parent.context), parent,false),
            episodeListAdapterListener
        )
    }

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {

        // There is an automatic cast over here
        // Returns a non-nullable type
        val episodeViewList = episodeViewList ?: return
        val episodeViewData = episodeViewList[position]

        holder.episodeViewData = episodeViewData
        holder.titleTextView.text = episodeViewData.title
        holder.descTextView.text = HtmlUtils.htmlToSpannable(episodeViewData.description ?: "")
        holder.durationTextView.text = episodeViewData.duration
        holder.releaseDateTextView.text = episodeViewData.releaseDate


    }

    override fun getItemCount(): Int {
        // If there is no elvis operator, there is error
        // Elvis makes sure that the result will be Int, not Int?
        return episodeViewList?.size ?: 0
    }
}