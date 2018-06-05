package xyz.guotianqi.qtplayer

import android.app.Application
import android.support.v7.app.AppCompatDelegate

class QtPlayerApplication: Application() {
    override fun onCreate() {
        super.onCreate()

        INSTANCE = this

        Stetho.initializeWithDefaults(this)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }

    companion object {
        lateinit var INSTANCE: QtPlayerApplication
    }
}