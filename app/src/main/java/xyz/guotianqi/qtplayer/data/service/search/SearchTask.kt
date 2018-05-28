package xyz.guotianqi.qtplayer.data.service.search

import android.os.AsyncTask
import android.os.Environment
import xyz.guotianqi.qtplayer.BuildConfig
import com.sun.tools.corba.se.idl.Util.getAbsolutePath
import com.sun.org.apache.xerces.internal.util.DOMUtil.getParent
import android.text.TextUtils
import android.util.Log
import java.io.File
import java.nio.file.Files.isDirectory
import android.os.Environment.getExternalStorageDirectory
import android.text.method.TextKeyListener.clear
import xyz.guotianqi.qtplayer.data.Song


class SearchTask: AsyncTask<Any?, Any?, Any?>() {
    private val songFiles = mutableListOf<File>()

    override fun doInBackground(vararg params: Any?): Any? {

        return null
    }


    /**
     * 搜索本地MP3
     * @param scanningSongListener
     * @return
     */
    fun searchLocalSongs(): MutableList<Song> {
        songFiles.clear()

        var startTime = System.currentTimeMillis()

        val searchPaths = mutableListOf<File>()
        val searchMainRootDir = Environment.getExternalStorageDirectory()
        searchPaths.add(searchMainRootDir)
        var secondaryStorage: String? = System.getenv("SECONDARY_STORAGE")
        secondaryStorage?.let {
            val secondaryStoragePaths = it.split(":")
            for (path in secondaryStoragePaths) {
                if (path != searchMainRootDir.absolutePath) {
                    searchPaths.add(File(path))
                }
            }
        }

        for (searchPath in searchPaths) {
            if (DEBUG) {
                Log.v(TAG, "searchDir = " + searchPath.absolutePath)
            }

            walkDir(searchPath, searchPath)
        }

        if (DEBUG) {
            val endTime = System.currentTimeMillis()
            Log.v(TAG, "Find Mp3 Lrc Time = " + (endTime - startTime))
            startTime = endTime
        }

        mediaDatas = createMediaDatas(90.0f, 10.0f)
        sortMediaDataByDesc(mediaDatas)

        if (DEBUG) {
            Log.v(TAG, "Match Lrc to Mp3 Time = " + (System.currentTimeMillis() - startTime))
            if (mediaDatas != null) {
                Log.v(TAG, "Found MediaData Number = " + mediaDatas.size)
            }
        }

        return mediaDatas
    }

    fun parseCreateSongs(songFiles: List<File>) {

    }
    /**
     *
     * @param rootDir 搜索的根目录
     * @param searchDir 当前搜索的目录
     * @param basePercent 百分比基数
     * @param dirPercent 分配给这个文件夹的百分比
     */
    private fun walkDir(rootDir: File, searchDir: File) {
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
                if (file.absolutePath.split("/").size - rootDirDepth > SEARCH_DIR_DEPTH) {

                    if (DEBUG) {
                        Log.v(TAG, "DEPTH > " + SEARCH_DIR_DEPTH + ", " + searchDir.absolutePath)
                    }

                    continue
                }

                listFile.add(0, file)
            } else {
                listFile.add(listFile.size, file)
            }
        }

        var notFoundCount = 0
        for (file in listFile) {
            if (DEBUG) {
                Log.v(TAG, "search file: " + file.absolutePath)
            }

            if (file.isDirectory) {
                walkDir(rootDir, file)
            } else {
                if (matchFileExt(file)) {
                    songFiles.add(file)
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
        }
    }

    private fun matchFileExt(file: File?): Boolean {
        if (file == null) {
            return false
        }

        val fileName = file.name.toLowerCase()
        for (ext in SONG_EXTS) {
            if (fileName.endsWith(ext)) {
                return true
            }
        }

        return false
    }


    companion object {
        private val TAG = SearchTask::class.simpleName
        private val DEBUG = BuildConfig.DEBUG

        /**
         * 如果在一个文件夹下连续 NOT_FOUND_FILE_MAX_NUM 个文件不是歌曲或者歌词，则跳过此文件夹的搜索
         */
        private const val NOT_FOUND_FILE_MAX_NUM = 10
        // 搜索深度，相对于存储主目录，超过就不搜索
        private const val SEARCH_DIR_DEPTH = 4
        // 根据文件扩展来搜索歌曲
        private val SONG_EXTS = arrayOf(".mp3", ".wma", ".ape", "flac", ".tac")
    }
}