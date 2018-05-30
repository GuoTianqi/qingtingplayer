package xyz.guotianqi.qtplayer.data.db

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
import xyz.guotianqi.qtplayer.data.Song
import xyz.guotianqi.qtplayer.data.SongsDao

@Database(entities = [Song::class], version = 1)
abstract class QtPlayerDb: RoomDatabase() {
    abstract fun songDao(): SongsDao

    companion object {
        @Volatile private var INSTANCE: QtPlayerDb? = null
        private val lock = Any()

        fun getInstance(context: Context): QtPlayerDb {
            if (INSTANCE == null) {
                synchronized(lock) {
                    if (INSTANCE == null) {
                        INSTANCE = Room.databaseBuilder(context.applicationContext,
                                QtPlayerDb::class.java, "QtPlayer.db")
                                .build()
                    }
                }
            }

            return INSTANCE!!
        }
    }
}