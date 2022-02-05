package com.raywenderlich.podplay.service

import android.util.Log
import com.raywenderlich.podplay.BuildConfig
import com.raywenderlich.podplay.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.w3c.dom.Node
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Url
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory

class RssFeedService private constructor() {

    val TAG = "RssFeedService"

    // Turns the Document object into an RssFeedResponse
    // This method is recursive. Operates on one node at a time
    // Passes the child node to the next call
    private fun domToRssFeedResponse(node: Node, rssFeedResponse: RssFeedResponse) {

        // Checks if it is an XML element
        if (node.nodeType == Node.ELEMENT_NODE) {
            val nodeName = node.nodeName
            val parentName = node.parentNode.nodeName

            val grandParentName = node.parentNode.parentNode?.nodeName ?: ""

            // This part parses each episode item
            if (parentName == "item" && grandParentName == "channel") {
                // 3
                val currentItem = rssFeedResponse.episodes?.last()

                if (currentItem != null) {
                    // 4
                    when (nodeName) {
                        "title" -> currentItem.title = node.textContent
                        "description" -> currentItem.description = node.textContent
                        "itunes:duration" -> currentItem.duration = node.textContent
                        "guid" -> currentItem.guid = node.textContent
                        "pubDate" -> currentItem.pubDate = node.textContent
                        "link" -> currentItem.link = node.textContent
                        "enclosure" -> {
                            currentItem.url = node.attributes.getNamedItem("url").textContent
                            currentItem.type = node.attributes.getNamedItem("type").textContent
                        }
                    }
                }
            }


            // If the current node is a child of the channel node
            if (parentName == "channel") {
                when (nodeName) {
                    "title" ->  rssFeedResponse.title = node.textContent
                    "description" -> rssFeedResponse.description = node.textContent
                    "itunes:summary" -> rssFeedResponse.summary = node.textContent
                    "item" -> rssFeedResponse.episodes?.add(RssFeedResponse.EpisodeResponse())
                    "pubDate" -> rssFeedResponse.lastUpdated = DateUtils.xmlDateToDate(node.textContent)
                }
            }
        }

        val nodeList = node.childNodes
        if (nodeList.length > 0) {
            if (node.nodeName == "item") {
                Log.d(TAG, "The ${node.nodeName} has ${nodeList.length} elements")
            }
            for (i in 0 until nodeList.length) {
                val childNode  = nodeList.item(i)
                domToRssFeedResponse(childNode, rssFeedResponse)
            }
        }
    }

    // Reads the RSS file into a Document object
    suspend fun getFeed(xmlFileURL: String): RssFeedResponse? {

        // The interface is bellow and the instance is assigned below
        var service: FeedService

        // Debug purposes
        // It could be removed
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY

        // To make a call with OkHttpClient, it's necessary an
        // HTTP Request object
        val client = OkHttpClient().newBuilder()
            .connectTimeout(30,TimeUnit.SECONDS)
            .writeTimeout(30,TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)

        // Interceptor is only added for debug builds
        // Could be removed
        if (BuildConfig.DEBUG) {
            client.addInterceptor(interceptor)
        }
        client.build()

        // Treats the baseUrl construction to avoid problems with Retroif2
        // The create method should be called
        var retrofit = Retrofit.Builder()
            .baseUrl("${xmlFileURL.split("?")[0]}/")
            .build()

        // The service is created
        service = retrofit.create(FeedService::class.java)

        try {
            // The XML is returned
            val result = service.getFeed(xmlFileURL)
            // If the code is equal to or greater than 400, this means there is an error
            if (result.code() >= 400) {
                println("server error, ${result.code()}, ${result.errorBody()}")
                return null
            } else {

                var rssFeedResponse: RssFeedResponse? = null

                // Parsing occurs over here
                // Retrofit is not good at parsing XML
                val dbFactory = DocumentBuilderFactory.newInstance()
                val dBuilder = dbFactory.newDocumentBuilder()
                // Starts a coroutine to run in the background
                withContext(Dispatchers.IO)  {
                    // ResponseBody = A one-shot stream from the origin server
                    // to the client application with the raw bytes of the response body
                    // Parses the XML into a document
                    // InputStream is an abstract class that is the superclass of all classes representing an input stream of bytes
                    // doc is the root element, that is, rss
                    val doc = dBuilder.parse(result.body()?.byteStream())
                    val rss = RssFeedResponse(episodes = mutableListOf())
                    // A reference is passed to this function. So, the changes are reflected outside
                    // the function
                    domToRssFeedResponse(doc, rss)
                    // Maybe it's possible to that in the logcat
                    //println(rss)
                    Log.i(TAG, "Parsing finished")
                    rssFeedResponse = rss
                }
                // Returns the class with the data
                return rssFeedResponse
            }
        } catch (t: Throwable) {
            println("error, ${t.localizedMessage}")
        }
        return null
    }

    // Way to use the singleton design pattern
    companion object {
        // The last line is always returned
        val instance: RssFeedService by lazy { RssFeedService() }
    }
}

// THESE TWO ARE NOT LINKED TOGETHER
interface FeedService {

    @Headers(
        "Content-Type: application/xml; charset=utf-8",
        "Accept: application/xml"
    )
    @GET // Gets a URL pointing to the RSS file            Response bytes returns raw bytes
    suspend fun getFeed(@Url xmlFileUrl: String): Response<ResponseBody>

}