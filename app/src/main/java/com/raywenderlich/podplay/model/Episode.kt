package com.raywenderlich.podplay.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

// Defines the data for a single podcast episode
@Entity(tableName = "episodes",
    foreignKeys = [
        // This object relates the podcastId property in the Episode
        // entity to the property id in the Podcast entity
        ForeignKey(
            // Parent entity
            entity = Podcast::class,
            // Defines the columns names on the parent entity: Podcast
            parentColumns = ["id"],
            // Defines the columns names in the child entity: Episode
            childColumns = ["podcastId"],
            /** Behavior when a parent entity is deleted
             * In this case, CASCADE means that when a podcast is deleted,
             * all episodes should be deleted
             */
            /** Behavior when a parent entity is deleted
             * In this case, CASCADE means that when a podcast is deleted,
             * all episodes should be deleted
             */
            onDelete = ForeignKey.CASCADE
        )
    ],
    // Indexes make the query faster. It's necessary to
    // index some columns
    // Creates an index on podcastId column on Episode table
    indices = [Index(value = ["podcastId"])])
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