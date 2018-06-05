package xyz.guotianqi.qtplayer.search

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import xyz.guotianqi.qtplayer.R
import xyz.guotianqi.qtplayer.databinding.SearchSongFragmentBinding

class SearchSongFragment : Fragment() {
    private lateinit var viewDataBinding: SearchSongFragmentBinding

    private lateinit var viewModel: SearchSongViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.search_song_fragment, container, false)
        viewDataBinding = SearchSongFragmentBinding.bind(root)

        return root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(SearchSongViewModel::class.java)
        viewDataBinding.viewModel = viewModel
    }


    companion object {
        fun newInstance() = SearchSongFragment()
    }
}
