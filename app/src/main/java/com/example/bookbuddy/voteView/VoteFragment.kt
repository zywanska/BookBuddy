package com.example.bookbuddy.voteView

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.example.bookbuddy.R
import com.example.bookbuddy.databinding.FragmentVoteBinding


class VoteFragment : Fragment(R.layout.fragment_vote) {
    private var al: ArrayList<String>? = null
    private val arrayAdapter: ArrayAdapter<String>? = null
    private val i = 0
    private lateinit var bindingVote: FragmentVoteBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        bindingVote = FragmentVoteBinding.inflate(inflater, container, false);


        return bindingVote.root
    }



    public fun DisLikeBtn(view: View) {

    }

}