package com.raywenderlich.podplay.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.raywenderlich.podplay.databinding.EpisodeBinding
import com.raywenderlich.podplay.viewmodel.PodcastViewModel

// Adapter of each episode
class EpisodeListAdapter(
    private var episodeViewList: List<PodcastViewModel.EpisodeViewData>?
): RecyclerView.Adapter<EpisodeListAdapter.EpisodeViewHolder>() {

    inner class EpisodeViewHolder(
        val databinding: EpisodeBinding
    ) : RecyclerView.ViewHolder(databinding.root) {


        // var episodeViewData: PodcastViewModel.EpisodeViewData? = null
        // With these lines of code, it is not necessary to access the databinding.
        // Just access the class instance proporties
        val titleTextView: TextView = databinding.titleView
        val descTextView: TextView = databinding.descView
        val durationTextView: TextView = databinding.durationView
        val releaseDateTextView: TextView = databinding.releaseDateView

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        return EpisodeViewHolder(
            EpisodeBinding.inflate(LayoutInflater.from(parent.context), parent,false)
        )
    }

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {

        // There is an automatic cast over here
        // Returns a non-nullable type
        val episodeViewList = episodeViewList ?: return
        val episodeViewData = episodeViewList[position]

        // holder.episodeViewData = episodeViewData
        holder.titleTextView.text = episodeViewData.title
        holder.descTextView.text = episodeViewData.description
        holder.durationTextView.text = episodeViewData.duration
        holder.releaseDateTextView.text = episodeViewData.releaseDate.toString()


    }

    override fun getItemCount(): Int {
        // If there is no elvis operator, there is error
        // Elvis makes sure that the result will be Int, not Int?
        return episodeViewList?.size ?: 0
    }
}