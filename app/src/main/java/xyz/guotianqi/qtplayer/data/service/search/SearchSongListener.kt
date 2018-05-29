package xyz.guotianqi.qtplayer.data.service.search

import xyz.guotianqi.qtplayer.data.Song

interface SearchSongListener {
    /**
     * 搜索进度
     * @param searchingPath 正在搜索的目录
     * @param percent 搜索进度，如 0，2， 99，100等
     */
    fun onSearching(searchingPath: String, percent: Int)

    /**
     * 解析歌曲文件信息，包括解析歌曲名，歌手名，专辑名和查找对应的本地lrc
     * @param songPath 歌曲文件路径
     * @param percent 解析进度，如 0，2， 99，100等
     */
    fun onParsingSongInfo(songPath: String, percent: Int)

    /**
     * 搜索完成
     * @param songs
     */
    fun onComplete(songs: List<Song>)
}