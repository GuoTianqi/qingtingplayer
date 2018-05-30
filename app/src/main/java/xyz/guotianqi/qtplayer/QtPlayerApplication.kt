package xyz.guotianqi.qtplayer

import android.app.Application
import com.facebook.stetho.Stetho

class QtPlayerApplication: Application() {
    override fun onCreate() {
        super.onCreate()

        INSTANCE = this

        Stetho.initializeWithDefaults(this)
    }

    companion object {
        lateinit var INSTANCE: QtPlayerApplication
    }
}