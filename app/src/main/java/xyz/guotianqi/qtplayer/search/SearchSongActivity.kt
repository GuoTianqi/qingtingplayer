package xyz.guotianqi.qtplayer.search

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import xyz.guotianqi.qtplayer.R
import xyz.guotianqi.qtplayer.app.ToolBarActivity

class SearchSongActivity : ToolBarActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, SearchSongFragment.newInstance())
                .commitNow()
        }
    }
}
