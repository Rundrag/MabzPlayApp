# Keep our custom classes
-keep class com.mabzplay.player.** { *; }

# Keep AdBlock engine classes
-keep class org.adblockplus.** { *; }
-keep class com.github.edsuns.adblock.** { *; }

# Keep WebView classes
-keepclassmembers class * extends android.webkit.WebViewClient {
    public *;
}