package com.raywenderlich.podplay.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

// Defines the data for a single podcast episode
@Entity()
data class Episode(
    @PrimaryKey var guid: String = "", // Unique identifier provided in the RSS feed for an episode
    var podcastId: Long? = null,
    var title: String = "",
    var description: String = "",
    var mediaUrl: String = "", // The location of the episode media. Either an audio or video file
    var mimeType: String = "", // Determines the type of file located at mediaUrl
    var releaseDate: String = "",
    var duration: String = "",
)