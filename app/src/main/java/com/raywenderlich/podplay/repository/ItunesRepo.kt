package com.raywenderlich.podplay.repository

import com.raywenderlich.podplay.service.ItunesService
import com.raywenderlich.podplay.service.PodcastResponse
import retrofit2.Response

/**
 * The only part of this app that touches the ItunesService
 * This repository hides the service
 */
class ItunesRepo(private val itunesService: ItunesService) {

    suspend fun searchByTerm(term: String): Response<PodcastResponse> {
        return itunesService.searchPodcastByTerm(term)
    }

}