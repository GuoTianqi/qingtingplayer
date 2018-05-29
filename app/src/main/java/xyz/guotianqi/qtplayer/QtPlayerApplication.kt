package xyz.guotianqi.qtplayer

import android.app.Application

class QtPlayerApplication: Application() {
    override fun onCreate() {
        super.onCreate()

        INSTANCE = this
    }

    companion object {
        lateinit var INSTANCE: QtPlayerApplication
    }
}