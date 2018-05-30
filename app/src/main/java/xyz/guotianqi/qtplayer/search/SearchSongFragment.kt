package xyz.guotianqi.qtplayer.search

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import xyz.guotianqi.qtplayer.R

class SearchSongFragment : Fragment() {

    companion object {
        fun newInstance() = SearchSongFragment()
    }

    private lateinit var viewModel: SearchSongViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.search_song_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(SearchSongViewModel::class.java)
        // TODO: Use the ViewModel
    }

}
