package com.raywenderlich.podplay.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.raywenderlich.podplay.R
import com.raywenderlich.podplay.ui.PodcastActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.URL

// Other apps can use this browser to playback media
// In this class will happen the hard work
class PodplayMediaService: MediaBrowserServiceCompat(), PodplayMediaCallback.PodplayMediaListener {

    // Media session is used to create and control the media player
    // Other systems can access it
    // It responds to other apps interactions
    private lateinit var mediaSession: MediaSessionCompat

    private fun getPausePlayActions(): Pair<NotificationCompat.Action, NotificationCompat.Action> {

        val pauseAction = NotificationCompat.Action(
            R.drawable.ic_pause_white,
            getString(R.string.pause),
            // Creates a pending intent that triggers a playback action on the media service
            MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PAUSE)
        )

        val playAction = NotificationCompat.Action(
            R.drawable.ic_play_arrow_white,
            getString(R.string.play),
            // Creates a pending intent that triggers a playback action on the media service
            MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY)
        )

        return Pair(pauseAction, playAction)
    }

    // Checks if the media player is playing media
    // to decide which action to use: play or pause
    private fun isPlaying(): Boolean {
        return mediaSession.controller.playbackState != null &&
                mediaSession.controller.playbackState.state ==
                PlaybackStateCompat.STATE_PLAYING
    }

    // Starts the podcast activity when the notification is tapped
    // @SuppressLint("UnspecifiedImmutableFlag")
    private fun getNotificationIntent(): PendingIntent {
        val openActivityIntent =
            Intent(this, PodcastActivity::class.java)
                openActivityIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP

        return PendingIntent.getActivity(
            this@PodplayMediaService,
            0,
            openActivityIntent,
            // How the intent should be used
            PendingIntent.FLAG_CANCEL_CURRENT
        )
    }

    // Creates a notification channel for newer versions of android
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (notificationManager.getNotificationChannel(PLAYER_CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                PLAYER_CHANNEL_ID,
                "Player",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Create the notification for the foreground service
    private fun createNotification(
        mediaDescription: MediaDescriptionCompat,
        bitmap: Bitmap?
    ): Notification {
        // Opens PodcastActivity
        val notificationIntent = getNotificationIntent()
        // The action are created
        val (pauseAction, playAction) = getPausePlayActions()
        //
        val notification = NotificationCompat.Builder(this@PodplayMediaService, PLAYER_CHANNEL_ID)

        notification
            .setContentTitle(mediaDescription.title)
            .setContentText(mediaDescription.subtitle)
             // Sets the album art to display on the notification
            .setLargeIcon(bitmap)
             // Podplay is launched when the notification is tapped
            .setContentIntent(notificationIntent)
             // Send an ACTION_STOP command to the service if
             // the user swipes away the notification
            .setDeleteIntent(
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_STOP)
            )
             // make sure the transport controls are visible on the lock screen
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
             // Set the icon to display in the status bar
            .setSmallIcon(R.drawable.ic_episode_icon)
             // Add action based on playback state
            .addAction(if (isPlaying()) pauseAction else playAction)
             // It can display up to five transport controls in the expanded view
            .setStyle(
                // MediaStyle automatically displays and handles the
                // playback controls
                androidx.media.app.NotificationCompat.MediaStyle()
                     // The system uses this as a flag to activate
                     // special features such as showing album artwork
                     // and playback controls on the lock screen
                    .setMediaSession(mediaSession.sessionToken)
                     // Indicates which action buttons to display in
                     // compact view mode
                    .setShowActionsInCompactView(0)
                     // Displays a cancel button on versions of Android
                     // before Lollipop
                    .setShowCancelButton(true)
                     // Pending intent to use when the cancel button is tapped
                    .setCancelButtonIntent(
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                            this, PlaybackStateCompat.ACTION_STOP
                        )
                    )
            )

        return notification.build()
    }

    // Starts the service and shows the notification
    private fun displayNotification() {

        // Shows the notification based on the Metadata
        if (mediaSession.controller.metadata == null) {
            return
        }

        // Android O requires a notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        // Get the description of this metadata to display
        // Controller: allows an app to interact with an ongoing media session
        val mediaDescription = mediaSession.controller.metadata.description

        GlobalScope.launch {
            // Allow to load the image over the network
            // The URL class is used to represent address of resources
            val iconUrl = URL(mediaDescription.iconUri.toString())
            // Loads the image from the internet and stores it in the bitmap object
            // Decode an input stream into a bitmap.
            val bitmap = BitmapFactory.decodeStream(iconUrl.openStream())
            Log.d("Testing", "Bitmap is equal to null? ${bitmap == null}")

            // After the image is loaded, the notification is created using
            // the description of the episode and the album art bitmap
            val notification = createNotification(mediaDescription, bitmap)

            ContextCompat.startForegroundService(
                this@PodplayMediaService,
                Intent(this@PodplayMediaService, PodplayMediaService::class.java)
            )

            // Displays the notification icon
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    // Display the notification when the state changes
    // between play and pause
    override fun onStateChanged() {
        displayNotification()
    }

    // Stops the service and removes it from the
    // foreground
    override fun onStopPlaying() {
        // Stop the service
        stopSelf()
        // Remove this service from foreground state, allowing it to be killed
        // if more memory is needed. This does not stop the service from running
        stopForeground(true)
    }

    // Only removes the service from the foreground
    // but does not remove the notification
    override fun onPausePLaying() {
        // Remove this service from foreground state, allowing it to be killed
        // if more memory is needed. This does not stop the service from running
        stopForeground(false)
    }

    // The media session should be created and initialized
    override fun onCreate() {
        super.onCreate()
        createMediaSession()
    }


    // When the service first starts, a media session should be created
    // These three steps are done everytime the onCreate method is called
    private fun createMediaSession() {
        mediaSession = MediaSessionCompat(this, "PodplayMediaService")
        // Links the service to the media session through the token
        sessionToken = mediaSession.sessionToken
        // Lack od PlaybackStateCompat
        // The last parameter is set to null by default
        val callback = PodplayMediaCallback(this, mediaSession)
        // The service will listen for callbacks
        callback.listener = this
        mediaSession.setCallback(callback)
    }

    // Called when the user dismisses the app from the recent
    // applications list
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        mediaSession.controller.transportControls.stop()
    }

    // These two following methods control client connections
    // They work together with the media browser
    // MEDIA BROWSER CALLS THESE TWO METHODS TO GET A LIST OOF BROWSABLE MENU ITEMS
    // TO SHOW TO THE USER

    // This one controls access to the service
    // Returns the root media ID of the content tree
    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot
    {
        // If this method returns null, this connection is refused
        // BrowserRoot is a root id that represents your content hierarchy
        // Returns an empty root media
        return BrowserRoot(PODPLAY_EMPTY_ROOT_MEDIA_ID, null)
    }

    // This one provides the ability for a client to build and display
    // a menu of the MediaBrowserService's content hierarchy
    // Returns a list of child media items given a parent media ID
    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>)
    {
        // Returns an empty list of children for the empty root ID
        if (parentId == PODPLAY_EMPTY_ROOT_MEDIA_ID) {
            result.sendResult(null)
        }
    }


    companion object {
        private const val NOTIFICATION_ID = 1
        private const val PLAYER_CHANNEL_ID = "podplay_player_channel"
        private const val PODPLAY_EMPTY_ROOT_MEDIA_ID = "podplay_empty_root_media_id"
    }

}