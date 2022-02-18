package com.raywenderlich.podplay.service

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat

// Other apps can use this browser to playback media
// In this class will happen the hard work
class PodplayMediaService: MediaBrowserServiceCompat() {

    // Media session is used to create and control the media player
    // Other systems can access it
    // It responds to other apps interactions
    private lateinit var mediaSession: MediaSessionCompat

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
        mediaSession.setCallback(callback)
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
        private const val PODPLAY_EMPTY_ROOT_MEDIA_ID = "podplay_empty_root_media_id"
    }
}