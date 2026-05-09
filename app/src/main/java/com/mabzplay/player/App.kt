package com.mabzplay.player

import android.app.Application
import com.github.edsuns.adblock.AdFilter
import com.github.edsuns.adblock.AdFilterViewModel
import timber.log.Timber

class App : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber for debugging (optional)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        // Initialize the ad-blocking engine
        // This loads the C++ core and prepares the filter lists
        AdFilter.create(this)
        
        // Setup filter subscriptions (uBlock Origin's filter lists)
        setupFilterSubscriptions()
    }
    
    private fun setupFilterSubscriptions() {
        val filter = AdFilter.get()
        val filterViewModel = filter.viewModel
        
        // These are the SAME filter lists that uBlock Origin uses
        // They block ads, trackers, malware domains, and annoyances
        val subscriptions = mapOf(
            "AdGuard Base" to "https://filters.adtidy.org/extension/chromium/filters/2.txt",
            "EasyList" to "https://easylist.to/easylist/easylist.txt",
            "EasyPrivacy" to "https://easylist.to/easylist/easyprivacy.txt",
            "AdGuard Tracking Protection" to "https://filters.adtidy.org/extension/chromium/filters/3.txt",
            "AdGuard Annoyances" to "https://filters.adtidy.org/extension/chromium/filters/14.txt",
            "Peter Lowe's list" to "https://pgl.yoyo.org/adservers/serverlist.php?hostformat=hosts&showintro=0&mimetype=plaintext"
        )
        
        // Only add subscriptions on first installation
        if (!filter.hasInstallation) {
            for ((name, url) in subscriptions) {
                val subscription = filterViewModel.addFilter(name, url)
                filterViewModel.download(subscription.id)
            }
        }
        
        // Update the filters when they change
        filterViewModel.onDirty.observeForever {
            // Clear WebView cache when filters update
            // The WebView will be reloaded automatically
        }
    }
}
