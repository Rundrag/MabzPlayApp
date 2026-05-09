package com.mabzplay.player

import android.annotation.SuppressLint
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
import java.io.ByteArrayInputStream

class MainActivity : AppCompatActivity() {
    
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var adBlockerIndicator: android.widget.LinearLayout
    private lateinit var adBlockerText: TextView
    private lateinit var adCounter: TextView
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private var adsBlockedCount = 0
    
    // Comprehensive ad patterns - uBlock Origin level filtering
    private val adPatterns = listOf(
        // Google ad services
        "doubleclick.net", "googleadservices.com", "googlesyndication.com",
        "google-analytics.com", "adservice.google", "pagead2.googlesyndication.com",
        "partner.googleadservices.com", "tpc.googlesyndication.com", "googleads.g.doubleclick.net",
        "ad.doubleclick.net", "static.doubleclick.net", "googleadsserving.cn",
        "googleadservices.com/pagead/conversion", "google.com/pagead",
        
        // Major ad networks
        "adbrite.com", "exponential.com", "quantserve.com", "scorecardresearch.com",
        "zedo.com", "adsafeprotected.com", "teads.tv", "outbrain.com", 
        "taboola.com", "criteo.com", "pubmatic.com", "openx.com",
        "rubiconproject.com", "appnexus.com", "adnxs.com", "adroll.com",
        "adsrvr.org", "casalemedia.com", "contextweb.com", "indexww.com",
        "lijit.com", "mookie1.com", "rlcdn.com", "sharethrough.com",
        "smartadserver.com", "sovrn.com", "tidaltv.com", "tremorhub.com",
        "adap.tv", "adsymptotic.com", "adtech.com", "adform.net", "adfox.ru",
        "adkernel.com", "admanmedia.com", "adnetic.com", "adobe.com/activation",
        "adpredictive.com", "adroll.com", "adsafeprotected.com", "adserver.com",
        
        // Video ad servers
        "vidoomy.com", "vidazoo.com", "spotx.tv", "optimatic.com",
        "vidible.tv", "jwpltx.com", "jwpsrv.com", "imasdk.googleapis.com",
        "pubads.g.doubleclick.net", "g.doubleclick.net", "vpaid.example.com",
        "vpaid.video", "adserver.video", "video.adnxs.com", "vid.facecast.xyz",
        
        // Pop-up and pop-under networks
        "popads.net", "popcash.net", "propellerads.com", "adsterra.com",
        "popunderjs.com", "popunders.com", "popcdn.com", "popmyads.com",
        "popupjs.com", "popupad.net", "popunder.ru", "popup.tools",
        
        // Tracking and analytics
        "facebook.com/tr", "connect.facebook.net", "amplitude.com",
        "mixpanel.com", "segment.com", "optimizely.com", "hotjar.com",
        "crazyegg.com", "fullstory.com", "heap.io", "intercom.com",
        "logrocket.com", "sentry.io", "datadoghq.com", "newrelic.com",
        
        // Russian ad networks
        "yads.ya.ru", "yandex.ru/ads", "an.yandex.ru", "mc.yandex.ru",
        "adfox.yandex.ru", "bs.yandex.ru", "suggest.yandex.ru",
        
        // Chinese ad networks
        "doubleclick.cn", "googleads.cn", "baidu.com/ads", "alimama.com",
        "tanx.com", "cnzz.com", "umeng.com", "qq.com/ads",
        
        // VidSrc specific
        "vidsrc.xyz/ads", "vidsrc.xyz/popup", "vidsrc.xyz/redirect",
        "embed.su/ads", "2embed.cc/ads", "vidsrc.net/ads",
        "vidsrc.pm/ads", "vsembed.ru/ads", "vidsrc.to/ads",
        
        // Common ad path patterns
        "/ads/", "/advertisement/", "/popup/", "/banners/", "/adserver/",
        "/doubleclick/", "/googlesyndication/", "/advertise/", "/sponsored/",
        "/partner-ads/", "/ad-injection/", "/ad-placement/", "/ad-roll/",
        "/ad-serving/", "/ad-delivery/", "/ad-rotation/", "/ad-refresh/",
        
        // Ad file types
        ".ad.", "ad-", "-ad", "_ad_", ".banner.", "popup-", "-popup",
        
        // Additional major ad domains
        "adzerk.net", "adreactor.com", "adswizz.com", "adition.com",
        "adition.net", "adkernel.io", "adnxs.com", "admeta.net",
        "adform.net", "adformdsp.net", "adformx.com", "adformobile.com",
        "adformedia.com", "adformarketing.com", "adformreklam.com",
        "adformreklam.net", "adformreklam.org", "adformreklam.ru",
        
        // Anti-adblock bypasses
        "adblock-detector", "adblock-detection", "adblock-bypass",
        "anti-adblock", "blockadblock", "adblock-notice"
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
                userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
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
    
    inner class AdBlockingWebViewClient : WebViewClient() {
        
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
        
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            
            val cssCode = """
                javascript:(function() {
                    var style = document.createElement('style');
                    style.innerHTML = `
                        [class*="ad-" i],[class*="_ad" i],[id*="ad-" i],[id*="_ad" i],
                        [class*="banner" i],[id*="banner" i],[class*="popup" i],[id*="popup" i],
                        [class*="modal" i],[class*="overlay" i],[class*="sponsor" i],
                        [class*="promo" i],[class*="offer" i],[data-ad],[data-ad-*],
                        iframe[src*="doubleclick"],iframe[src*="googlead"],
                        iframe[src*="googlesyndication"],iframe[src*="popup"],
                        .video-ads, .vjs-ad-iframe, .ad-container, .ad-wrapper,
                        div[style*="z-index: 999"], [aria-label*="ad" i], [role="banner"],
                        .ytp-ad-module, .video-annotations, .ytp-cued-thumbnail-overlay {
                            display: none !important;
                            visibility: hidden !important;
                            height: 0 !important;
                            width: 0 !important;
                            overflow: hidden !important;
                            pointer-events: none !important;
                            opacity: 0 !important;
                        }
                    `;
                    document.head.appendChild(style);
                    
                    window.open = function() { return null; };
                    
                    var highestTimeoutId = setTimeout(function(){}, 0);
                    for (var i = 0; i < highestTimeoutId; i++) {
                        clearTimeout(i);
                        clearInterval(i);
                    }
                    
                    var originalFetch = window.fetch;
                    window.fetch = function(url) {
                        if (typeof url === 'string' && url.toLowerCase().match(/ads?|doubleclick|googlesyndication|popup|popunder/i)) {
                            return Promise.reject();
                        }
                        return originalFetch.apply(this, arguments);
                    };
                })();
            """.trimIndent()
            
            view?.evaluateJavascript(cssCode, null)
        }
        
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            val url = request?.url.toString().lowercase()
            
            val blocked = listOf("popup", "popunder", "doubleclick", "googlesyndication", "adsterra")
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
