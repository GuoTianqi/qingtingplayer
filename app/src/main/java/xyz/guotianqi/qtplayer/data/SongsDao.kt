package xyz.guotianqi.qtplayer.data

import androidx.room.Dao
import androidx.room.Query

@Dao
interface SongsDao {
    @Query("SELECT * FROM songs") fun getSongs(): List<Song>

    @Query("SELECT * FROM songs WHERE id = :songId") fun getSongById(songId: String): Song?
}