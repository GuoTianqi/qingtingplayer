package xyz.guotianqi.qtplayer.data

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.*

@Dao
interface SongsDao {
    @Query("SELECT * FROM songs") fun getSongs(): List<Song>
    @Query("SELECT * FROM songs") fun getSongsAsync(): LiveData<List<Song>>

    @Query("SELECT * FROM songs WHERE id = :songId") fun getSongById(songId: String): Song?

    @Delete
    fun deleteSong(song: Song)
    @Delete fun deleteSongs(vararg songs: Song)

    @Insert(onConflict = OnConflictStrategy.REPLACE) fun insertSongs(vararg songs: Song)
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun insertSongs(songs: List<Song>)
}