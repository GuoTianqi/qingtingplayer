package xyz.guotianqi.qtplayer.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import xyz.guotianqi.qtplayer.data.Song
import xyz.guotianqi.qtplayer.data.SongsDao

@Database(entities = [Song::class], version = 1)
abstract class QtPlayerDb: RoomDatabase() {
    abstract fun songDao(): SongsDao

    companion object {
        @Volatile private var INSTANCE: QtPlayerDb? = null

        fun getInstance(context: Context): QtPlayerDb {
            if (INSTANCE == null) {
                synchronized(QtPlayerDb::class) {
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