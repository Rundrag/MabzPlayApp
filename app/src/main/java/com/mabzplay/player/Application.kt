package com.mabzplay.player

import android.app.Application
import org.adblockplus.libadblockplus.LibraryLoader
import org.adblockplus.libadblockplus.android.AndroidHttpClient
import org.adblockplus.libadblockplus.android.SettingsStorage
import java.io.File

class Application : Application() {
    
    companion object {
        lateinit var instance: Application
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize AdBlock Plus library
        LibraryLoader.loadLibrary(this)
        
        // Initialize AdBlock settings storage
        val settingsStorage = SettingsStorage(File(filesDir, "adblock-settings.json"))
        val httpClient = AndroidHttpClient()
        
        // AdBlock will download filter lists automatically
    }
}