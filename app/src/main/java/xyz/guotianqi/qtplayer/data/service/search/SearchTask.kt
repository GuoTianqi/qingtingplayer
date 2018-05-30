package xyz.guotianqi.qtplayer.data.service.search

import android.media.MediaMetadataRetriever
import android.os.AsyncTask
import android.os.Environment
import android.support.annotation.IntDef
import android.support.annotation.MainThread
import android.text.TextUtils
import android.util.Log
import xyz.guotianqi.qtplayer.BuildConfig
import xyz.guotianqi.qtplayer.QtPlayerApplication
import xyz.guotianqi.qtplayer.data.Song
import xyz.guotianqi.qtplayer.data.db.QtPlayerDb
import xyz.guotianqi.qtplayer.ext.removeFileExt
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.concurrent.Executors

class SearchTask: AsyncTask<Any?, SearchTask.ProgressData, List<Song>>() {

    private val searchSongListeners = mutableListOf<SearchSongListener>()

    private val songFiles = mutableListOf<File>()

    // 除了文件名是数字的所有lrc
    private val allLrcs = mutableListOf<File>()
    // 文件名是数字的lrc
    private val digitLrcs = mutableListOf<File>()

    // 保存解析过的lrc，避免再次解析
    private val lrcIdTagsMap = hashMapOf<String, LrcIdTags>()

    fun addSearchSongListener(searchSongListener: SearchSongListener) {
        searchSongListeners.add(searchSongListener)
    }

    fun removeSearchSongListener(searchSongListener: SearchSongListener) {
        searchSongListeners.remove(searchSongListener)
    }

    @MainThread
    fun start() {
        if (status == Status.PENDING) {
            executeOnExecutor(Executors.newSingleThreadExecutor())
        }
    }

    override fun doInBackground(vararg params: Any?): List<Song> {
        val songList = searchLocalSongs()
        resetInstance()

        return songList
    }

    override fun onProgressUpdate(vararg progressDatas: ProgressData) {
        val progressData = progressDatas[0]
        when(progressData.state) {
            ProgressData.STATE_SEARCHING -> {
                for (searchSongListener in searchSongListeners) {
                    searchSongListener.onSearching(progressData.path, progressData.progress)
                }
            }

            ProgressData.STATE_PARSING -> {
                for (searchSongListener in searchSongListeners) {
                    searchSongListener.onParsingSongInfo(progressData.path, progressData.progress)
                }
            }
        }
    }

    override fun onPostExecute(songs: List<Song>) {
        for (searchSongListener in searchSongListeners) {
            searchSongListener.onComplete(songs)
        }
    }


    /**
     * 搜索本地MP3
     * @param scanningSongListener
     * @return
     */
    private fun searchLocalSongs(): MutableList<Song> {
        songFiles.clear()
        allLrcs.clear()
        digitLrcs.clear()

        var startTime = System.currentTimeMillis()

        val searchPaths = mutableListOf<File>()
        // 内置sdk卡路径
        val searchMainRootDir = Environment.getExternalStorageDirectory()
        searchPaths.add(searchMainRootDir)
        // 外置置sd卡路径
        var secondaryStorage: String? = System.getenv("SECONDARY_STORAGE")
        secondaryStorage?.let {
            val secondaryStoragePaths = it.split(":")
            for (path in secondaryStoragePaths) {
                if (path.isNotBlank() && path != searchMainRootDir.absolutePath) {
                    searchPaths.add(File(path))
                }
            }
        }

        // 每个最外层搜索路径占的百分比，平均分配
        val dirPercent = 100f / searchPaths.size
        for (i in searchPaths.indices) {
            val searchPath = searchPaths[i]

            if (DEBUG) {
                Log.v(TAG, "searchDir = " + searchPath.absolutePath)
            }

            // 搜索目录的开始的基础百分比
            val basePercent = dirPercent * i

            walkDir(searchPath, searchPath, basePercent, dirPercent)
        }

        if (DEBUG) {
            val endTime = System.currentTimeMillis()
            Log.v(TAG, "Find Mp3 Lrc Time = " + (endTime - startTime))
            startTime = endTime
        }

        val songList = parseAndCreateSongs(songFiles)

        if (DEBUG) {
            Log.v(TAG, "Match Lrc to Mp3 Time = " + (System.currentTimeMillis() - startTime))
            if (songList != null) {
                Log.v(TAG, "Found MediaData Number = " + songList.size)
            }
        }

        return songList
    }

    /**
     *
     * @param rootDir 搜索的根目录
     * @param searchDir 当前搜索的目录
     * @param basePercent 百分比基数
     * @param dirPercent 分配给这个文件夹的百分比
     */
    private fun walkDir(rootDir: File, searchDir: File, basePercent: Float, dirPercent: Float) {
        val tmpListFile = searchDir.listFiles() ?: return

        val listFile = mutableListOf<File>()

        val rootDirDepth = rootDir.absolutePath.split("/").size
        // 文件夹优先
        for (file in tmpListFile) {
            val searchPath = file.absolutePath
            if (file.isHidden ||
                    searchPath.contains("Android") ||
                    searchPath.contains("cache") ||
                    searchPath.contains("Cache")) {
                continue
            }

            if (file.isDirectory) {
                // 判断搜索深度
                if (file.absolutePath.split("/").size - rootDirDepth > SEARCH_DIR_MAX_DEPTH) {

                    if (DEBUG) {
                        Log.v(TAG, "DEPTH > " + SEARCH_DIR_MAX_DEPTH + ", " + searchDir.absolutePath)
                    }

                    continue
                }

                listFile.add(0, file)
            } else {
                listFile.add(listFile.size, file)
            }
        }

        val percentStep = dirPercent / listFile.size
        var basePercentTmp = basePercent

        var notFoundCount = 0
        for (file in listFile) {
            if (DEBUG) {
                Log.v(TAG, "search file: " + file.absolutePath)
            }

            if (file.isDirectory) {
                walkDir(rootDir, file, basePercentTmp, percentStep)
            } else {
                if (matchFileExt(file)) {
                    notFoundCount = 0
                } else {
                    notFoundCount++
                    if (notFoundCount > NOT_FOUND_FILE_MAX_NUM &&
                            // 最外层目录下的文件不算
                            file.parent != rootDir.absolutePath) {
                        if (DEBUG) {
                            Log.v(TAG, "search " + file.parent + " break")
                        }
                        return
                    }
                }
            }

            basePercentTmp += percentStep

            if (searchSongListeners.isNotEmpty()) {
                publishProgress(ProgressData(ProgressData.STATE_SEARCHING, file.parent, (basePercentTmp + 0.5f).toInt()))
            }
        }
    }

    private fun matchFileExt(file: File): Boolean {
        val fileName = file.name.toLowerCase()
        for (ext in SONG_EXTS) {
            if (fileName.endsWith(ext)) {
                songFiles.add(file)
                return true
            }
        }

        for (ext in LRC_EXTS) {
            if (fileName.endsWith(ext)) {
                if (fileName.matches("\\d+\\.lrc".toRegex())) {
                    digitLrcs.add(file)
                } else {
                    allLrcs.add(file)
                }

                return true
            }
        }

        return false
    }


    private fun parseAndCreateSongs(songFiles: List<File>): MutableList<Song> {
        lrcIdTagsMap.clear()
        val newSongList = mutableListOf<Song>()
        val mediaMetadataRetriever = MediaMetadataRetriever()
        val oldSongList = getOldSongList()

        val percentStep = 100f / songFiles.size
        var basePercent = 0f
        for (i in 0 until songFiles.size) {
            val songFile = songFiles[i]
            val songFilePath = songFile.absolutePath
            val song = Song(songFilePath)

            if (searchSongListeners.isNotEmpty()) {
                publishProgress(ProgressData(ProgressData.STATE_PARSING,
                        songFilePath, (basePercent + percentStep * i + 0.5f).toInt()))
            }

            // 对比旧数据，避免再次解析
            var needContinue = false
            for (oldSong in oldSongList) {
                if (songFilePath == oldSong.songPath &&
                        songFile.lastModified() == File(oldSong.songPath).lastModified())
                {
                    // 歌曲文件相同， 歌词文件也存在
                    if (!TextUtils.isEmpty(oldSong.lrcPath) &&
                        File(oldSong.lrcPath).exists()) {
                            newSongList.add(oldSong)
                            needContinue = true
                            break
                        }
                }
            }

            if (needContinue) {
                continue
            }

            mediaMetadataRetriever.setDataSource(songFile.absolutePath)

            song.songName =
                    mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: ""
            song.singerName =
                    mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: ""
            song.album =
                    mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: ""
            val albumArtist =
                    mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST) ?: ""
            val bitrate = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            song.bitrate = bitrate?.toLong() ?: 0
            val duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            song.duration = duration?.toLong() ?: 0
            song.size = songFile.length()

            if (TextUtils.isEmpty(song.singerName)) {
                song.singerName = albumArtist
            }

            // 进一步解析歌名，歌词，针对对某些音乐软件的不正规MP3（歌名，歌星乱码或者为空），如酷狗
            parseSongAndSingerName(songFile, song)

            if (DEBUG) {
                Log.v(TAG, "songInfo = $song")
            }

            // 过滤时长短的歌曲
            if (song.duration < SONG_DURATION_MIN) {
                continue
            }

            // 过滤歌曲文件太小的
            if (song.size < SONG_SIZE_MIN) {
                continue
            }

            // 查找 lrc

            // 特定app查找
            // 多米
            if (matchDuomiLrc(song)) {
                newSongList.add(song)
                continue
            }

            // 先判断是否精确匹配文件名，再匹配歌名，歌星(最小匹配)
            // 是否找到精确匹配文件名
            var exactMatch = false
            val smallMatchedLrcs = mutableListOf<File>()
            for (lrcFile in allLrcs) {
                val lrcName = lrcFile.getName()

                // 精确匹配文件名
                if (lrcFileNameExactMatch(lrcName, songFile.getName())) {
                    song.lrcPath = lrcFile.absolutePath
                    newSongList.add(song)
                    exactMatch = true
                    break
                }

                if (lrcFileNameContains(lrcName, song.songName) &&
                    lrcFileNameContains(lrcName, song.singerName)
                ) {
                    smallMatchedLrcs.add(lrcFile)
                }
            }

            if (exactMatch) {
                continue
            }

            // 如果通过文件名找不到匹配歌词，则通过解析lrc来匹配
            if (smallMatchedLrcs.size == 0) {
                if (DEBUG) {
                    Log.d(TAG, "解析文件名是数字的lrc文件")
                }

                val success = matchLrcFileByParseLrcIdTags(song)
                if (DEBUG) {
                    Log.d(TAG, "matchLrcFileByParseLrcIdTags success = $success")
                }

                newSongList.add(song)
                continue
            }

            // 进一步匹配专辑（最大匹配）
            val largeMatchedLrcs = ArrayList<File>()
            if (song.album != null) {
                for (lrcFile in smallMatchedLrcs) {
                    val lrcName = lrcFile.name
                    if (lrcFileNameContains(lrcName, song.album)) {
                        largeMatchedLrcs.add(lrcFile)
                    }
                }
            }

            val matchLrcs: List<File>
            if (largeMatchedLrcs.size != 0) {
                matchLrcs = largeMatchedLrcs
            } else {
                matchLrcs = smallMatchedLrcs
            }

            // 进一步选取： .lrc优先，如果没有，默认为第一个找到的歌词
            var matchLrcFile: File? = null
            for (lrcFile in matchLrcs) {
                if (lrcFile.name.toLowerCase().endsWith(".lrc")) {
                    matchLrcFile = lrcFile
                    break
                }
            }

            if (matchLrcFile == null) {
                matchLrcFile = matchLrcs[0]
            }

            song.lrcPath = matchLrcFile.absolutePath
            newSongList.add(song)
        }

        mediaMetadataRetriever.release()

        QtPlayerDb.getInstance(QtPlayerApplication.INSTANCE.applicationContext)
                .songDao().insertSongs(newSongList)

        return newSongList
    }

    private fun getOldSongList(): List<Song> {
        val songDao = QtPlayerDb.getInstance(QtPlayerApplication.INSTANCE.applicationContext)
                .songDao();
        val tmpSongList = songDao.getSongs()

        val existSongList = mutableListOf<Song>()
        for (song in tmpSongList) {
            if (File(song.songPath).exists()) {
                existSongList.add(song)
            } else {
                // 删除数据库记录
                songDao.deleteSong(song)
            }
        }

        return existSongList
    }

    private fun parseSongAndSingerName(songFile: File, song: Song) {
        // 酷狗很多歌曲mp3乱码或者不存在歌名，歌星，直接通过mp3文件名解析
        // NOTE: 一定程度对酷狗下载的mp3文件命名规则造成依赖，精确分解所有‘-’分隔的字段
        val songFilePath = songFile.absolutePath
        if (songFilePath.contains("kgmusic") || songFilePath.contains("DUOMI")) {
            val strs = songFile.name.removeFileExt().split("-")
            if (strs.size < 2) {
                return
            }
            song.singerName = strs[0].trim()
            song.songName = strs[1].trim()
        }

        // 最后如果还是空的，直接通过文件名解析，先以 ‘ - ’分解歌手和歌名，如果解析不到，在用‘-’分解
        // NOTE: 不是酷狗或者多米的会直接走这一步，如果是酷狗或者多米会重复上面的一遍解析'-'的过程
        if (TextUtils.isEmpty(song.songName) || TextUtils.isEmpty(song.singerName)) {
            val songFileName = songFile.name.removeFileExt()
            // NOTE：只分解成两个字段，后面的整个字符串直接当成歌名
            var strs = songFileName.split(" - ".toRegex(), 2).toTypedArray()
            if (strs.size < 2) {
                strs = songFileName.split("-".toRegex(), 2).toTypedArray()
                if (strs.size < 2) {
                    return
                }
            }

            song.singerName = strs[0].trim()
            song.songName = strs[1].trim()
        }

        // 最后妥协：文件名直接当成歌名
        if (TextUtils.isEmpty(song.songName)) {
            song.songName = songFile.name.removeFileExt()
        }
    }

    private fun lrcFileNameExactMatch(lrcFileName: String, songFileName: String): Boolean {
        val lrcFileNameNotExt = lrcFileName.removeFileExt().toLowerCase()
        val songFileNameNotExt = songFileName.removeFileExt().toLowerCase()

        return lrcFileNameNotExt == songFileNameNotExt
    }

    private fun lrcFileNameContains(lrcFileName: String, containStr: String?): Boolean {
        if (containStr == null) {
            return false
        }

        // 去掉后缀名
        val lrcFileNameNotExt = lrcFileName.removeFileExt()

        val nameSplits = lrcFileNameNotExt.split("-")

        if (DEBUG) {
            Log.v(TAG, "lrcFileName=$lrcFileNameNotExt, containStr=$containStr")
            for (str in nameSplits) {
                Log.v(TAG, "nameSplits = $str")
            }
        }

        for (str in nameSplits) {
            if (str.trim().toLowerCase() == containStr.toLowerCase()) {
                return true
            }
        }

        return false
    }

    // 多米lrc查找
    private fun matchDuomiLrc(song: Song): Boolean {
        if (!song.songPath.contains("DUOMI")) {
            return false
        }

        val songPath = File(song.songPath)
        val dmsFilePath = songPath.parent + "/." + songPath.name + ".dms"

        val dmsFile = File(dmsFilePath)
        if (dmsFile.exists()) {
            BufferedReader(FileReader(dmsFile)).use {
                var lrcFileName: String? = it.readLine()

                if (lrcFileName != null) {
                    // 去掉第一个奇怪的字符"^A"
                    lrcFileName = lrcFileName.substring(1)
                    val lrcFile =
                        File(songPath.parentFile.parent + "/lyric/" + lrcFileName + ".lrc")
                    if (lrcFile.exists()) {
                        song.lrcPath = lrcFile.absolutePath
                        if (DEBUG) {
                            Log.d(TAG, "多米歌词匹配: lrc path = " + lrcFile.absolutePath)
                        }

                        return true
                    }
                }
            }
        }

        return false
    }

    private fun matchLrcFileByParseLrcIdTags(song: Song): Boolean {
        var found = false

        for (lrcFile in digitLrcs) {
            val (ar, al, ti) = parseLrcIdTags(lrcFile) ?: continue

            if ((song.songName.toLowerCase() == ti.toLowerCase() ||
                    "《${song.songName}》".toLowerCase() == ti.toLowerCase()) && // lrc歌名有可能包含书名号
                    song.singerName.toLowerCase() == ar.toLowerCase()
            ) {
                found = true
                song.lrcPath = lrcFile.absolutePath

                // 如果匹配精确到专辑名，说明已经找到对应的歌词文件了
                // 如果没有匹配到，则继续查找
                if (song.album.toLowerCase() == al.toLowerCase()) {

                    return true
                }
            }
        }

        return found
    }

    private fun parseLrcIdTags(lrcFile: File): LrcIdTags? {
        var lrcIdTags = lrcIdTagsMap[lrcFile.absolutePath]
        if (lrcIdTags != null) {
            return lrcIdTags
        }

        BufferedReader(FileReader(lrcFile)).use {
            lrcIdTags = LrcIdTags()

            val sb = StringBuilder()
            // NOTE: 只读前6行会不会太少了
            for (i in 0..5) {
                val line = it.readLine()

                if (line == null) break else sb.append(line)
            }

            val lrcContent = sb.toString()
            lrcIdTags!!.ti = getLrcIdTagValue(lrcContent, "ti")
            lrcIdTags!!.al = getLrcIdTagValue(lrcContent, "al")
            lrcIdTags!!.ar = getLrcIdTagValue(lrcContent, "ar")

            if (DEBUG) {
                Log.v(TAG, "LrcIdTags = " + lrcIdTags!!)
            }

            lrcIdTagsMap.put(lrcFile.absolutePath, lrcIdTags!!)
        }

        return lrcIdTags
    }

    private fun getLrcIdTagValue(lrcContent: String, tag: String): String {
        val startStr = "[$tag:"
        val firstIndex = lrcContent.indexOf(startStr)
        if (firstIndex == -1) {
            return ""
        }
        val lastIndex = lrcContent.indexOf("]", firstIndex)
        if (lastIndex == -1) {
            return ""
        }
        return lrcContent.substring(firstIndex + startStr.length, lastIndex)
    }

    data class ProgressData(val state: Int, val path: String, val progress: Int) {
        companion object {
            const val STATE_SEARCHING = 0
            const val STATE_PARSING = 1

            @Retention(AnnotationRetention.SOURCE)
            @IntDef(value = [STATE_SEARCHING, STATE_PARSING])
            annotation class State
        }
    }


    companion object {
        private const val TAG = "SearchTask"
        private val DEBUG = BuildConfig.DEBUG

        /**
         * 如果在一个文件夹下连续 NOT_FOUND_FILE_MAX_NUM 个文件不是歌曲或者歌词，则跳过此文件夹的搜索
         */
        private const val NOT_FOUND_FILE_MAX_NUM = 10
        // 搜索深度，相对于存储主目录，超过就不搜索
        private const val SEARCH_DIR_MAX_DEPTH = 4
        // 根据文件扩展来搜索歌曲
        private val SONG_EXTS = arrayOf(".mp3", ".wma", ".ape", "flac", ".tac")

        private val LRC_EXTS = arrayOf(".lrc", ".krc", ".trc")

        private const val SONG_DURATION_MIN = 60 * 1000
        private const val SONG_SIZE_MIN = 100 * 1024

        @Volatile private var INSTANCE: SearchTask? = null

        @Synchronized
        fun getInstance(): SearchTask {
            if (INSTANCE == null) {
                INSTANCE = SearchTask()
            }

            return INSTANCE!!
        }

        @Synchronized
        private fun resetInstance() {
            INSTANCE = null
        }
    }
}