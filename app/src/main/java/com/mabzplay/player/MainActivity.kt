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
    
    // Comprehensive ad patterns
    private val adPatterns = listOf(
        "doubleclick.net", "googleadservices.com", "googlesyndication.com",
        "google-analytics.com", "adservice.google", "pagead2.googlesyndication.com",
        "partner.googleadservices.com", "tpc.googlesyndication.com", "googleads.g.doubleclick.net",
        "ad.doubleclick.net", "static.doubleclick.net",
        "adbrite.com", "exponential.com", "quantserve.com", "scorecardresearch.com",
        "zedo.com", "adsafeprotected.com", "teads.tv", "outbrain.com", 
        "taboola.com", "criteo.com", "pubmatic.com", "openx.com",
        "rubiconproject.com", "appnexus.com", "adnxs.com", "adroll.com",
        "popads.net", "popcash.net", "propellerads.com", "adsterra.com",
        "popunderjs.com", "popunders.com",
        "vidsrc.xyz/ads", "vidsrc.xyz/popup", "vidsrc.xyz/redirect",
        "embed.su/ads", "2embed.cc/ads", "vidsrc.net/ads",
        "vidsrc.pm/ads", "vsembed.ru/ads"
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
                cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                setSupportMultipleWindows(false)
                javaScriptCanOpenWindowsAutomatically = false
                loadsImagesAutomatically = true
                blockNetworkImage = false
                blockNetworkLoads = false
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
        // Clear any previous errors
        loadError = false
        // Load the website
        webView.loadUrl("https://mabzplay.vercel.app")
        // Also try with www if the main URL fails (handled in WebViewClient)
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
        }
        
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            progressBar.visibility = View.GONE
            
            if (!loadError) {
                injectAdBlockingScript()
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
            
            // Show error message to user
            Toast.makeText(this@MainActivity, "Failed to load: $description", Toast.LENGTH_SHORT).show()
            
            // Try alternative URL if main fails
            if (failingUrl?.contains("mabzplay.vercel.app") == true) {
                mainHandler.postDelayed({
                    webView.loadUrl("https://mabzplay.netlify.app")
                }, 2000)
            }
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
            
            // Allow navigation within the website
            if (url.contains("mabzplay.vercel.app") || 
                url.contains("mabzplay.netlify.app") ||
                url.contains("vidsrc") ||
                url.contains("tmdb")) {
                view?.loadUrl(url)
                return true
            }
            
            // Block external URLs that might be ads
            if (url.contains("popup") || url.contains("doubleclick") || url.contains("googlead")) {
                updateAdCounter()
                return true
            }
            
            // For everything else, load normally
            view?.loadUrl(url)
            return true
        }
        
        private fun injectAdBlockingScript() {
            val jsCode = """
                javascript:(function() {
                    // Hide ad elements
                    var style = document.createElement('style');
                    style.innerHTML = `
                        [class*="ad-" i],[class*="_ad" i],[id*="ad-" i],[id*="_ad" i],
                        [class*="banner" i],[id*="banner" i],[class*="popup" i],[id*="popup" i],
                        [class*="modal" i],[class*="overlay" i],[class*="sponsor" i],
                        [class*="promo" i],[class*="offer" i],[data-ad],
                        iframe[src*="doubleclick"],iframe[src*="googlead"],
                        .video-ads, .vjs-ad-iframe, .ad-container,
                        div[style*="z-index: 999"] {
                            display: none !important;
                            visibility: hidden !important;
                            height: 0 !important;
                            overflow: hidden !important;
                        }
                    `;
                    document.head.appendChild(style);
                    
                    // Block popups
                    window.open = function() { return null; };
                    
                    // Clear intervals
                    var highestTimeoutId = setTimeout(function(){}, 0);
                    for (var i = 0; i < highestTimeoutId; i++) {
                        clearTimeout(i);
                        clearInterval(i);
                    }
                    
                    console.log('Ad blocker injected successfully');
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
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }
    
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        webView.restoreState(savedInstanceState)
    }
}
