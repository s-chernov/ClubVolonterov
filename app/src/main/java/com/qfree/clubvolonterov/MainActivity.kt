package com.qfree.clubvolonterov

import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ColorSpace
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.widget.NestedScrollView
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.google.android.material.progressindicator.CircularProgressIndicator
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import org.json.JSONTokener
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener


class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private var uploadMessage: ValueCallback<Uri>? = null
    private var uploadMessageAboveL: ValueCallback<Array<Uri>>? = null

    //Скроллинг
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
        val bUp: ImageButton = findViewById(R.id.bUp)
        bUp.visibility = upView

        val bDown: ImageButton = findViewById(R.id.bDown)
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

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        val drawer: DrawerLayout = findViewById(R.id.drawer_layout)
        val toggle = ActionBarDrawerToggle(this, drawer, toolbar, 0, 0)
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        val swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipe_container)
        swipeRefreshLayout.setOnRefreshListener {
            webView.loadUrl(webView.url!!)
            swipeRefreshLayout.isRefreshing = false
        }

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
        webView.loadUrl(getString(R.string.baseURL) + "index.php")
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val drawer: DrawerLayout = findViewById(R.id.drawer_layout)

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
            return true
        } else if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
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
        changeSpinnerVisible(View.VISIBLE)

        val nestedScrollView: NestedScrollView = activity!!.findViewById(R.id.nestedScrollView)
        nestedScrollView.fullScroll(View.FOCUS_UP);
        nestedScrollView.scrollTo(0,0);

        val bUp: ImageButton = activity!!.findViewById(R.id.bUp)
        bUp.visibility = View.INVISIBLE

        val bDown: ImageButton = activity!!.findViewById(R.id.bDown)
        bDown.visibility = View.VISIBLE
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)

        view.evaluateJavascript(" document.getElementById(\"add_files\").type = \"file\";", null)

        changeSpinnerVisible(View.INVISIBLE)

        view.visibility = View.VISIBLE

        getMenuLinks(view)
        getUser(view)
    }

    //Отображение анимации загрузки
    private fun changeSpinnerVisible(res: Int) {
        val spinner: CircularProgressIndicator = activity!!.findViewById(R.id.spinner)
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

    //Ссылки для меню
    private fun getMenuLinks(view: WebView) {
        view.evaluateJavascript("(function() {var res = [];" +
                "document.getElementById('quick-links').remove();" +
                "Array.from(document.getElementById('nav-main').querySelectorAll('[role=\"menuitem\"]')).map(elem =>" +
                "{res.push({text:elem.text.trim(), href:elem.href});});" +
                "return JSON.stringify(res);})()")
        { json:String ->
            if (!json.isNullOrBlank() && !json.equals("null")) {
                var jArray = JSONArray(JSONTokener(json).nextValue().toString())
                val nav_menu: NavigationView = activity!!.findViewById(R.id.nav_view)
                nav_menu.menu.clear()

                nav_menu.menu.add("Главная").setOnMenuItemClickListener {
                    view.loadUrl("https://www.club-volonterov.ru/phpBB3/index.php")

                    val drawer = activity!!.findViewById<View>(R.id.drawer_layout) as DrawerLayout
                    drawer.closeDrawer(GravityCompat.START)

                    true
                }

                if (jArray.length() > 3) {
                    val str = jArray.getJSONObject(jArray.length() - 1)
                    nav_menu.menu.add(str.get("text").toString()).setOnMenuItemClickListener {
                        view.loadUrl(str.get("href").toString())

                        val drawer =
                            activity!!.findViewById<View>(R.id.drawer_layout) as DrawerLayout
                        drawer.closeDrawer(GravityCompat.START)

                        true
                    }

                    jArray.remove(jArray.length() - 1)
                }

                for (i in 0 until jArray.length()) {
                    val str = jArray.getJSONObject(i)
                    nav_menu.menu.add(str.get("text").toString()).setOnMenuItemClickListener {
                        view.loadUrl(str.get("href").toString())

                        val drawer =
                            activity!!.findViewById<View>(R.id.drawer_layout) as DrawerLayout
                        drawer.closeDrawer(GravityCompat.START)

                        true
                    }
                }
            }
        }
    }

    //Данные пользователя
    private fun getUser(view: WebView) {
        view.evaluateJavascript("(function() {" +
                "let sAvatar = document.getElementsByClassName('avatar');" +
                "let sName = document.getElementsByClassName('username-coloured');" +
                "return JSON.stringify({url:sAvatar[0].getAttribute('src')," +
                "name:sName[0].textContent});})()")
        { json:String ->
            var userPhoto: ImageView = activity!!.findViewById(R.id.userPhoto)
            var userName: TextView = activity!!.findViewById(R.id.userName)

            if (!json.isNullOrBlank() && json != "null") {
                if (userName.text == "") {
                    var jObject = JSONObject(JSONTokener(json).nextValue().toString())

                    userName.text = jObject.getString("name")

                    Glide
                        .with(activity!!)
                        .load(
                            "https://www.club-volonterov.ru/phpBB3/" + jObject.getString("url")
                                .drop(1)
                        )
                        .circleCrop()
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .into(userPhoto)
                }
            } else {
                userPhoto.setImageDrawable(null)
                userName.text = ""
            }
        }
    }

}
