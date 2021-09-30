package com.qfree.clubvolonterov

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.InputStream


class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    private val mOnNavigationItemSelectedListener =
        BottomNavigationView.OnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.bHome-> {
                    onbHomeClick()
                    return@OnNavigationItemSelectedListener true
                }
                R.id.bActive -> {
                    onbActiveClick()
                    return@OnNavigationItemSelectedListener true
                }
                R.id.bMessages -> {
                    onbMessagesClick()
                    return@OnNavigationItemSelectedListener true
                }
            }
            false
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        webView.webViewClient = SimpleWebViewClientImpl(this)
        webView.getSettings().setJavaScriptEnabled(true)
        webView.getSettings().cacheMode = WebSettings.LOAD_DEFAULT

        val navigation = findViewById<View>(R.id.navigation) as BottomNavigationView
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        onbHomeClick()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    fun onbHomeClick() {
        webView.loadUrl("https://www.club-volonterov.ru/phpBB3/index.php")
    }

    fun onbActiveClick() {
        webView.loadUrl("https://www.club-volonterov.ru/phpBB3/search.php?search_id=active_topics")
    }

    fun onbMessagesClick() {
        webView.loadUrl("https://www.club-volonterov.ru/phpBB3/ucp.php?i=pm&folder=inbox")
    }
}

class SimpleWebViewClientImpl(activity: Activity?) : WebViewClient() {
    private var activity: Activity? = null

    override fun onLoadResource(view: WebView, url: String) {
        super.onLoadResource(view, url)
        Log.e("res", url)

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

        view.visibility = View.INVISIBLE

        val nestedScrollView: NestedScrollView? = activity?.findViewById(R.id.nestedScrollView)
        nestedScrollView?.fullScroll(View.FOCUS_UP);
        nestedScrollView?.scrollTo(0,0);
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)

        view.visibility = View.VISIBLE
    }

    init {
        this.activity = activity
    }

    private fun injectCSS(view: WebView) {
        try {
            val inputStream: InputStream = activity!!.assets.open("style.css")
            val buffer = ByteArray(inputStream.available())
            inputStream.read(buffer)
            inputStream.close()
            val encoded: String = Base64.encodeToString(buffer, Base64.NO_WRAP)
            val js = "(function() {if (document.styleSheets.length > 0) { " +
                    "var style = document.createElement('style'); style.innerHTML = window.atob('$encoded'); " +
                    "document.head.appendChild(style); return true} else {return false}})();"
            view.evaluateJavascript(js, null)
        }
        catch (e: Exception) {
            e.printStackTrace()
        }
    }
}