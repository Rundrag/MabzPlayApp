package com.mabzplay.player

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.webkit.JavascriptInterface
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
import java.net.URL

class MainActivity : AppCompatActivity() {
    
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var adBlockerIndicator: android.widget.LinearLayout
    private lateinit var adBlockerText: TextView
    private lateinit var adCounter: TextView
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private var adsBlockedCount = 0
    private var isPopupLoading = false
    private var currentMovieUrl = ""
    
    // Comprehensive ad and popup patterns
    private val blockedDomains = setOf(
        "doubleclick.net", "googleadservices.com", "googlesyndication.com",
        "google-analytics.com", "adservice.google", "pagead2.googlesyndication.com",
        "popads.net", "popcash.net", "propellerads.com", "adsterra.com",
        "popunderjs.com", "popunders.com", "exoclick.com", "adf.ly",
        "linkbucks.com", "shorte.st", "bc.vc", "adfoc.us", "exe.io",
        "earnify.com", "linkvertise.com", "sub2unlock.com", "lootdest.com",
        "dailyuploads.net", "up-to-down.net", "vidoza.net", "streamtape.com",
        "mp4upload.com", "rapidvideo.com", "openload.co", "vidcloud9.com"
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
                // CRITICAL: Block new windows
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
            
            // Add JavaScript interface for popup detection
            addJavascriptInterface(PopupBlockerInterface(), "PopupBlocker")
            
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }
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
    
    private fun isAdUrl(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return blockedDomains.any { lowerUrl.contains(it) } ||
               lowerUrl.contains("popup") ||
               lowerUrl.contains("popunder") ||
               lowerUrl.matches(Regex(".*\\.(xyz|top|club|site|online|pw|space)/.*"))
    }
    
    inner class PopupBlockerInterface {
        @JavascriptInterface
        fun onPopupAttempt(url: String) {
            runOnUiThread {
                updateAdCounter()
                Toast.makeText(this@MainActivity, "Popup blocked", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    inner class PopupBlockingWebChromeClient : WebChromeClient() {
        override fun onCreateWindow(
            view: WebView?,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: android.os.Message?
        ): Boolean {
            // Block all popup window creation
            updateAdCounter()
            return true
        }
    }
    
    inner class PopupBlockingWebViewClient : WebViewClient() {
        
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            progressBar.visibility = View.VISIBLE
            
            // Track the main movie URL when on the main site
            if (url?.contains("mabz.vercel.app") == true) {
                currentMovieUrl = url
            }
        }
        
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            progressBar.visibility = View.GONE
            isPopupLoading = false
            
            // Always inject the ad blocking script
            injectPopupBlockingScript()
        }
        
        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {
            val url = request?.url.toString().lowercase()
            
            if (isAdUrl(url)) {
                updateAdCounter()
                return WebResourceResponse(
                    "text/plain",
                    "utf-8",
                    ByteArrayInputStream("".toByteArray())
                )
            }
            
            return super.shouldInterceptRequest(view, request)
        }
        
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            val url = request?.url.toString()
            
            // Allow main site and video content
            if (url.contains("mabz.vercel.app") ||
                url.contains("vidsrc") ||
                url.contains(".m3u8") ||
                url.contains(".mp4") ||
                url.contains("tmdb.org") ||
                url.contains("image.tmdb.org")) {
                return false
            }
            
            // Block popup/ad URLs
            if (isAdUrl(url)) {
                updateAdCounter()
                Toast.makeText(this@MainActivity, "Popup blocked", Toast.LENGTH_SHORT).show()
                return true
            }
            
            // For any other URL, load it but track it as a popup
            isPopupLoading = true
            view?.loadUrl(url)
            return true
        }
        
        override fun onReceivedError(
            view: WebView?,
            errorCode: Int,
            description: String?,
            failingUrl: String?
        ) {
            super.onReceivedError(view, errorCode, description, failingUrl)
            progressBar.visibility = View.GONE
            isPopupLoading = false
        }
        
        private fun injectPopupBlockingScript() {
            val jsCode = """
                (function() {
                    // Completely override window.open
                    var originalOpen = window.open;
                    window.open = function() {
                        PopupBlocker.onPopupAttempt(arguments[0] || 'unknown');
                        return null;
                    };
                    
                    // Override anchor tags
                    document.querySelectorAll('a').forEach(function(a) {
                        var originalClick = a.onclick;
                        a.onclick = function(e) {
                            if (a.href && (a.href.includes('popup') || a.href.includes('ad'))) {
                                e.preventDefault();
                                e.stopPropagation();
                                PopupBlocker.onPopupAttempt(a.href);
                                return false;
                            }
                            if (originalClick) return originalClick.call(this, e);
                        };
                        a.setAttribute('target', '_self');
                    });
                    
                    // Prevent popup from elements with onclick
                    var allElements = document.querySelectorAll('[onclick]');
                    allElements.forEach(function(el) {
                        var onclickAttr = el.getAttribute('onclick');
                        if (onclickAttr && (onclickAttr.includes('window.open') || 
                            onclickAttr.includes('open(') ||
                            onclickAttr.includes('popup'))) {
                            el.removeAttribute('onclick');
                        }
                    });
                    
                    // Block iframe popups
                    var observer = new MutationObserver(function(mutations) {
                        mutations.forEach(function(mutation) {
                            mutation.addedNodes.forEach(function(node) {
                                if (node.nodeName === 'IFRAME') {
                                    var src = node.src || '';
                                    if (src.includes('popup') || src.includes('ad') || src.includes('doubleclick')) {
                                        node.remove();
                                        PopupBlocker.onPopupAttempt(src);
                                    }
                                }
                            });
                        });
                    });
                    observer.observe(document.body, { childList: true, subtree: true });
                    
                    console.log('Popup blocker fully active');
                })();
            """.trimIndent()
            
            webView.evaluateJavascript(jsCode, null)
        }
    }
    
    override fun onBackPressed() {
        if (isPopupLoading) {
            // We're on a popup page - just close it and go back to movie
            isPopupLoading = false
            if (webView.canGoBack()) {
                webView.goBack()
            } else {
                webView.loadUrl("https://mabz.vercel.app")
            }
        } else if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
