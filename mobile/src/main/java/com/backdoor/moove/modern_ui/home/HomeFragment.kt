package com.backdoor.moove.modern_ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.backdoor.moove.R
import com.backdoor.moove.data.Reminder
import com.backdoor.moove.databinding.HomeFragmentBinding
import com.backdoor.moove.modern_ui.home.list.RemindersRecyclerAdapter
import com.backdoor.moove.utils.ActionsListener
import com.backdoor.moove.utils.ListActions
import com.backdoor.moove.utils.SuperUtil

class HomeFragment : Fragment() {

    private lateinit var viewModel: HomeViewModel
    private lateinit var binding: HomeFragmentBinding
    private val adapter = RemindersRecyclerAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = HomeFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initToolbar()

        binding.fab.setOnClickListener {
            findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToCreateReminderFragment(""))
        }
        initList()
    }

    private fun initToolbar() {
        binding.toolbar.inflateMenu(R.menu.fragment_home)
        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_places -> {
                    findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToPlacesFragment())
                }
                R.id.action_donate -> {
                    findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToDonateFragment())
                }
                R.id.action_directions -> {
                    findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToEventsMapFragment())
                }
                R.id.action_more -> {
                    SuperUtil.showMore(context!!)
                }
                R.id.action_settings -> {
                    findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToSettingsFragment())
                }
            }
            return@setOnMenuItemClickListener true
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initViewModel()
    }

    private fun initList() {
        adapter.actionsListener = object : ActionsListener<Reminder> {
            override fun onAction(view: View, position: Int, t: Reminder?, actions: ListActions) {

            }
        }
        binding.eventsList.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        binding.eventsList.adapter = adapter
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this).get(HomeViewModel::class.java)
    }
}
