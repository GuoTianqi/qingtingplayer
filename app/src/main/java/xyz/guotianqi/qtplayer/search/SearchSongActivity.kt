package xyz.guotianqi.qtplayer.search

import android.os.Bundle
import xyz.guotianqi.qtplayer.R
import xyz.guotianqi.qtplayer.app.ToolBarActivity

class SearchSongActivity : ToolBarActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // setTitle(R.string.page_title_search_song)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, SearchSongFragment.newInstance())
                .commitNow()
        }
    }
}
