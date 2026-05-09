package com.mabzplay.player

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream

class MainActivity : AppCompatActivity() {
    
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var adBlockerIndicator: android.widget.LinearLayout
    private lateinit var adBlockerText: TextView
    private lateinit var adCounter: TextView
    
    private var adsBlockedCount = 0
    
    // Comprehensive ad patterns based on uBlock Origin
    private val adPatterns = listOf(
        "doubleclick.net", "googleadservices.com", "googlesyndication.com",
        "google-analytics.com", "adservice.google.", "pagead2.googlesyndication.com",
        "partner.googleadservices.com", "tpc.googlesyndication.com",
        "adbrite.com", "exponential.com", "quantserve.com", "scorecardresearch.com",
        "zedo.com", "adsafeprotected.com", "teads.tv", "outbrain.com", 
        "taboola.com", "criteo.com", "pubmatic.com", "openx.com",
        "rubiconproject.com", "appnexus.com", "adnxs.com", "adroll.com",
        "adsrvr.org", "casalemedia.com", "contextweb.com", "indexww.com",
        "lijit.com", "mookie1.com", "rlcdn.com", "sharethrough.com",
        "smartadserver.com", "sovrn.com", "tidaltv.com", "tremorhub.com",
        "vidoomy.com", "vidazoo.com", "spotx.tv", "optimatic.com",
        "vidible.tv", "jwpltx.com", "jwpsrv.com", "imasdk.googleapis.com",
        "pubads.g.doubleclick.net", "g.doubleclick.net",
        "popads.net", "popcash.net", "propellerads.com", "adsterra.com",
        "popunderjs.com", "popunders.com", "popcdn.com",
        "facebook.com/tr", "connect.facebook.net", "amplitude.com",
        "mixpanel.com", "segment.com", "optimizely.com", "hotjar.com",
        "crazyegg.com", "fullstory.com",
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
        
        // Auto-hide ad blocker indicator after 3 seconds
        lifecycleScope.launch {
            delay(3000)
            adBlockerIndicator.animate().alpha(0.5f).duration = 1000
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
            }
            
            webViewClient = AdBlockingWebViewClient()
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    if (newProgress < 100) {
                        progressBar.progress = newProgress
                        progressBar.visibility = android.view.View.VISIBLE
                    } else {
                        progressBar.visibility = android.view.View.GONE
                    }
                }
            }
            
            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
        }
    }
    
    private fun setupAdBlocker() {
        adBlockerText.text = "uBlock Active"
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
        lifecycleScope.launch {
            delay(2000)
            adBlockerIndicator.animate().alpha(0.5f).duration = 1000
        }
    }
    
    inner class AdBlockingWebViewClient : WebViewClient() {
        
        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {
            val url = request?.url.toString().lowercase()
            
            // Check if this is an ad request
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
        
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            
            // Inject CSS to hide any remaining ad elements
            val cssCode = """
                javascript:(function() {
                    var style = document.createElement('style');
                    style.innerHTML = `
                        [class*="ad-"],[class*="_ad"],[id*="ad-"],[id*="_ad"],
                        [class*="banner"],[id*="banner"],[class*="popup"],[id*="popup"],
                        [class*="modal"],[class*="overlay"],[class*="sponsor"],
                        [class*="promo"],[class*="offer"],[data-ad],[data-ad-*],
                        iframe[src*="doubleclick"],iframe[src*="googlead"],
                        iframe[src*="googlesyndication"],iframe[src*="popup"],
                        .video-ads, .vjs-ad-iframe, .ad-container, .ad-wrapper,
                        div[style*="z-index: 999"] {
                            display: none !important;
                            visibility: hidden !important;
                            height: 0 !important;
                            overflow: hidden !important;
                        }
                    `;
                    document.head.appendChild(style);
                    
                    // Block popup windows
                    window.open = function() { return null; };
                    
                    // Remove any open intervals that might create popups
                    var highestTimeoutId = setTimeout(function(){}, 0);
                    for (var i = 0; i < highestTimeoutId; i++) {
                        clearTimeout(i);
                        clearInterval(i);
                    }
                })();
            """.trimIndent()
            
            view?.evaluateJavascript(cssCode, null)
        }
        
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            val url = request?.url.toString().lowercase()
            
            // Block known popup URLs
            val blocked = listOf("popup", "popunder", "doubleclick", "googlesyndication")
            for (pattern in blocked) {
                if (url.contains(pattern)) {
                    updateAdCounter()
                    return true
                }
            }
            
            view?.loadUrl(url)
            return true
        }
    }
}
