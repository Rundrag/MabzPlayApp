package com.mabzplay.player

import android.annotation.SuppressLint
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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayInputStream

class MainActivity : AppCompatActivity() {
    
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private var adsBlockedCount = 0
    
    // Only block known ad/popup domains - NOT video domains
    private val adDomains = setOf(
        "doubleclick.net", "googleadservices.com", "googlesyndication.com",
        "google-analytics.com", "adservice.google", "pagead2.googlesyndication.com",
        "popads.net", "popcash.net", "propellerads.com", "adsterra.com",
        "popunderjs.com", "popunders.com", "exoclick.com", "adf.ly",
        "linkbucks.com", "shorte.st", "bc.vc", "adfoc.us", "exe.io",
        "earnify.com", "linkvertise.com", "sub2unlock.com"
    )
    
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        
        setupWebView()
        loadWebsite()
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
                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            }
            
            webViewClient = SmartWebViewClient()
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    if (newProgress < 100) {
                        progressBar.progress = newProgress
                        progressBar.visibility = View.VISIBLE
                    } else {
                        progressBar.visibility = View.GONE
                    }
                }
            }
            
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }
    }
    
    private fun loadWebsite() {
        webView.loadUrl("https://mabz.vercel.app")
    }
    
    private fun isAdDomain(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return adDomains.any { lowerUrl.contains(it) }
    }
    
    inner class SmartWebViewClient : WebViewClient() {
        
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            progressBar.visibility = View.VISIBLE
        }
        
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            progressBar.visibility = View.GONE
            injectAdBlockingCSS()
        }
        
        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {
            val url = request?.url.toString().lowercase()
            
            // Only block known ad domains - allow everything else
            if (isAdDomain(url)) {
                adsBlockedCount++
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
            
            // Allow video and main site content
            if (url.contains("mabz.vercel.app") ||
                url.contains("vidsrc") ||
                url.contains(".m3u8") ||
                url.contains(".mp4") ||
                url.contains("tmdb.org") ||
                url.contains("image.tmdb.org")) {
                return false
            }
            
            // Block only known bad domains
            if (isAdDomain(url)) {
                return true
            }
            
            // For any other URL, load normally
            return false
        }
        
        private fun injectAdBlockingCSS() {
            // CSS-only ad hiding - doesn't break video playback
            val cssCode = """
                javascript:(function() {
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
                        }
                    `;
                    document.head.appendChild(style);
                    
                    // Block window.open but preserve video player functionality
                    var originalOpen = window.open;
                    window.open = function(url, name, specs) {
                        if (url && (url.includes('popup') || url.includes('ad'))) {
                            return null;
                        }
                        return originalOpen ? originalOpen.call(this, url, name, specs) : null;
                    };
                })();
            """.trimIndent()
            
            webView.evaluateJavascript(cssCode, null)
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
