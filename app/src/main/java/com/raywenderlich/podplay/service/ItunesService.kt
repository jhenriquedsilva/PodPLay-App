package com.raywenderlich.podplay.service

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit is good because it does the background task and lets you concentrate
 * on writing functionalities
 */

/**
 * This service is called when the user searches for podcasts
 */

/**
 * THIS INTERFACE IS A DIRECT REPRESENTATION OF THE API THAT YOU'RE ACCESSING
 */
interface ItunesService {

    /**
     * Retrofit URL-encodes automatically the queries
     */
    // The GET annotation takes only one param: the path to the endpoint that should be called
    @GET("/search?media=podcast")
    // Query indicates that the parameter should be added as query term to the endpoint
    suspend fun searchPodcastByTerm(@Query("term") term: String): Response<PodcastResponse>

    /**
     * This code not necessarily needs to be over here. BBut it was allocated here through a
     * companion object
     */
    companion object {

        /**
         * This is called property delegation (keyword by)
         * You can delegate the properties getters and setters
         * to a class. The one used over here is the Lazy<T>, that is
         * return by the lazy function. The first call to get() executes
         * the lambda passed to lazy() and remember the results. So after the variable
         * is initialized, the same instance will be returned
         */
        val instance: ItunesService by lazy {

            /**
             * Creates a concrete implementation of the interface
             * and Retrofit supplies the method bodies
             */
            Retrofit.Builder()
                .baseUrl("https://itunes.apple.com")
                .addConverterFactory(GsonConverterFactory.create()) // Creates an instance of GsonConverterFactory
                .build()
                .create(ItunesService::class.java)
        }
    }

}
