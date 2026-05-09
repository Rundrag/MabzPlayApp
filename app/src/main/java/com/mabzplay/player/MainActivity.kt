package com.mabzplay.player

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.github.edsuns.adblock.AdFilter
import com.github.edsuns.adblock.AdFilterViewModel
import com.github.edsuns.adblock.WebViewClientListener

class MainActivity : AppCompatActivity(), WebViewClientListener {
    
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var adBlockerIndicator: android.widget.LinearLayout
    private lateinit var adBlockerText: TextView
    private lateinit var adCounter: TextView
    private lateinit var filter: AdFilter
    private lateinit var filterViewModel: AdFilterViewModel
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private var adsBlockedCount = 0
    private var isLoading = true
    
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        adBlockerIndicator = findViewById(R.id.adBlockerIndicator)
        adBlockerText = findViewById(R.id.adBlockerText)
        adCounter = findViewById(R.id.adCounter)
        
        // Initialize the ad-blocking engine
        filter = AdFilter.get()
        filterViewModel = filter.viewModel
        
        setupWebView()
        setupAdBlocker()
        setupFilterObservers()
        loadWebsite()
        
        // Auto-hide ad blocker indicator after 3 seconds
        mainHandler.postDelayed({
            adBlockerIndicator.animate().alpha(0.5f).duration = 1000
        }, 3000)
        
        // Display which filters are active
        displayActiveFilters()
    }
    
    private fun displayActiveFilters() {
        val activeFilters = filterViewModel.subscriptions.value
        if (!activeFilters.isNullOrEmpty()) {
            val filterCount = activeFilters.size
            adBlockerText.text = "uBlock Active ($filterCount filters)"
        }
    }
    
    private fun setupFilterObservers() {
        // Track when filters are updated
        filterViewModel.onDirty.observe(this) {
            // Filters have been updated - clear cache
            webView.clearCache(true)
            if (!isLoading) {
                webView.reload()
            }
        }
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
                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }
            
            // Setup WebView with ad-blocking client
            filter.setupWebView(this)
            webViewClient = AdBlockingWebViewClient()
            
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    isLoading = newProgress < 100
                    if (newProgress < 100) {
                        progressBar.progress = newProgress
                        progressBar.visibility = android.view.View.VISIBLE
                    } else {
                        progressBar.visibility = android.view.View.GONE
                        mainHandler.postDelayed({
                            adBlockerIndicator.animate().alpha(0.5f).duration = 1000
                        }, 2000)
                    }
                }
            }
            
            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
        }
    }
    
    private fun setupAdBlocker() {
        adBlockerText.text = "uBlock Origin Active"
        adCounter.text = "0"
    }
    
    private fun loadWebsite() {
        webView.loadUrl("https://mabzplay.vercel.app")
    }
    
    private fun updateAdCounter() {
        adsBlockedCount++
        adCounter.text = adsBlockedCount.toString()
        
        // Make indicator visible
        adBlockerIndicator.animate().alpha(1f).duration = 200
        mainHandler.postDelayed({
            adBlockerIndicator.animate().alpha(0.5f).duration = 1000
        }, 2000)
    }
    
    // WebViewClientListener implementation for AdblockAndroid
    override fun onBlockedRequest(url: String) {
        updateAdCounter()
    }
    
    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
    }
    
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        
        // Inject additional CSS for element hiding (cosmetic filtering)
        val cosmeticCss = """
            javascript:(function() {
                // Additional cosmetic filtering for annoying elements
                var style = document.createElement('style');
                style.innerHTML = `
                    /* Hide common ad containers that might slip through */
                    [class*="ad" i], [class*="banner" i], [class*="popup" i],
                    [class*="modal" i], [class*="overlay" i], [class*="sponsor" i],
                    [id*="ad" i], [id*="banner" i], [id*="popup" i],
                    [id*="modal" i], [id*="overlay" i], [id*="sponsor" i],
                    .video-ads, .vjs-ad-iframe, .ad-container, .ad-wrapper,
                    div[style*="z-index: 999"], div[style*="position: fixed"][style*="z-index"],
                    iframe[src*="doubleclick"], iframe[src*="googlead"],
                    iframe[src*="googlesyndication"] {
                        display: none !important;
                        visibility: hidden !important;
                        height: 0 !important;
                        width: 0 !important;
                        overflow: hidden !important;
                        pointer-events: none !important;
                    }
                `;
                document.head.appendChild(style);
                
                // Block popup windows
                window.open = function() { return null; };
                
                // Remove any intervals that might create popups
                var highestTimeoutId = setTimeout(function(){}, 0);
                for (var i = 0; i < highestTimeoutId; i++) {
                    clearTimeout(i);
                    clearInterval(i);
                }
            })();
        """.trimIndent()
        
        view?.evaluateJavascript(cosmeticCss, null)
    }
    
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
    
    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
    
    inner class AdBlockingWebViewClient : WebViewClient() {
        
        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {
            // Let the AdblockAndroid engine handle the interception
            val result = filter.shouldIntercept(view!!, request!!)
            if (result != null && result.resourceResponse != null) {
                updateAdCounter()
            }
            return result?.resourceResponse
        }
    }
}
