package com.qfree.clubvolonterov

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.io.InputStream
import java.lang.Exception
import android.webkit.ValueCallback





class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        webView.webViewClient = SimpleWebViewClientImpl(this)
        webView.getSettings().setJavaScriptEnabled(true)
        webView.getSettings().cacheMode = WebSettings.LOAD_DEFAULT
        onbHomeClick(null)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    fun onbHomeClick(view: View?) {
        webView.loadUrl("https://www.club-volonterov.ru/phpBB3/index.php")
    }

    fun onbActiveClick(view: View?) {
        webView.loadUrl("https://www.club-volonterov.ru/phpBB3/search.php?search_id=active_topics")
    }

    fun onbMessagesClick(view: View?) {
        webView.loadUrl("https://www.club-volonterov.ru/phpBB3/ucp.php?i=pm&folder=inbox")
    }
}

class SimpleWebViewClientImpl(activity: Activity?) : WebViewClient() {
    private var activity: Activity? = null

    override fun onLoadResource(view: WebView, url: String?) {
        super.onLoadResource(view, url)

        injectCSS(view)
    }

    override fun shouldOverrideUrlLoading(webView: WebView, url: String): Boolean {
        if (url.contains("www.club-volonterov.ru/phpBB3/")) {
            return false
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        activity!!.startActivity(intent)
        return true
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
//        view.visibility = View.INVISIBLE

//        if (!url.contains("&mobapp")) {
//            view.visibility = View.INVISIBLE
//            var htmlData = ""
//            var baseUrl = ""
//            val cm: CookieManager = CookieManager.getInstance()
//            val cookie = cm.getCookie(url)
//
//            GlobalScope.launch(Dispatchers.IO) {
//                try {
//                    val doc: Document = Jsoup.connect(url + if (url.contains("?")) "&mobapp" else "?&mobapp")
//                        .header("Cookie", cookie)
//                        .get()
//                    doc.head().appendElement("link")
//                        .attr("rel", "stylesheet")
//                        .attr("type", "text/css")
//                        .attr("href", "file:///android_asset/style.css")
//                    htmlData = doc.html()
//                    baseUrl = doc.baseUri()
//                } catch (e: IOException) {
//                    e.printStackTrace()
//                }
//                GlobalScope.launch(Dispatchers.Main) {
//                    view.loadDataWithBaseURL(
//                        baseUrl,
//                        htmlData,
//                        "text/html",
//                        "UTF-8",
//                        null)
//                }
//            }
//        }
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
//        view.visibility = View.VISIBLE
//        injectCSS(view)
//
//        GlobalScope.launch(Dispatchers.IO) {
//            Thread.sleep(1000)
//            GlobalScope.launch(Dispatchers.Main) {
//                view.visibility = View.VISIBLE
//            }
//        }
    }

    init {
        this.activity = activity
    }

    private fun injectCSS(view: WebView) {
        try {
//            val inputStream: InputStream = activity!!.assets.open("style.css")
//            val buffer = ByteArray(inputStream.available())
//            inputStream.read(buffer)
//            inputStream.close()
//            val encoded: String = Base64.encodeToString(buffer, Base64.NO_WRAP)
//            val js = "var style = document.createElement('style'); style.innerHTML = window.atob('$encoded'); " +
//                    "document.head.appendChild(style);"
            val css = "file:///android_asset/style.css"
            val js = "var link = document.createElement('link'); link.setAttribute('href','$css'); " +
                    "link.setAttribute('rel', 'stylesheet'); link.setAttribute('type','text/css'); " +
                    "document.head.appendChild(link);"
            view.evaluateJavascript(js,null)
        }
        catch (e: Exception) {
            e.printStackTrace()
        }
    }
}