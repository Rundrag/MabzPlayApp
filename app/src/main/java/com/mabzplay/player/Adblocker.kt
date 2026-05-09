package com.mabzplay.player

import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

/**
 * Custom ad blocker with uBlock Origin filter patterns.
 * This serves as a fallback should the AdblockWebView have issues.
 */
class AdBlocker {
    
    companion object {
        // Comprehensive ad domain patterns based on uBlock Origin's filter list[citation:1][citation:10]
        private val adPatterns = listOf(
            // Major ad networks
            "doubleclick.net", "googleadservices.com", "googlesyndication.com",
            "google-analytics.com", "adservice.google", "pagead2.googlesyndication.com",
            "partner.googleadservices.com", "tpc.googlesyndication.com",
            
            // Ad servers
            "adbrite.com", "exponential.com", "quantserve.com", "scorecardresearch.com",
            "zedo.com", "adsafeprotected.com", "teads.tv", "outbrain.com", 
            "taboola.com", "criteo.com", "pubmatic.com", "openx.com",
            "rubiconproject.com", "appnexus.com", "adnxs.com", "adroll.com",
            "adsrvr.org", "casalemedia.com", "contextweb.com", "indexww.com",
            "lijit.com", "mookie1.com", "rlcdn.com", "rk231.com", "rlcdn.com",
            "sharethrough.com", "smartadserver.com", "sovrn.com", "tidaltv.com",
            "tremorhub.com", "undertone.com", "yldbt.com",
            
            // Video ad servers
            "vidoomy.com", "vidazoo.com", "spotx.tv", "optimatic.com",
            "vidible.tv", "jwpltx.com", "jwpsrv.com", "imasdk.googleapis.com",
            "pubads.g.doubleclick.net", "g.doubleclick.net",
            
            // Pop-up networks
            "popads.net", "popcash.net", "propellerads.com", "adsterra.com",
            "popunderjs.com", "popunders.com", "popcdn.com", "popmyads.com",
            
            // Tracking and analytics
            "facebook.com/tr", "connect.facebook.net", "amplitude.com",
            "mixpanel.com", "segment.com", "optimizely.com", "hotjar.com",
            "crazyegg.com", "fullstory.com", "loggly.com", "newrelic.com",
            "datadoghq.com", "sentry.io", "rollbar.com",
            
            // iFrame and popup blockers
            "yads.ya.ru", "yandex.ru/ads", "an.yandex.ru", "mc.yandex.ru",
            
            // VidSrc related
            "vidsrc.xyz/ads", "vidsrc.xyz/popup", "vidsrc.xyz/redirect",
            "embed.su/ads", "2embed.cc/ads", "vidsrc.net/ads",
            "vidsrc.pm/ads", "vsembed.ru/ads",
            
            // Common ad paths
            "/ads/", "/advertisement/", "/popup/", "/banners/", "/adserver/",
            "/doubleclick/", "/googlesyndication/"
        )
        
        fun isAdUrl(url: String): Boolean {
            val lowercaseUrl = url.lowercase()
            for (pattern in adPatterns) {
                if (lowercaseUrl.contains(pattern)) {
                    return true
                }
            }
            return false
        }
        
        fun createBlockedResponse(): WebResourceResponse {
            return WebResourceResponse(
                "text/plain",
                "utf-8",
                ByteArrayInputStream("".toByteArray())
            )
        }
    }
}