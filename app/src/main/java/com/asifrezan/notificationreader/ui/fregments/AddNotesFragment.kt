package com.asifrezan.notificationreader.ui.fregments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.asifrezan.notificationreader.R
import com.asifrezan.notificationreader.databinding.FragmentAddNotesBinding
import com.asifrezan.notificationreader.databinding.FragmentNotesBinding

class AddNotesFragment : Fragment() {
    lateinit var binding: FragmentAddNotesBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddNotesBinding.inflate(inflater, container, false)


        return binding.root
    }


}