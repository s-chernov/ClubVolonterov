package com.qfree.clubvolonterov

import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.AnimationDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.InputStream

import androidx.drawerlayout.widget.DrawerLayout
import android.widget.AdapterView.OnItemClickListener


class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private var uploadMessage: ValueCallback<Uri>? = null
    private var uploadMessageAboveL: ValueCallback<Array<Uri>>? = null
    private var mAnimationDrawable: AnimationDrawable? = null
    private var mScreenTitles: Array<String>? = null
    private var mDrawerLayout: DrawerLayout? = null
    private var mDrawerList: ListView? = null

    //Нижнее меню
    private val mOnNavigationItemSelectedListener =
        BottomNavigationView.OnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.bHome-> {
                    webView.loadUrl(getString(R.string.baseURL))
                    return@OnNavigationItemSelectedListener true
                }
                R.id.bActive -> {
                    webView.loadUrl(getString(R.string.baseURL) + "search.php?search_id=active_topics")
                    return@OnNavigationItemSelectedListener true
                }
                R.id.bMessages -> {
                    webView.loadUrl(getString(R.string.baseURL) + "ucp.php?i=pm&folder=inbox")
                    return@OnNavigationItemSelectedListener true
                }
            }
            false
        }


    //Боковые кнопки
    fun bUpClick(view: View) {
        scrollView(0, 0)
        visibilityUpDown(View.INVISIBLE, View.VISIBLE)
    }

    fun bDownClick(view: View) {
        scrollView(0, webView.bottom)
        visibilityUpDown(View.VISIBLE, View.INVISIBLE)
    }

    private fun scrollView(x: Int, y: Int) {
        val nestedScrollView: NestedScrollView = findViewById(R.id.nestedScrollView)
        nestedScrollView.scrollTo(x, y)

    }

    private fun visibilityUpDown(upView: Int, downView: Int) {
        val bUp: Button = findViewById(R.id.bUp)
        bUp.visibility = upView

        val bDown: Button = findViewById(R.id.bDown)
        bDown.visibility = downView
    }


    //Выбор файла
    companion object {
        private val FILE_CHOOSER_RESULT_CODE = 10000
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Меню
        mScreenTitles = resources.getStringArray(R.array.screen_array)
        mDrawerLayout = findViewById(R.id.drawer_layout)
        mDrawerList = findViewById(R.id.left_drawer)
        mDrawerList!!.adapter = ArrayAdapter<String>(this,
            R.layout.drawer_list_item, mScreenTitles as Array<out String>)
        mDrawerList!!.setOnItemClickListener { parent, view, position, id ->
            var url: String = getString(R.string.baseURL)
            when (position) {
                0 -> url += "app.php/calendar/"
                1 -> url += ""
                2 -> url += ""
                else -> {url = ""}
            }

            if (url != "") {
                mDrawerLayout!!.closeDrawer(mDrawerList!!)
                webView.loadUrl(url)
            }
        }


        val navigation = findViewById<View>(R.id.navigation) as BottomNavigationView
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)


        val imageView = findViewById<View>(R.id.spinner) as ImageView
        imageView.setBackgroundResource(R.drawable.spinner)

        mAnimationDrawable = imageView.background as AnimationDrawable
        mAnimationDrawable?.start()


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
        webView.loadUrl(getString(R.string.baseURL))
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

}

class SimpleWebViewClientImpl(activity: Activity?) : WebViewClient() {
    private var activity: Activity? = null

    init {
        this.activity = activity
    }

    override fun onLoadResource(view: WebView, url: String) {
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

        view.visibility = View.INVISIBLE
        changeSpinnerVisible(0)


        val nestedScrollView: NestedScrollView? = activity?.findViewById(R.id.nestedScrollView)
        nestedScrollView?.fullScroll(View.FOCUS_UP);
        nestedScrollView?.scrollTo(0,0);

        val bUp: Button? = activity?.findViewById(R.id.bUp)
        bUp?.visibility = View.INVISIBLE

        val bDown: Button? = activity?.findViewById(R.id.bDown)
        bDown?.visibility = View.VISIBLE
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)

        view.evaluateJavascript(" document.getElementById(\"add_files\").type = \"file\";", null)

        changeSpinnerVisible(4)
        view.visibility = View.VISIBLE
    }

    //Отображение анимации загрузки
    private fun changeSpinnerVisible(res: Int) {
        var spinner: ImageView = activity!!.findViewById(R.id.spinner)
        spinner.visibility = res
    }

    //Свой CSS
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

