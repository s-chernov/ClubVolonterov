package com.qfree.clubvolonterov

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ColorSpace
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
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
import android.widget.Toast

import android.widget.AdapterView
import com.bumptech.glide.load.resource.bitmap.TransformationUtils.circleCrop
import com.mikepenz.materialdrawer.holder.BadgeStyle
import com.mikepenz.materialdrawer.holder.ColorHolder
import com.mikepenz.materialdrawer.holder.ImageHolder
import com.mikepenz.materialdrawer.holder.StringHolder
import com.mikepenz.materialdrawer.model.*

import com.mikepenz.materialdrawer.model.interfaces.*
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader
import com.mikepenz.materialdrawer.util.DrawerImageLoader
import com.mikepenz.materialdrawer.util.getPlaceHolder
import com.mikepenz.materialdrawer.widget.AccountHeaderView
import com.mikepenz.materialdrawer.widget.MaterialDrawerSliderView
import kotlinx.coroutines.withContext


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

        // Инициализируем Toolbar
        val drawer: DrawerLayout = findViewById(R.id.drawer_layout)
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        var toggle = ActionBarDrawerToggle(
            this,
            drawer,
            toolbar,
            com.mikepenz.materialdrawer.R.string.material_drawer_open,
            com.mikepenz.materialdrawer.R.string.material_drawer_close
        )
        drawer.addDrawerListener(toggle)
        toggle.syncState()


        DrawerImageLoader.init(object : AbstractDrawerImageLoader() {
            override fun set(imageView: ImageView, uri: Uri, placeholder: Drawable, tag: String?) {
                Glide.with(imageView.context).load(uri).circleCrop().placeholder(placeholder).into(imageView)
            }

            override fun cancel(imageView: ImageView) {
                Glide.with(imageView.context).clear(imageView)
            }
        })


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

        var url = intent.data.toString()
        if (intent.data == null) {
            url = getString(R.string.baseURL) + "index.php";
        }

        webView.loadUrl(url)
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
            if (!json.isNullOrBlank() && json != "null") {
                var jArray = JSONArray(JSONTokener(json).nextValue().toString())
                val slider: MaterialDrawerSliderView = activity!!.findViewById(R.id.slider)

                slider.itemAdapter.clear()
                slider.itemAdapter.add(PrimaryDrawerItem().apply { nameText = "Главная";
                    onDrawerItemClickListener = { dview: View?, iDrawerItem: IDrawerItem<*>, i: Int ->
                        view.loadUrl("https://www.club-volonterov.ru/phpBB3/index.php")

                        false
                } })

                for (i in 0 until jArray.length()) {
                    val str = jArray.getJSONObject(i)
                    var strTxt = str.get("text").toString()
                    var strBadge = ""
                    var indx = i + 1

                    if (strTxt.contains("Личные сообщения")) {
                        strBadge = strTxt.substringAfter("Личные сообщения").trim()
                        strTxt = "Личные сообщения"
                        indx = 2
                    }

                    slider.itemAdapter.add(indx, PrimaryDrawerItem().apply { nameText = strTxt; badgeText = strBadge;
                        badgeStyle = BadgeStyle().apply {
                            if (strBadge.isNotEmpty() && strBadge != "0") {
                                textColor = ColorHolder.fromColor(Color.WHITE);
                                color = ColorHolder.fromColorRes(R.color.md_red_700)
                            }
                        };
                        onDrawerItemClickListener = { dview: View?, iDrawerItem: IDrawerItem<*>, i: Int ->
                            view.loadUrl(str.get("href").toString())

                            false
                        } })
                }
            }
        }
    }

    //Данные пользователя
    private fun getUser(view: WebView) {
        view.evaluateJavascript(
            "(function() {" +
                    "let sAvatar = document.getElementsByClassName('avatar');" +
                    "let sName = document.getElementsByClassName('username-coloured');" +
                    "return JSON.stringify({url:sAvatar[0].getAttribute('src')," +
                    "name:sName[0].textContent});})()"
        )
        { json: String ->
//            var userPhoto: ImageView = activity!!.findViewById(R.id.userPhoto)
//            var userName: TextView = activity!!.findViewById(R.id.userName)
            val headerView = AccountHeaderView(activity!!)
            headerView.attachToSliderView(activity!!.findViewById(R.id.slider))
            headerView.headerBackground = ImageHolder(R.drawable.header)
            headerView.selectionListEnabledForSingleProfile = false


            if (!json.isNullOrBlank() && json != "null") {
                    var jObject = JSONObject(JSONTokener(json).nextValue().toString())

                    headerView.apply {
                        addProfile(
                            ProfileDrawerItem().apply {
                                nameText = jObject.getString("name");
                                iconUrl =
                                    "https://www.club-volonterov.ru/phpBB3/" + jObject.getString("url").drop(1);
                            },
                            0
                        )
                    }
            } else {
                headerView.apply {
                    addProfile(
                        ProfileDrawerItem().apply {
                            nameText = "";
                            iconRes = R.drawable.ic_profile_placeholder
                        },
                        0
                    )
                }
            }
        }
    }
}
