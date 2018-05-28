package xyz.guotianqi.qtplayer.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "songs")
data class Song @JvmOverloads constructor(
        @ColumnInfo(name = "song_name") var songName: String,
        @ColumnInfo(name = "singer_name") var singerName: String?,
        @ColumnInfo(name = "album") var album: String?,
        @ColumnInfo(name = "duration") var duration: Long,
        @ColumnInfo(name = "size") var size: Long,
        @ColumnInfo(name = "path") var path: String,
        @PrimaryKey @ColumnInfo(name = "id") var id: String = UUID.randomUUID().toString()) {

}