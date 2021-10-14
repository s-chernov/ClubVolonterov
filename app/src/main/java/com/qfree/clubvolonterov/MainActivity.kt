package com.qfree.clubvolonterov

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.AnimationDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.internal.ContextUtils.getActivity
import java.io.InputStream


class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private var uploadMessage: ValueCallback<Uri>? = null
    private var uploadMessageAboveL: ValueCallback<Array<Uri>>? = null
    private var mAnimationDrawable: AnimationDrawable? = null

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

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val imageView = findViewById<View>(R.id.spinner) as ImageView
        imageView.setBackgroundResource(R.drawable.spinner)

        mAnimationDrawable = imageView.background as AnimationDrawable

        webView = findViewById(R.id.webView)
        webView.webViewClient = SimpleWebViewClientImpl(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.cacheMode = WebSettings.LOAD_DEFAULT
        webView.settings.domStorageEnabled = true
        webView.settings.allowContentAccess = true
        webView.webChromeClient = object : WebChromeClient() {

            // For Android < 3.0
            fun openFileChooser(valueCallback: ValueCallback<Uri>) {
                uploadMessage = valueCallback
                openImageChooserActivity()
            }

            // For Android  >= 3.0
            fun openFileChooser(valueCallback: ValueCallback<Uri>, acceptType: String) {
                uploadMessage = valueCallback
                openImageChooserActivity()
            }

            //For Android  >= 4.1
            fun openFileChooser(
                valueCallback: ValueCallback<Uri>,
                acceptType: String,
                capture: String
            ) {
                uploadMessage = valueCallback
                openImageChooserActivity()
            }

            // For Android >= 5.0
            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: WebChromeClient.FileChooserParams
            ): Boolean {
                uploadMessageAboveL = filePathCallback
                openImageChooserActivity()
                return true
            }
        }

        val navigation = findViewById<View>(R.id.navigation) as BottomNavigationView
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        mAnimationDrawable?.start()

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

    private fun openImageChooserActivity() {
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.addCategory(Intent.CATEGORY_OPENABLE)
        i.type = "image/*"
        startActivityForResult(Intent.createChooser(i, "Image Chooser"), FILE_CHOOSER_RESULT_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (null == uploadMessage && null == uploadMessageAboveL) return
            val result = if (data == null || resultCode != Activity.RESULT_OK) null else data.data
            if (uploadMessageAboveL != null) {
                onActivityResultAboveL(requestCode, resultCode, data)
            } else if (uploadMessage != null) {
                uploadMessage!!.onReceiveValue(result)
                uploadMessage = null
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun onActivityResultAboveL(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode != FILE_CHOOSER_RESULT_CODE || uploadMessageAboveL == null)
            return
        var results: Array<Uri>? = null
        if (resultCode == Activity.RESULT_OK) {
            if (intent != null) {
                val dataString = intent.dataString
                val clipData = intent.clipData
                if (clipData != null) {
                    results = Array(clipData.itemCount){
                            i -> clipData.getItemAt(i).uri
                    }
                }
                if (dataString != null)
                    results = arrayOf(Uri.parse(dataString))
            }
        }
        uploadMessageAboveL!!.onReceiveValue(results)
        uploadMessageAboveL = null
    }

    companion object {
        private val FILE_CHOOSER_RESULT_CODE = 10000
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
        changeSpinnerVisible(View.VISIBLE)

        val nestedScrollView: NestedScrollView? = activity?.findViewById(R.id.nestedScrollView)
        nestedScrollView?.fullScroll(View.FOCUS_UP);
        nestedScrollView?.scrollTo(0,0);
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)

        view.evaluateJavascript(" document.getElementById(\"add_files\").type = \"file\";", null)

        changeSpinnerVisible(View.INVISIBLE)
        view.visibility = View.VISIBLE
    }

    init {
        this.activity = activity
    }

    private fun changeSpinnerVisible(res: Int) {
        var spinner: ImageView = activity!!.findViewById(R.id.spinner)
        spinner.visibility = res
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
