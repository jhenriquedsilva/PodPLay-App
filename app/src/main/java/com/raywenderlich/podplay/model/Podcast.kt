package com.raywenderlich.podplay.model

import androidx.room.*

// Retrieves the podcast details and return it to the view model
// The foreignKeys attributes let you define relationships between tables
// and add constraints on them
@Entity(
    tableName = "podcasts",
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
            onDelete = ForeignKey.CASCADE
        )
    ],
    // Indexes make the query faster. It's necessary to
    // index some columns
    // Creates an index on podcastId column on Episode table
    indices = [Index(value = ["podcastId"])]
)
data class Podcast(
    // Annotate a column to make it a primary key
    @PrimaryKey(autoGenerate = true) var id: Long? = null,
    // If I want the column to have a different name from the
    // property name, I can use the @ColumnInfo() annotation for that
    @ColumnInfo(name = "feedUrl") var feedUrl: String = "", // Location of the RSS feed
    @ColumnInfo(name = "feedTitle") var feedTitle: String = "",
    @ColumnInfo(name = "feedDesc") var feedDesc: String = "",
    @ColumnInfo(name = "imageUrl") var imageUrl: String = "",
    @ColumnInfo(name = "lastUpdated") var lastUpdated: String = "",
    @ColumnInfo(name = "episodes") var episodes: List<Episode> = listOf()
)

/**

That's away to represent the relation between these two classes
But that's a recent to do that

Data class that represents the relaation
data class PodcastsWithEpisodes(
    @Embedded val podcast: Podcast,
    @Relation(
        parentColumn = "id",
        entityColumn = "podcastId"
    )
    val episodes: List<Episode>
)

Query on DAO that retrieves the values
@Transaction
@Query("SELECT * FROM podcasts")
fun getPodcastsWithEpisodes(): List<PodcastsWithEpisodes>

*/