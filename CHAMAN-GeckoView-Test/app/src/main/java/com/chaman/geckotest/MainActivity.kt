package com.chaman.geckotest

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import org.json.JSONObject
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.WebExtension

class MainActivity : AppCompatActivity() {
    private lateinit var runtime: GeckoRuntime
    private lateinit var session: GeckoSession
    private lateinit var geckoView: GeckoView
    private lateinit var address: EditText
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12)
            setBackgroundColor(0xFF11151A.toInt())
        }

        val heading = TextView(this).apply {
            text = "CHAMAN · GeckoView Proof of Concept"
            textSize = 17f
            setTextColor(0xFFF4F4F4.toInt())
            setPadding(8)
        }
        root.addView(heading)

        address = EditText(this).apply {
            hint = "Enter website URL"
            singleLine = true
            setText("https://example.com")
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF9AA4B2.toInt())
            setPadding(12)
            imeOptions = EditorInfo.IME_ACTION_GO
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    loadAddress()
                    true
                } else false
            }
        }
        root.addView(address)

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val go = Button(this).apply {
            text = "Go"
            setOnClickListener { loadAddress() }
        }
        val back = Button(this).apply {
            text = "Back"
            setOnClickListener {
                session.goBack()
            }
        }
        val reload = Button(this).apply {
            text = "Reload"
            setOnClickListener { session.reload() }
        }
        controls.addView(go, LinearLayout.LayoutParams(0, -2, 1f))
        controls.addView(back, LinearLayout.LayoutParams(0, -2, 1f))
        controls.addView(reload, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(controls)

        status = TextView(this).apply {
            text = "Starting GeckoView…"
            textSize = 12f
            setTextColor(0xFFD7B56D.toInt())
            setPadding(8)
        }
        root.addView(status)

        geckoView = GeckoView(this)
        root.addView(
            geckoView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)

        runtime = GeckoRuntime.create(this)
        session = GeckoSession()
        session.open(runtime)
        geckoView.setSession(session)

        installTestExtension()
    }

    private fun installTestExtension() {
        status.text = "Installing built-in WebExtension…"
        runtime.webExtensionController
            .ensureBuiltIn(
                "resource://android/assets/chaman_test_extension/",
                EXTENSION_ID
            )
            .accept(
                { extension ->
                    extension.setMessageDelegate(object : WebExtension.MessageDelegate {
                        override fun onMessage(
                            nativeApp: String,
                            message: Any,
                            sender: WebExtension.MessageSender
                        ): GeckoResult<Any>? {
                            runOnUiThread {
                                if (message is JSONObject) {
                                    when (message.optString("type")) {
                                        "CHAMAN_COOKIE_COUNT" -> {
                                            val host = message.optString("host")
                                            val count = message.optInt("count")
                                            status.text = "Extension OK · Cookie API OK · $host · $count cookies"
                                            Toast.makeText(
                                                this@MainActivity,
                                                "Cookie API read $count cookies (values not shared)",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                        "CHAMAN_COOKIE_ERROR" -> {
                                            status.text = "Cookie API error: ${message.optString("error")}"
                                        }
                                    }
                                }
                            }
                            return null
                        }
                    }, NATIVE_APP_ID)

                    status.text = "GeckoView ready · WebExtension installed · Open a site and tap its test button"
                    session.loadUri("https://example.com")
                },
                { error ->
                    status.text = "Extension install FAILED: ${error.message ?: error.javaClass.simpleName}"
                }
            )
    }

    private fun loadAddress() {
        var value = address.text.toString().trim()
        if (value.isBlank()) return
        if (!value.contains("://")) {
            value = if (value.contains('.') && !value.contains(' ')) "https://$value"
            else "https://www.google.com/search?q=" + android.net.Uri.encode(value)
        }
        address.setText(value)
        session.loadUri(value)
    }


    override fun onDestroy() {
        if (::session.isInitialized) {
            session.close()
        }
        super.onDestroy()
    }

    companion object {
        private const val EXTENSION_ID = "chaman-cookie-bridge-test@chaman.app"
        private const val NATIVE_APP_ID = "chaman.browser"
    }
}
