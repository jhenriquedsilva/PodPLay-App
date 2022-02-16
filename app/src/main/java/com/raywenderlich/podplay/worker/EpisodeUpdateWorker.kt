package com.raywenderlich.podplay.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.raywenderlich.podplay.R
import com.raywenderlich.podplay.db.PodPlayDatabase
import com.raywenderlich.podplay.repository.PodcastRepo
import com.raywenderlich.podplay.service.RssFeedService
import com.raywenderlich.podplay.ui.PodcastActivity
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async

// When the task is ready to be executed, this class is activated
class EpisodeUpdateWorker(context: Context, parameters: WorkerParameters): CoroutineWorker(context,parameters) {

    override suspend fun doWork(): Result {
        // coroutineScope is a coroutine builder. It creates
        // a suspending coroutine block and returns the last line
        // of the block
        return coroutineScope {
            val job = async {
                val db = PodPlayDatabase.getInstance(applicationContext, this)
                val repo = PodcastRepo(RssFeedService.instance, db.podcastDao())

                val podcastUpdates = repo.updatePodcastEpisodes()

                // Channels are allowed from API 26 on
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    createNotificationChannel()
                }

                // The notifications are shown after the channel is created
                for (podcastUpdate in podcastUpdates) {
                    displayNotification(podcastUpdate)
                }
            }
            // Suspends the function until
            // the coroutine is completed
            job.await()

            Result.success()
        }
    }

    // This method should only be called when running on API 26
    // because channels started on this API
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        // If a channel is not created,
        // the notification will not appear

        // Notification manager is retrieved
        val notificationManager = applicationContext.getSystemService(NOTIFICATION_SERVICE)
                as NotificationManager

        // Notification manager checks if the channel already exists
        if (notificationManager.getNotificationChannel(EPISODE_CHANNEL_ID) == null) {
            // If the channel does not exist, a new one is created
            val channel = NotificationChannel(EPISODE_CHANNEL_ID,"Episodes", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Creates a single notification
    private fun displayNotification(podcastInfo: PodcastRepo.PodcastUpdateInfo) {

        // The notification manager needs to know what content to display
        // when the user taps the notification
        val contentIntent = Intent(applicationContext, PodcastActivity::class.java)
        contentIntent.putExtra(EXTRA_FEED_URL,podcastInfo.feedUrl)

        val pendingContentIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat
             // The channel ID is ignored by older versions of Android
             // The constructed Notification will be posted on this NotificationChannel
            .Builder(applicationContext, EPISODE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_episode_icon)
            .setContentTitle(applicationContext.getString(R.string.episode_notification_title))
            .setContentText(applicationContext.getString(R.string.episode_notification_text,podcastInfo.newCount,podcastInfo.name))
             // Number of new items associated with
             // this notification
            .setNumber(podcastInfo.newCount)
             // Clear notification when a user taps on it
            .setAutoCancel(true)
            .setContentIntent(pendingContentIntent)
            .build()

        // Notification manager is retrieved
        val notificationManager = applicationContext
            .getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // The notification manager is instructed to notify the user
        // with the notification object created by the builder
        // Tag and id is used to identify the notification
        notificationManager.notify(podcastInfo.name,0,notification)
    }


    companion object {
        const val EPISODE_CHANNEL_ID = "podplay_episodes_channel"
        const val EXTRA_FEED_URL = "PodcastFeedUrl"
    }
}