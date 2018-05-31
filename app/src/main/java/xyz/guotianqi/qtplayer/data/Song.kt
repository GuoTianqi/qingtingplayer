package xyz.guotianqi.qtplayer.data

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import xyz.guotianqi.qtplayer.ext.md5

@Entity(tableName = "songs")
data class Song @JvmOverloads constructor(
    @ColumnInfo(name = "song_path") var songPath: String = "",
    @ColumnInfo(name = "song_name") var songName: String = "",
    @ColumnInfo(name = "singer_name") var singerName: String = "",
    @ColumnInfo(name = "album") var album: String = "",
    @ColumnInfo(name = "duration") var duration: Long = 0,
    @ColumnInfo(name = "size") var size: Long = 0,
    @ColumnInfo(name = "bitrate") var bitrate: Long = 0,
    @ColumnInfo(name = "lrc_path") var lrcPath: String = "",
    @PrimaryKey @ColumnInfo(name = "id") var id:String = songPath.md5()) {
}