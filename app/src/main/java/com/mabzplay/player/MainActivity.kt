package com.mabzplay.player

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
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
    
    // Comprehensive ad patterns including popup triggers
    private val adPatterns = listOf(
        "doubleclick.net", "googleadservices.com", "googlesyndication.com",
        "google-analytics.com", "adservice.google", "pagead2.googlesyndication.com",
        "popads.net", "popcash.net", "propellerads.com", "adsterra.com",
        "popunderjs.com", "popunders.com", "popup", "popunder", "pop-up",
        "vidsrc.xyz/ads", "vidsrc.xyz/popup", "embed.su/ads", "2embed.cc/ads",
        "openload", "streamtape", "mp4upload", "rapidvideo", "vidoza",
        "adf.ly", "linkbucks.com", "shorte.st", "bc.vc", "adfoc.us",
        "exe.io", "earnify.com", "linkvertise.com", "sub2unlock.com",
        "lootdest.com", "dailyuploads.net", "up-to-down.net"
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
                // CRITICAL: Disable multiple windows to prevent popups
                setSupportMultipleWindows(false)
                javaScriptCanOpenWindowsAutomatically = false
                loadsImagesAutomatically = true
                blockNetworkImage = false
                blockNetworkLoads = false
                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            }
            
            webViewClient = PopupBlockingWebViewClient()
            webChromeClient = PopupBlockingWebChromeClient()
            
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }
    }
    
    private fun setupAdBlocker() {
        adBlockerText.text = "uBlock Origin"
        adCounter.text = "0"
    }
    
    private fun loadWebsite() {
        webView.loadUrl("https://mabz.vercel.app")
    }
    
    private fun updateAdCounter() {
        adsBlockedCount++
        adCounter.text = adsBlockedCount.toString()
        adBlockerIndicator.animate().alpha(1f).duration = 200
        mainHandler.postDelayed({
            adBlockerIndicator.animate().alpha(0.5f).duration = 1000
        }, 2000)
    }
    
    // This blocks JavaScript window.open() calls and new window creation
    inner class PopupBlockingWebChromeClient : WebChromeClient() {
        override fun onCreateWindow(
            view: WebView?,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: android.os.Message?
        ): Boolean {
            // CRITICAL: This prevents new windows/tabs from opening
            // Return true to block the popup, false would allow it
            updateAdCounter()
            Toast.makeText(this@MainActivity, "Popup blocked", Toast.LENGTH_SHORT).show()
            // If we need to handle the popup, we would create a new WebView
            // But since we want to block it completely, just return true
            return true
        }
    }
    
    inner class PopupBlockingWebViewClient : WebViewClient() {
        
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            progressBar.visibility = View.VISIBLE
        }
        
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            progressBar.visibility = View.GONE
            injectAdBlockingScript()
        }
        
        override fun onReceivedError(
            view: WebView?,
            errorCode: Int,
            description: String?,
            failingUrl: String?
        ) {
            super.onReceivedError(view, errorCode, description, failingUrl)
            progressBar.visibility = View.GONE
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
        
        // CRITICAL: This blocks new navigation attempts that try to load separate pages
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            val url = request?.url.toString().lowercase()
            
            // Allow the main site and video sources
            if (url.contains("mabz.vercel.app") ||
                url.contains("vidsrc") ||
                url.contains("m3u8") ||
                url.contains(".mp4") ||
                url.contains("tmdb.org")) {
                return false // Let WebView load it normally
            }
            
            // Block common popup/ad domains
            val blockedDomains = listOf(
                "popup", "popunder", "doubleclick", "googlead", "googlesyndication",
                "adservice", "adsterra", "popads", "propellerads", "exoclick", "adf.ly",
                "linkbucks", "shorte.st", "bc.vc", "adfoc.us", "exe.io", "earnify.com",
                "linkvertise", "sub2unlock", "lootdest", "dailyuploads", "vidoza",
                "streamtape", "mp4upload", "rapidvideo", "openload"
            )
            
            for (domain in blockedDomains) {
                if (url.contains(domain)) {
                    updateAdCounter()
                    Toast.makeText(this@MainActivity, "Popup URL blocked", Toast.LENGTH_SHORT).show()
                    return true // Block the URL
                }
            }
            
            // For any other external URL, load it but in the same WebView
            // (This prevents new tabs from opening)
            view?.loadUrl(url)
            return true
        }
        
        private fun injectAdBlockingScript() {
            // This JavaScript runs on every page to block popup triggers
            val jsCode = """
                javascript:(function() {
                    // Store original window.open
                    var originalWindowOpen = window.open;
                    
                    // Override window.open to do nothing
                    window.open = function() {
                        console.log('Popup blocked by Mabz Play');
                        return null;
                    };
                    
                    // Override window.location.replace for ad redirects
                    var originalLocationReplace = window.location.replace;
                    window.location.replace = function(url) {
                        if (url && (url.includes('popup') || url.includes('ad') || url.includes('doubleclick'))) {
                            console.log('Redirect blocked:', url);
                            return;
                        }
                        return originalLocationReplace.call(this, url);
                    };
                    
                    // Prevent popup from anchor tags with target="_blank"
                    document.querySelectorAll('a[target="_blank"]').forEach(function(el) {
                        el.setAttribute('target', '_self');
                    });
                    
                    // Remove event listeners that might open popups
                    var buttons = document.querySelectorAll('button, div, span, a');
                    buttons.forEach(function(el) {
                        if (el.getAttribute('onclick') && 
                            (el.getAttribute('onclick').includes('window.open') ||
                             el.getAttribute('onclick').includes('popup'))) {
                            el.removeAttribute('onclick');
                        }
                    });
                    
                    // Hide ad elements
                    var style = document.createElement('style');
                    style.innerHTML = `
                        [class*="ad-"],[class*="_ad"],[id*="ad-"],[id*="_ad"],
                        [class*="banner"],[id*="banner"],[class*="popup"],[id*="popup"],
                        [class*="modal"],[class*="overlay"],[class*="sponsor"],
                        iframe[src*="doubleclick"],iframe[src*="googlead"],
                        .video-ads, .vjs-ad-iframe, .ad-container {
                            display: none !important;
                            visibility: hidden !important;
                            height: 0 !important;
                            width: 0 !important;
                            overflow: hidden !important;
                        }
                    `;
                    document.head.appendChild(style);
                    
                    console.log('Mabz Play popup blocker active');
                })();
            """.trimIndent()
            
            webView.evaluateJavascript(jsCode, null)
        }
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Handle back button to navigate within WebView history
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
    
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
