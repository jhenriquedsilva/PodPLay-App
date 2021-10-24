package com.raywenderlich.podplay.service

import com.raywenderlich.podplay.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Url
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory

class RssFeedService private constructor() {

    // Reads the RSS file into a Document object
    suspend fun getFeed(xmlFileURL: String): RssFeedResponse? {
        var service: FeedService

        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY

        val client = OkHttpClient().newBuilder()
            .connectTimeout(30,TimeUnit.SECONDS)
            .writeTimeout(30,TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)

        // Interceptor is only added for debug builds
        if (BuildConfig.DEBUG) {
            client.addInterceptor(interceptor)
        }
        client.build()

        var retrofit = Retrofit.Builder()
            .baseUrl("${xmlFileURL.split("?")[0]}/")
            .build()
        service = retrofit.create(FeedService::class.java)

        try {
            val result = service.getFeed(xmlFileURL)
            if (result.code() >= 400) {
                println("server error, ${result.code()}, ${result.errorBody()}")
                return null
            } else {
                var rssFeedResponse: RssFeedResponse? = null

                // Parsing occurs over here
                val dbFactory = DocumentBuilderFactory.newInstance()
                val dBuilder = dbFactory.newDocumentBuilder()
                withContext(Dispatchers.IO)  {
                    val doc = dBuilder.parse(result.body()?.byteStream())
                }
                // Finishes over here

                return rssFeedResponse
            }
        } catch (t: Throwable) {
            println("error, ${t.localizedMessage}")
        }
        return null
    }

    // Way to use the singleton design pattern
    companion object {

        val instance: RssFeedService by lazy {
            RssFeedService()
        }

    }
}

interface FeedService {

    @Headers(
        "Content-Type: application/xml; charset=utf-8",
        "Accept: application/xml"
    )
    @GET
    suspend fun getFeed(@Url xmlFileUrl: String): Response<ResponseBody>

}