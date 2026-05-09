package com.mabzplay.player

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mabzplay.player.databinding.ActivityMainBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.adblockplus.libadblockplus.android.AdblockEngine
import org.adblockplus.libadblockplus.android.AdblockEngineProvider
import org.adblockplus.libadblockplus.android.SingleInstanceEngineProvider
import org.adblockplus.libadblockplus.android.webview.AdblockWebView

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var webView: AdblockWebView
    private lateinit var progressBar: ProgressBar
    private lateinit var adBlockerIndicator: android.widget.LinearLayout
    private lateinit var adBlockerText: TextView
    private lateinit var adCounter: TextView
    
    private var adsBlockedCount = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        webView = binding.webView
        progressBar = binding.progressBar
        adBlockerIndicator = binding.adBlockerIndicator
        adBlockerText = binding.adBlockerText
        adCounter = binding.adCounter
        
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
            }
            
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
        // Initialize Adblock Engine provider
        val engineProvider = SingleInstanceEngineProvider(
            application = application,
            enableIdleRefresh = true,
            enableElementHiding = true,
            enableUrlHiding = true
        )
        
        // Configure AdBlock WebView
        webView.configure(engineProvider)
        
        adBlockerText.text = "uBlock Active"
        adCounter.text = "0"
    }
    
    private fun loadWebsite() {
        webView.loadUrl("https://mabzplay.vercel.app")
    }
    
    override fun onDestroy() {
        webView.onDestroy()
        super.onDestroy()
    }
}