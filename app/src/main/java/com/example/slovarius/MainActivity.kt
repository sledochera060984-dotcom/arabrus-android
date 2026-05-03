package com.example.slovarius

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import org.json.JSONObject
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var tts: TextToSpeech
    private var webView: WebView? = null
    
    private val TAG = "SlovariusAuth"
    private val PREFS_NAME = "SlovariusPrefs"
    private val KEY_USER_JSON = "user_json"

    // Launcher for Google Sign-In intent result
    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                handleSignInResult(account)
            } catch (e: ApiException) {
                Log.e(TAG, "Sign-in failed: ${e.statusCode}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize TTS
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale("ar")
                tts.setPitch(1.0f)
                tts.setSpeechRate(0.9f)
            }
        }

        // Google Sign-In Configuration
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestIdToken("814248156149-152n3k2l6qqfguih6li9ue7hrqigtqk6.apps.googleusercontent.com")
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            SlovariusWebView(
                onWebViewCreated = { webView = it },
                onLoginRequest = { signInLauncher.launch(googleSignInClient.signInIntent) }
            )
        }
    }

    private fun handleSignInResult(account: GoogleSignInAccount?) {
        account?.let {
            val userObj = JSONObject().apply {
                put("uid", it.id)
                put("email", it.email)
                put("displayName", it.displayName)
                put("photoUrl", it.photoUrl?.toString())
                put("idToken", it.idToken)
            }
            val userJson = userObj.toString()
            
            // 1. Persistent Session Management: Save to SharedPreferences
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putString(KEY_USER_JSON, userJson)
                .apply()

            webView?.post {
                webView?.evaluateJavascript("if(window.onAndroidLoginSuccess) window.onAndroidLoginSuccess($userJson);", null)
            }
        }
    }

    private fun performLogout() {
        // Clear SharedPreferences
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().remove(KEY_USER_JSON).apply()
        
        // Google Sign-Out
        googleSignInClient.signOut().addOnCompleteListener {
            webView?.post {
                webView?.evaluateJavascript("localStorage.clear(); location.reload();", null)
            }
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }

    // Bridge for JS
    inner class WebAppInterface(private val onLogin: () -> Unit) {
        @JavascriptInterface
        fun speak(text: String, lang: String = "ar") {
            runOnUiThread {
                tts.language = Locale(lang)
                tts.setPitch(1.0f)
                tts.setSpeechRate(0.9f)
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }

        @JavascriptInterface
        fun login() {
            runOnUiThread { onLogin() }
        }

        @JavascriptInterface
        fun logout() {
            runOnUiThread { performLogout() }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    fun SlovariusWebView(onWebViewCreated: (WebView) -> Unit, onLoginRequest: () -> Unit) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    onWebViewCreated(this)
                    
                    // Focus & Keyboard
                    requestFocus()
                    isFocusableInTouchMode = true
                    
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        userAgentString = userAgentString.replace("; wv", "")
                    }
                    
                    webChromeClient = WebChromeClient()
                    addJavascriptInterface(WebAppInterface(onLoginRequest), "AndroidApp")
                    
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val url = request?.url.toString()
                            
                            // External Intent Handling
                            if (url.startsWith("mailto:") || url.startsWith("tel:") || url.contains("t.me/")) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    ctx.startActivity(intent)
                                    return true
                                } catch (e: Exception) {
                                    Log.e(TAG, "External link failed: $url")
                                }
                            }
                            
                            if (url.startsWith("file:///")) return false
                            
                            // Other https links
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                ctx.startActivity(intent)
                                return true
                            } catch (e: Exception) {
                                return false
                            }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            
                            // 1. Restore Persistent Session
                            val prefs = ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                            val savedUserJson = prefs.getString(KEY_USER_JSON, null)
                            if (savedUserJson != null) {
                                view?.evaluateJavascript("""
                                    localStorage.setItem('user', '$savedUserJson');
                                    localStorage.setItem('arabrus_logged_in', '1');
                                    if (window.applyAuthState) {
                                        try {
                                            let u = JSON.parse('$savedUserJson');
                                            window.applyAuthState({
                                                uid: u.uid,
                                                email: u.email,
                                                displayName: u.displayName,
                                                photoURL: u.photoUrl
                                            });
                                        } catch(e) { window.applyAuthState(); }
                                    }
                                """.trimIndent(), null)
                            }

                            // 2. Advanced TTS Click Interceptor
                            view?.evaluateJavascript("""
                                document.addEventListener('click', function(e) {
                                    let s = e.target.closest('.btn-speak, [onclick*="play"], [onclick*="speak"]');
                                    if (s) {
                                        e.preventDefault(); e.stopPropagation();
                                        let w = s.getAttribute('onclick')?.match(/['"]([^'"]+)['"]/)?.[1];
                                        if (!w) {
                                            let container = s.closest('.item, .card-item, .reading-token, .item-details');
                                            if (container) {
                                                let ar = container.querySelector('.ar, .reading-ar, .card-front-ar, .card-front-text');
                                                w = ar ? ar.innerText : '';
                                            }
                                        }
                                        if (!w) w = s.innerText;
                                        if (w && window.AndroidApp) window.AndroidApp.speak(w.trim(), 'ar');
                                    }

                                    let l = e.target.closest('[onclick*="toggleAuth"], [onclick*="Google"]');
                                    if (l && window.AndroidApp) {
                                        e.preventDefault(); e.stopPropagation();
                                        window.AndroidApp.login();
                                    }
                                }, true);
                            """.trimIndent(), null)
                        }
                    }
                    loadUrl("file:///android_asset/index.html")
                }
            }
        )
    }
}
