package com.mabzplay.player

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayInputStream

class MainActivity : AppCompatActivity() {
    
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var adBlockerIndicator: android.widget.LinearLayout
    private lateinit var adBlockerText: TextView
    private lateinit var adCounter: TextView
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private var adsBlockedCount = 0
    private var loadError = false
    private var retryCount = 0
    
    private val adPatterns = listOf(
        "doubleclick.net", "googleadservices.com", "googlesyndication.com",
        "google-analytics.com", "adservice.google", "pagead2.googlesyndication.com",
        "popads.net", "popcash.net", "propellerads.com", "adsterra.com",
        "vidsrc.xyz/ads", "vidsrc.xyz/popup", "embed.su/ads", "2embed.cc/ads"
    )
    
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        adBlockerIndicator = findViewById(R.id.adBlockerIndicator)
        adBlockerText = findViewById(R.id.adBlockerText)
        adCounter = findViewById(R.id.adCounter)
        
        setupWebView()
        setupAdBlocker()
        
        // Show a message that loading has started
        Toast.makeText(this, "Loading Mabz Play...", Toast.LENGTH_SHORT).show()
        
        loadWebsite()
        
        mainHandler.postDelayed({
            adBlockerIndicator.animate().alpha(0.5f).duration = 1000
        }, 3000)
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                builtInZoomControls = true
                displayZoomControls = false
                setSupportZoom(true)
                mediaPlaybackRequiresUserGesture = false
                allowFileAccess = true
                allowContentAccess = true
                cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
                setSupportMultipleWindows(false)
                javaScriptCanOpenWindowsAutomatically = false
                loadsImagesAutomatically = true
                blockNetworkImage = false
                blockNetworkLoads = false
                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            }
            
            webViewClient = AdBlockingWebViewClient()
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    if (newProgress < 100) {
                        progressBar.progress = newProgress
                        progressBar.visibility = View.VISIBLE
                    } else {
                        progressBar.visibility = View.GONE
                    }
                }
                
                override fun onReceivedTitle(view: WebView?, title: String?) {
                    super.onReceivedTitle(view, title)
                    if (title?.isNotEmpty() == true && !title.startsWith("about:blank")) {
                        supportActionBar?.title = title
                    }
                }
            }
            
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }
    }
    
    private fun setupAdBlocker() {
        adBlockerText.text = "uBlock Origin"
        adCounter.text = "0"
    }
    
    private fun loadWebsite() {
        loadError = false
        // Try multiple URLs in case one is down
        val urls = listOf(
            "https://mabz.vercel.app",
            "https://mabzplay.netlify.app",
            "https://mabz.vercel.app/"
        )
        
        val urlToLoad = if (retryCount < urls.size) urls[retryCount] else urls[0]
        webView.loadUrl(urlToLoad)
    }
    
    private fun retryLoad() {
        retryCount++
        loadWebsite()
    }
    
    private fun updateAdCounter() {
        adsBlockedCount++
        adCounter.text = adsBlockedCount.toString()
        adBlockerIndicator.animate().alpha(1f).duration = 200
        mainHandler.postDelayed({
            adBlockerIndicator.animate().alpha(0.5f).duration = 1000
        }, 2000)
    }
    
    inner class AdBlockingWebViewClient : WebViewClient() {
        
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            progressBar.visibility = View.VISIBLE
            loadError = false
            // Log the URL being loaded
            android.util.Log.d("MabzPlay", "Loading URL: $url")
        }
        
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            progressBar.visibility = View.GONE
            
            if (!loadError && url != null && url.isNotEmpty()) {
                injectAdBlockingScript()
                // Show success message
                Toast.makeText(this@MainActivity, "Loaded: ${view?.title}", Toast.LENGTH_SHORT).show()
            } else if (loadError) {
                Toast.makeText(this@MainActivity, "Failed to load, retrying...", Toast.LENGTH_SHORT).show()
                mainHandler.postDelayed({
                    retryLoad()
                }, 2000)
            }
        }
        
        override fun onReceivedError(
            view: WebView?,
            errorCode: Int,
            description: String?,
            failingUrl: String?
        ) {
            super.onReceivedError(view, errorCode, description, failingUrl)
            loadError = true
            progressBar.visibility = View.GONE
            
            val errorMsg = "Error $errorCode: $description"
            android.util.Log.e("MabzPlay", errorMsg)
            Toast.makeText(this@MainActivity, errorMsg, Toast.LENGTH_LONG).show()
            
            // Show a visible error message in WebView
            val errorHtml = """
                <html>
                <body style="background:#0a0c10; color:#eef2ff; text-align:center; padding:40px;">
                    <h2 style="color:#39ff14;">Mabz Play</h2>
                    <p>Failed to load website.</p>
                    <p style="color:#e50914;">$description</p>
                    <p>Check your internet connection and try again.</p>
                    <button onclick="location.reload()" style="background:#39ff14; border:none; padding:10px 20px; border-radius:5px;">Retry</button>
                </body>
                </html>
            """.trimIndent()
            view?.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", null)
        }
        
        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {
            val url = request?.url.toString().lowercase()
            
            for (pattern in adPatterns) {
                if (url.contains(pattern)) {
                    updateAdCounter()
                    return WebResourceResponse(
                        "text/plain",
                        "utf-8",
                        ByteArrayInputStream("".toByteArray())
                    )
                }
            }
            
            return super.shouldInterceptRequest(view, request)
        }
        
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            val url = request?.url.toString()
            
            // Allow any URL that's not obviously an ad
            if (url.contains("popup") || url.contains("doubleclick") || url.contains("googlead")) {
                updateAdCounter()
                return true
            }
            
            view?.loadUrl(url)
            return true
        }
        
        private fun injectAdBlockingScript() {
            val jsCode = """
                javascript:(function() {
                    var style = document.createElement('style');
                    style.innerHTML = `
                        [class*="ad-"],[class*="_ad"],[id*="ad-"],[id*="_ad"],
                        [class*="banner"],[id*="banner"],[class*="popup"],[id*="popup"],
                        [class*="modal"],[class*="overlay"],[class*="sponsor"],
                        .video-ads, .vjs-ad-iframe, .ad-container,
                        iframe[src*="doubleclick"],iframe[src*="googlead"] {
                            display: none !important;
                            visibility: hidden !important;
                            height: 0 !important;
                        }
                    `;
                    document.head.appendChild(style);
                    window.open = function() { return null; };
                    console.log('Ad blocker injected');
                })();
            """.trimIndent()
            
            webView.evaluateJavascript(jsCode, null)
        }
    }
    
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
