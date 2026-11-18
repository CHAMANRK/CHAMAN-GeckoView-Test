package com.chaman.geckotest

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONArray
import org.json.JSONObject
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.WebExtension
import java.io.File

private data class Site(
    val id: String,
    val name: String,
    val hint: String,
    val startUrl: String,
    // Used only for cookie export. Navigation is NOT restricted by this list.
    val cookieDomains: List<String>,
    // Cookie NAMES that prove a login (only the name is checked, never the value).
    val loginCookies: List<String>
)

private val SITES = listOf(
    Site(
        id = "instagram",
        name = "Instagram",
        hint = "Login karo, phir Export dabao",
        startUrl = "https://www.instagram.com/accounts/login/",
        cookieDomains = listOf("instagram.com"),
        loginCookies = listOf("sessionid")
    ),
    Site(
        id = "youtube",
        name = "YouTube",
        hint = "Google login ke baad Export dabao",
        startUrl = "https://www.youtube.com/",
        cookieDomains = listOf("youtube.com", "google.com"),
        loginCookies = listOf("SAPISID", "__Secure-3PSID", "SID")
    )
)

class MainActivity : AppCompatActivity() {
    private lateinit var runtime: GeckoRuntime
    private lateinit var session: GeckoSession
    private lateinit var geckoView: GeckoView
    private lateinit var address: EditText
    private lateinit var homeView: LinearLayout
    private lateinit var browserView: LinearLayout
    private lateinit var homeStatus: TextView
    private lateinit var browserStatus: TextView

    private var currentSite: Site? = null
    private var pendingSite: Site? = null
    private var exportPort: WebExtension.Port? = null
    private var canGoBack = false
    private var fromDownloader = false

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Old exports never stay around between launches.
        File(cacheDir, "cookies").deleteRecursively()

        // Edge-to-edge (forced on targetSdk 35): pad root by the system bar insets so the
        // title/URL bar no longer sits under the status bar.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(COLOR_BG)
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            v.setPadding(
                bars.left + dp(12),
                bars.top + dp(8),
                bars.right + dp(12),
                bars.bottom + dp(8)
            )
            WindowInsetsCompat.CONSUMED
        }

        homeView = buildHome()
        browserView = buildBrowser()
        browserView.visibility = View.GONE
        root.addView(homeView, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(browserView, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (browserView.visibility == View.VISIBLE) {
                    if (canGoBack) session.goBack() else showHome()
                } else {
                    finish()
                }
            }
        })

        runtime = GeckoRuntime.create(this)
        session = GeckoSession()
        session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onLocationChange(
                session: GeckoSession,
                url: String?,
                perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>,
                hasUserGesture: Boolean
            ) {
                runOnUiThread { if (url != null) address.setText(url) }
            }

            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                this@MainActivity.canGoBack = canGoBack
            }

            // Login popups (target=_blank / window.open) open in the same view.
            override fun onNewSession(session: GeckoSession, uri: String): GeckoResult<GeckoSession>? {
                session.loadUri(uri)
                return null
            }
        }
        session.open(runtime)
        geckoView.setSession(session)

        installExtension()
        handleLaunch(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunch(intent)
    }

    // Handshake 1: Downloader opens the login page of one site directly (see Handshake.md).
    private fun handleLaunch(i: Intent?) {
        if (i?.action != ACTION_LOGIN) return
        val site = SITES.firstOrNull { it.id == i.getStringExtra("site") } ?: return
        fromDownloader = true
        openSite(site)
    }

    private fun buildHome(): LinearLayout {
        val home = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        home.addView(TextView(this).apply {
            text = "CHAMAN Cookie Browser"
            textSize = 22f
            setTextColor(COLOR_TEXT)
            setPadding(dp(4), dp(12), dp(4), dp(4))
        })
        home.addView(TextView(this).apply {
            text = "Site chuno, login karo, cookies export karo."
            textSize = 13f
            setTextColor(COLOR_MUTED)
            setPadding(dp(4), 0, dp(4), dp(20))
        })

        for (site in SITES) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(18), dp(16), dp(18), dp(16))
                background = GradientDrawable().apply {
                    setColor(COLOR_CARD)
                    cornerRadius = dp(16).toFloat()
                    setStroke(dp(1), COLOR_ACCENT)
                }
                isClickable = true
                isFocusable = true
                setOnClickListener { openSite(site) }
            }
            card.addView(TextView(this).apply {
                text = site.name
                textSize = 18f
                setTextColor(COLOR_TEXT)
            })
            card.addView(TextView(this).apply {
                text = site.hint
                textSize = 12f
                setTextColor(COLOR_MUTED)
                setPadding(0, dp(4), 0, 0)
            })
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.bottomMargin = dp(12)
            home.addView(card, lp)
        }

        // Pushes the status line to the bottom.
        home.addView(View(this), LinearLayout.LayoutParams(-1, 0, 1f))
        homeStatus = TextView(this).apply {
            text = "Starting GeckoView…"
            textSize = 12f
            setTextColor(COLOR_ACCENT)
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        home.addView(homeStatus)
        return home
    }

    private fun buildBrowser(): LinearLayout {
        val browser = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        // Read-only: shows where redirects went (so a fake login page is visible), no typing.
        address = EditText(this).apply {
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.END
            isFocusable = false
            isFocusableInTouchMode = false
            isCursorVisible = false
            keyListener = null
            setTextColor(COLOR_TEXT)
            textSize = 13f
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        browser.addView(address)

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        fun button(label: String, action: () -> Unit) = Button(this).apply {
            text = label
            setAllCaps(false)
            textSize = 13f
            setOnClickListener { action() }
        }
        controls.addView(button("Home") { showHome() }, LinearLayout.LayoutParams(0, -2, 1f))
        controls.addView(button("Back") { session.goBack() }, LinearLayout.LayoutParams(0, -2, 1f))
        controls.addView(button("Reload") { session.reload() }, LinearLayout.LayoutParams(0, -2, 1f))
        controls.addView(button("Export") { startExport() }, LinearLayout.LayoutParams(0, -2, 1f))
        browser.addView(controls)

        browserStatus = TextView(this).apply {
            textSize = 12f
            setTextColor(COLOR_ACCENT)
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        browser.addView(browserStatus)

        geckoView = GeckoView(this)
        browser.addView(geckoView, LinearLayout.LayoutParams(-1, 0, 1f))
        return browser
    }

    private fun setStatus(text: String) {
        runOnUiThread {
            homeStatus.text = text
            browserStatus.text = text
        }
    }

    private fun openSite(site: Site) {
        currentSite = site
        pendingSite = null
        homeView.visibility = View.GONE
        browserView.visibility = View.VISIBLE
        setStatus("${site.name}: login karke Export dabao")
        session.loadUri(site.startUrl)
    }

    private fun showHome() {
        pendingSite = null
        browserView.visibility = View.GONE
        homeView.visibility = View.VISIBLE
    }

    private fun installExtension() {
        setStatus("Installing built-in WebExtension…")
        runtime.webExtensionController
            .ensureBuiltIn(
                "resource://android/assets/chaman_test_extension/",
                EXTENSION_ID
            )
            .accept(
                { extension ->
                    if (extension == null) {
                        setStatus("Extension install FAILED: extension is null")
                        return@accept
                    }
                    extension.setMessageDelegate(object : WebExtension.MessageDelegate {
                        override fun onConnect(port: WebExtension.Port) {
                            port.setDelegate(object : WebExtension.PortDelegate {
                                override fun onPortMessage(message: Any, port: WebExtension.Port) {
                                    if (message !is JSONObject) return
                                    when (message.optString("type")) {
                                        "HELLO" -> {
                                            port.postMessage(JSONObject().put("type", "READY"))
                                            runOnUiThread {
                                                exportPort = port
                                                setStatus("Ready · extension connected · site chuno")
                                            }
                                        }
                                        "COOKIES_TXT" -> runOnUiThread { onCookiesText(message) }
                                        "COOKIES_ERROR" -> runOnUiThread {
                                            pendingSite = null
                                            setStatus("Export error: ${message.optString("error")}")
                                        }
                                    }
                                }

                                override fun onDisconnect(port: WebExtension.Port) {
                                    runOnUiThread {
                                        if (exportPort === port) exportPort = null
                                    }
                                }
                            })
                        }
                    }, NATIVE_APP_ID)
                    setStatus("Extension installed · connecting…")
                },
                { error ->
                    setStatus(
                        "Extension install FAILED: ${error?.message ?: error?.javaClass?.simpleName ?: "unknown"}"
                    )
                }
            )
    }

    // Tap 1: only this native button can start an export. Page JS has no way to trigger it.
    private fun startExport() {
        val site = currentSite ?: return
        val port = exportPort
        if (port == null) {
            setStatus("Extension abhi connect nahi hua, 2-3 sec baad phir dabao")
            return
        }
        pendingSite = site
        setStatus("${site.name}: cookies read ho rahi hain…")
        port.postMessage(
            JSONObject()
                .put("type", "EXPORT_COOKIES")
                .put("site", site.id)
                .put("domains", JSONArray(site.cookieDomains))
                .put("loginNames", JSONArray(site.loginCookies))
        )
    }

    private fun onCookiesText(message: JSONObject) {
        val site = pendingSite ?: return
        if (message.optString("site") != site.id) return
        pendingSite = null

        if (!message.optBoolean("loggedIn", true)) {
            setStatus("${site.name}: login nahi hua. Pehle login karo, phir Export")
            return
        }
        val text = message.optString("text")
        val (good, bad) = validateNetscape(text)
        if (bad > 0) {
            setStatus("${site.name}: cookies.txt INVALID ($bad bad lines), save nahi hui")
            return
        }
        if (good == 0) {
            setStatus("${site.name}: 0 cookies mili. Pehle login karo, phir Export")
            return
        }
        val bytes = text.toByteArray(Charsets.UTF_8)
        val dir = File(cacheDir, "cookies").apply { mkdirs() }
        val file = File(dir, "${site.id}.txt")
        file.writeBytes(bytes)
        // Only count and size are shown. Cookie names/values are never displayed or logged.
        setStatus("${site.name} · $good cookies · ${formatSize(bytes.size)} · Netscape format valid")
        sendToDownloader(site, file, good)
    }

    // Handshake 2: hand the file to CHAMAN Downloader (explicit intent + one-file read grant).
    // Downloader comes to the front, which is the "redirect back".
    private fun sendToDownloader(site: Site, file: File, count: Int) {
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.cookies", file)
            val send = Intent(ACTION_IMPORT)
                .setClassName(DOWNLOADER_PKG, DOWNLOADER_ACTIVITY)
                .setData(uri)
                .putExtra("site", site.id)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(send)
        } catch (e: ActivityNotFoundException) {
            setStatus("${site.name} · $count cookies ready, par CHAMAN Downloader install nahi hai")
        } catch (e: Exception) {
            setStatus("Downloader ko bhejna fail: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    // Returns (valid data lines, invalid lines). Header must be the Netscape marker.
    private fun validateNetscape(text: String): Pair<Int, Int> {
        val lines = text.split("\n").map { it.trimEnd('\r') }.filter { it.isNotBlank() }
        if (lines.isEmpty() || !lines[0].startsWith("# Netscape HTTP Cookie File")) {
            return Pair(0, 1)
        }
        var good = 0
        var bad = 0
        for (line in lines.drop(1)) {
            if (line.startsWith("#") && !line.startsWith("#HttpOnly_")) continue
            val f = line.split("\t")
            val ok = f.size == 7 &&
                f[0].isNotEmpty() &&
                (f[1] == "TRUE" || f[1] == "FALSE") &&
                f[2].startsWith("/") &&
                (f[3] == "TRUE" || f[3] == "FALSE") &&
                f[4].toLongOrNull() != null &&
                f[5].isNotEmpty()
            if (ok) good++ else bad++
        }
        return Pair(good, bad)
    }

    private fun formatSize(bytes: Int): String =
        if (bytes < 1024) "$bytes B" else String.format("%.1f KB", bytes / 1024.0)

    override fun onDestroy() {
        if (::session.isInitialized) {
            session.close()
        }
        super.onDestroy()
    }

    companion object {
        private const val EXTENSION_ID = "chaman-cookie-bridge-test@chaman.app"
        private const val NATIVE_APP_ID = "chaman.browser"
        // Handshake contract: see Handshake.md (keep identical in both repos).
        private const val ACTION_LOGIN = "com.chaman.browser.action.LOGIN"
        private const val ACTION_IMPORT = "com.chaman.downloader.action.IMPORT_COOKIES"
        private const val DOWNLOADER_PKG = "com.chaman.downloader"
        private const val DOWNLOADER_ACTIVITY = "com.chaman.downloader.MainActivity"
        private const val COLOR_BG = 0xFF11151A.toInt()
        private const val COLOR_CARD = 0xFF20242B.toInt()
        private const val COLOR_ACCENT = 0xFFD7B56D.toInt()
        private const val COLOR_TEXT = 0xFFF4F4F4.toInt()
        private const val COLOR_MUTED = 0xFF9AA4B2.toInt()
    }
}
