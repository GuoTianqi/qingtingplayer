package xyz.guotianqi.qtplayer.search

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.databinding.ObservableBoolean
import android.databinding.ObservableField
import xyz.guotianqi.qtplayer.R
import xyz.guotianqi.qtplayer.data.Song
import xyz.guotianqi.qtplayer.ext.resources

class SearchSongViewModel(application: Application) : AndroidViewModel(application) {
    val searchEnable = ObservableBoolean()
    val searchProgress = ObservableField<String>()
    val searchStatus = ObservableField<String>()
    val searchPath = ObservableField<String>()
    val searching = ObservableBoolean()

    private val searchSongListener = object : SearchSongListener {
        override fun onSearching(searchingPath: String, percent: Int) {
            searchStatus.set(resources.getString(R.string.searching_song))
            searchPath.set(searchingPath)
            searchProgress.set("$percent%")
        }

        override fun onParsingSongInfo(songPath: String, percent: Int) {
            searchStatus.set(resources.getString(R.string.parsing_song))
            searchPath.set(songPath)
            searchProgress.set("$percent%")
        }

        override fun onComplete(songs: List<Song>) {
            searchProgress.set("搜索完成")
            searchEnable.set(true)
            searchStatus.set("")
            searchPath.set("")

            searching.set(false)
        }
    }

    init {
        searchEnable.set(true)
        searchProgress.set(resources.getString(R.string.start_searching))

        SearchTask.getInstance().addSearchSongListener(searchSongListener)
    }

    override fun onCleared() {
        SearchTask.getInstance().removeSearchSongListener(searchSongListener)

        super.onCleared()
    }

    fun startSearch() {
        if (!searchEnable.get()) {
            return
        }

        searchEnable.set(false)
        searching.set(true)
        SearchTask.getInstance().startTask()
    }
}
