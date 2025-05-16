package com.ssyrix.tarlakontrol

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class TarlaAdapter (activity: MainActivity) : FragmentStateAdapter(activity){

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ToprakSicaklikFragment()
            1 -> HavaSicaklikFragment()
            else -> HavaNemFragment()
        }
    }
}

