package com.osamaalek.kiosklauncher.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.osamaalek.kiosklauncher.R
import com.osamaalek.kiosklauncher.adapter.AppsAdapter
import com.osamaalek.kiosklauncher.util.AppsUtil
import com.osamaalek.kiosklauncher.util.KioskUtil

class HomeFragment : Fragment() {

    //private lateinit var fabApps: FloatingActionButton
    private lateinit var imageButtonExit: ImageButton
    private lateinit var imageButtonLock: ImageButton
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.fragment_home, container, false)

        //fabApps = v.findViewById(R.id.floatingActionButton)
        imageButtonExit = v.findViewById(R.id.imageButton_exit)
        imageButtonLock = v.findViewById(R.id.imageButton_lock)

        //fabApps.setOnClickListener {
        //    requireActivity().supportFragmentManager.beginTransaction()
        //        .replace(R.id.fragmentContainerView, AppsListFragment()).commit()
        //}

        imageButtonExit.setOnClickListener {
            KioskUtil.stopKioskMode(requireActivity())
        }

        imageButtonLock.setOnClickListener {
            KioskUtil.startKioskMode(requireActivity())
        }

        recyclerView = v.findViewById(R.id.recyclerView_apps)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 4, VERTICAL, false)
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = AppsAdapter(AppsUtil.getAllApps(requireContext()), requireContext())

        return v
    }

}