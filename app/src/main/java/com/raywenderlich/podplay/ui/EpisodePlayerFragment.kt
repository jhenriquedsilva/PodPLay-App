package com.raywenderlich.podplay.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.raywenderlich.podplay.databinding.FragmentEpisodePlayerBinding

class EpisodePlayerFragment: Fragment() {

    // That's the minimum code required to display a fragment
    private lateinit var layout: FragmentEpisodePlayerBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // super.onCreateView(inflater, container, savedInstanceState)

        layout = FragmentEpisodePlayerBinding.inflate(inflater, container, false)
        return layout.root

    }

    companion object {
        fun newInstance(): EpisodePlayerFragment {
            return EpisodePlayerFragment()
        }
    }
}