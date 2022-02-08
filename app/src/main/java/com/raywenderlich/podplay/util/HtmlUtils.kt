package com.raywenderlich.podplay.util

import android.os.Build
import android.text.Html
import android.text.Spanned
import android.util.Log

object HtmlUtils {

    val TAG = "HtmlUtils"

    // Convert a html string into a spanned character sequence
    fun htmlToSpannable(htmlDesc: String): Spanned {
        Log.d(TAG, "Before transsformed by Regex")
        var newHtmlDesc = htmlDesc.replace("\n".toRegex(), "")
        Log.d(TAG, "After transformed by Regex")
        newHtmlDesc = newHtmlDesc.replace("(<(/)img>) | (<img.+?>)".toRegex(),"")

        val descSpan: Spanned
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            descSpan = Html.fromHtml(newHtmlDesc,Html.FROM_HTML_MODE_LEGACY)
        } else {
            // Allow the code to compile even though it is deprecated
            @Suppress("DEPRECATION")
            descSpan = Html.fromHtml(newHtmlDesc)
        }
        return descSpan
    }
}