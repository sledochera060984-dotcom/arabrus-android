package com.example.slovarius

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
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

    private val tag = "SlovariusAuth"
    private val prefsName = "SlovariusPrefs"
    private val keyUserJson = "user_json"

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            Log.w(tag, "Google Sign-In cancelled or failed. resultCode=${result.resultCode}")
            showWebMessage("Вход отменён или не выполнен")
            return@registerForActivityResult
        }

        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            handleSignInResult(account)
        } catch (e: ApiException) {
            Log.e(tag, "Google Sign-In failed: ${e.statusCode}", e)
            showWebMessage("Ошибка входа Google: ${e.statusCode}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale("ar")
                tts.setPitch(1.0f)
                tts.setSpeechRate(0.9f)
            }
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestIdToken("814248156149-152n3k2l6qqfguih6li9ue7hrqigtqk6.apps.googleusercontent.com")
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            SlovariusWebView(
                onWebViewCreated = { webView = it },
                onLoginRequest = { signInLauncher.launch(googleSignInClient.signInIntent) },
            )
        }
    }
    override fun onResume() {
        super.onResume()

        GoogleSignIn.getLastSignedInAccount(this)?.let { account ->
            handleSignInResult(account)
        }

        webView?.postDelayed({ restoreSavedUser(webView) }, 300)
        webView?.postDelayed({ restoreSavedUser(webView) }, 1200)
        webView?.postDelayed({ restoreSavedUser(webView) }, 2500)
    }
    private fun handleSignInResult(account: GoogleSignInAccount?) {
        if (account == null) {
            showWebMessage("Google не вернул аккаунт")
            return
        }

        val userObj = JSONObject().apply {
            put("uid", account.id ?: account.email ?: "android-user")
            put("email", account.email ?: "")
            put("displayName", account.displayName ?: account.email ?: "Пользователь")
            put("photoUrl", account.photoUrl?.toString() ?: "")
            put("idToken", account.idToken ?: "")
            put("provider", "google")
        }
        val userJson = userObj.toString()

        getSharedPreferences(prefsName, MODE_PRIVATE).edit()
            .putString(keyUserJson, userJson)
            .apply()

        webView?.post { restoreUserIntoWebView(userJson, callSuccessCallback = true) }
    }

    private fun restoreSavedUser(view: WebView?) {
        val savedUserJson = getSharedPreferences(prefsName, MODE_PRIVATE).getString(keyUserJson, null)
        if (!savedUserJson.isNullOrBlank()) {
            view?.post { restoreUserIntoWebView(savedUserJson, callSuccessCallback = true) }
        }
    }

    private fun restoreUserIntoWebView(userJson: String, callSuccessCallback: Boolean) {
        val callbackLine = if (callSuccessCallback) {
            "if (window.onAndroidLoginSuccess) window.onAndroidLoginSuccess(user);"
        } else {
            ""
        }

        webView?.evaluateJavascript(
            """
            (function() {
                try {
                    const user = $userJson;
                    localStorage.setItem('arabrus_logged_in', '1');
                    localStorage.setItem('user', JSON.stringify(user));
                    localStorage.setItem('arabrus_user', JSON.stringify(user));
                    localStorage.setItem('arabrus_native_local_sync', '1');
                    localStorage.setItem('arabrus_access_open', '1');
                    document.documentElement.classList.add('prelogged-in-compact');
                    $callbackLine
                    if (window.applyAuthState) window.applyAuthState(user);
                    if (window.AndroidNativeBridge && window.AndroidNativeBridge.fixSyncUi) {
                        window.AndroidNativeBridge.fixSyncUi();
                    }
                    if (window.AndroidNativeBridge && window.AndroidNativeBridge.openLocalAccess) {
                        window.AndroidNativeBridge.openLocalAccess();
                    }
                    if (window.showMsg) window.showMsg('Вход выполнен: ' + (user.email || user.displayName || 'Google'));
                } catch (e) {
                    console.error('Android auth restore failed', e);
                    if (window.showMsg) window.showMsg('Ошибка восстановления входа');
                }
            })();
            """.trimIndent(),
            null,
        )
    }

    private fun performLogout() {
        getSharedPreferences(prefsName, MODE_PRIVATE).edit()
            .remove(keyUserJson)
            .apply()

        googleSignInClient.signOut().addOnCompleteListener {
            webView?.post {
                webView?.evaluateJavascript(
                    """
                    (function() {
                        try {
                            localStorage.removeItem('arabrus_logged_in');
                            localStorage.removeItem('arabrus_native_local_sync');
                            localStorage.removeItem('arabrus_access_open');
                            localStorage.removeItem('user');
                            localStorage.removeItem('arabrus_user');
                            document.documentElement.classList.remove('prelogged-in-compact');
                            if (window.applyAuthState) window.applyAuthState(null);
                            if (window.showMsg) window.showMsg('Вы вышли из аккаунта');
                            setTimeout(function() { location.reload(); }, 250);
                        } catch (e) {
                            console.error('Android logout failed', e);
                            location.reload();
                        }
                    })();
                    """.trimIndent(),
                    null,
                )
            }
        }
    }

    private fun showWebMessage(message: String) {
        val quotedMessage = JSONObject.quote(message)
        webView?.post {
            webView?.evaluateJavascript(
                "if(window.showMsg) window.showMsg($quotedMessage); else console.log($quotedMessage);",
                null,
            )
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }

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

        @JavascriptInterface
        fun getSavedUser(): String {
            return getSharedPreferences(prefsName, MODE_PRIVATE).getString(keyUserJson, "") ?: ""
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

                    requestFocus()
                    isFocusableInTouchMode = true

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        userAgentString = userAgentString.replace("; wv", "")
                    }

                    webChromeClient = WebChromeClient()
                    addJavascriptInterface(WebAppInterface(onLoginRequest), "AndroidApp")

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val url = request?.url.toString()

                            if (url.startsWith("mailto:") || url.startsWith("tel:") || url.contains("t.me/")) {
                                return try {
                                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    true
                                } catch (e: Exception) {
                                    Log.e(this@MainActivity.tag, "External link failed: $url", e)
                                    false
                                }
                            }

                            if (url.startsWith("file:///")) return false

                            return try {
                                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                true
                            } catch (e: Exception) {
                                false
                            }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            installNativeClickBridge(view)
                            restoreSavedUser(view)
                        }
                    }

                    loadUrl("file:///android_asset/index.html")
                }
            },
        )
    }

    private fun installNativeClickBridge(view: WebView?) {
        view?.evaluateJavascript(
            """
            (function() {
                if (window.__androidBridgeInstalled) return;
                window.__androidBridgeInstalled = true;

                window.AndroidNativeBridge = window.AndroidNativeBridge || {};

                window.AndroidNativeBridge.openLocalAccess = function() {
                    try {
                        localStorage.setItem('arabrus_access_open', '1');

                        ['canUseFeature', 'hasAccess', 'requireAccess', 'isAccessAllowed'].forEach(function(name) {
                            try { window[name] = function() { return true; }; } catch (_) {}
                        });

                        document.querySelectorAll('.lock-card').forEach(function(card) {
                            card.style.display = 'none';
                        });

                        document.querySelectorAll('.tab, .chip, button, .btn').forEach(function(el) {
                            if (el.textContent) el.textContent = el.textContent.replace(/🔒/g, '').trim();
                            el.classList.remove('locked');
                            el.removeAttribute('disabled');
                            el.style.pointerEvents = '';
                            el.style.opacity = '';
                        });

                        const trial = document.getElementById('trialIndicator');
                        if (trial) {
                            trial.className = 'status ok';
                            trial.textContent = '✅ Доступ открыт';
                            trial.style.display = '';
                        }
                    } catch (e) {
                        console.warn('openLocalAccess failed', e);
                    }
                };

                window.AndroidNativeBridge.fixSyncUi = function() {
                    try {
                        const sync = document.getElementById('syncIndicator');
                        if (sync) {
                            sync.className = 'status ok';
                            sync.textContent = '✅ Сохранено на устройстве';
                            sync.style.display = '';
                        }
                        document.querySelectorAll('.status, .toast, .message, div, span').forEach(function(el) {
                            const txt = (el.textContent || '').trim().toLowerCase();
                            if (txt.includes('ошибка синхронизации')) {
                                el.textContent = '✅ Сохранено на устройстве';
                                el.classList.remove('err');
                                el.classList.add('ok');
                            }
                        });
                    } catch (e) {
                        console.warn('fixSyncUi failed', e);
                    }
                };

                const makeLocalOk = async function() {
                    localStorage.setItem('arabrus_native_local_sync', '1');
                    if (window.AndroidNativeBridge && window.AndroidNativeBridge.fixSyncUi) {
                        window.AndroidNativeBridge.fixSyncUi();
                    }
                    if (window.AndroidNativeBridge && window.AndroidNativeBridge.openLocalAccess) {
                        window.AndroidNativeBridge.openLocalAccess();
                    }
                    return true;
                };

                window.nativeLocalSync = makeLocalOk;
                if (!window.__androidOriginalShowMsg && window.showMsg) {
                    window.__androidOriginalShowMsg = window.showMsg;
                    window.showMsg = function(message) {
                        const text = String(message || '');
                        const lower = text.toLowerCase();
                        if (lower.includes('ошибка синхронизации')) {
                            window.AndroidNativeBridge.fixSyncUi();
                            return window.__androidOriginalShowMsg('Сохранено на устройстве');
                        }
                        if ((lower.includes('проб') && lower.includes('законч')) || lower.includes('доступ закрыт')) {
                            window.AndroidNativeBridge.openLocalAccess();
                            return window.__androidOriginalShowMsg('Доступ открыт');
                        }
                        return window.__androidOriginalShowMsg(message);
                    };
                }

                document.addEventListener('click', function(e) {
                    const text = (e.target && e.target.innerText ? e.target.innerText : '').trim().toLowerCase();
                    const logoutButton = e.target.closest('[data-android-logout], [onclick*="logout"], [onclick*="Logout"], [onclick*="signOut"], [onclick*="SignOut"]');
                    if ((logoutButton || text === '🚪 выйти' || text === 'выйти' || text.includes('выйти')) && window.AndroidApp) {
                        e.preventDefault();
                        e.stopPropagation();
                        window.AndroidApp.logout();
                        return;
                    }

                    const speakButton = e.target.closest('.btn-speak, [onclick*="play"], [onclick*="speak"]');
                    if (speakButton && window.AndroidApp) {
                        e.preventDefault();
                        e.stopPropagation();
                        let word = speakButton.getAttribute('data-word') || '';
                        if (!word) {
                            const onclick = speakButton.getAttribute('onclick') || '';
                            const match = onclick.match(/['\"]([^'\"]+)['\"]/);
                            word = match ? match[1] : '';
                        }
                        if (!word) {
                            const container = speakButton.closest('.item, .card-item, .reading-token, .item-details');
                            const ar = container ? container.querySelector('.ar, .reading-ar, .card-front-ar, .card-front-text') : null;
                            word = ar ? ar.innerText : speakButton.innerText;
                        }
                        if (word) window.AndroidApp.speak(word.trim(), 'ar');
                        return;
                    }

                    const loginButton = e.target.closest('[data-android-login], [onclick*="toggleAuth"], [onclick*="Google"]');
                    if (loginButton && window.AndroidApp) {
                        e.preventDefault();
                        e.stopPropagation();
                        window.AndroidApp.login();
                    }
                }, true);

                let attempts = 0;
                const timer = setInterval(function() {
                    attempts += 1;
                    if (window.AndroidNativeBridge && window.AndroidNativeBridge.fixSyncUi) {
                        window.AndroidNativeBridge.fixSyncUi();
                    }
                    if (window.AndroidNativeBridge && window.AndroidNativeBridge.openLocalAccess) {
                        window.AndroidNativeBridge.openLocalAccess();
                    }
                    if (attempts > 20) clearInterval(timer);
                }, 500);
            })();
            """.trimIndent(),
            null,
        )
    }
}
