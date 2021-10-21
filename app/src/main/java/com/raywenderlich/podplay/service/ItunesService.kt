package com.raywenderlich.podplay.service

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * This interface is a direct representation of the API that is accessed
 */
interface ItunesService {

    // https://itunes.apple.com/search?term=Android+Developer&media=podcast
    // Function annotation. Applies to a function
    // @param: The path to the endpoint that should be called
    @GET("/search?media=podcast")
    suspend fun searchPodcastByTerm(@Query("term") term: String): Response<PodcastResponse>

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

                Retrofit.Builder()
                .baseUrl("https://itunes.apple.com")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ItunesService::class.java)
        }
    }
}
