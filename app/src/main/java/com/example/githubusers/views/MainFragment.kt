package com.example.githubusers.views

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SearchView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.githubusers.MainActivity
import com.example.githubusers.R
import com.example.githubusers.interfaces.CanLoadData
import com.example.githubusers.recyclerList.MainRecyclerListAdapter
import com.example.githubusers.widgetElements.SearchViewQueryTextListener
import kotlinx.coroutines.launch

class MainFragment: Fragment(), CanLoadData {

    private lateinit var recyclerAdapter: MainRecyclerListAdapter
    private lateinit var recyclerView: RecyclerView

    override var failedLoad: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    private val handler = Handler(Looper.getMainLooper())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.clearFocus()
        // Set to variable recyclerView
        recyclerView = view.findViewById(R.id.userList)
        // For convenience, set main activity to mainActivity
        val mainActivity = (activity as MainActivity)
        // Assign the user list adapter to the recyclerAdapter
        recyclerAdapter = MainRecyclerListAdapter(
            mainActivity.viewModel
        )
        // Set layoutManager, an adapter for recyclerView
        recyclerView.run {
            layoutManager = LinearLayoutManager(context)
            adapter = recyclerAdapter
        }
        // Network lost icon
        val connectionLostIcon = view.findViewById<ImageView>(R.id.connectionLostIcon)
        lifecycleScope.launch {
            // Set color of internet connection lost icon to match the theme.
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainActivity.viewModel.isDarkTheme.collect {
                    if(it) {
                        connectionLostIcon.setImageResource(R.drawable.connection_lost_white)
                    }
                }
            }
        }
        lifecycleScope.launch {
            // Set visibility of internet connection lost icon depend on networkLost.
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainActivity.viewModel.networkLost.collect {
                    if(it) {
                        connectionLostIcon.visibility = View.VISIBLE
                    }
                    else {
                        connectionLostIcon.visibility = View.GONE
                    }
                }
            }
        }
        // Try to load data into users list.
        loadData()
        // Set on query text listener for SearchView.
        view.findViewById<SearchView>(R.id.editTextText).setOnQueryTextListener(
            SearchViewQueryTextListener(recyclerAdapter, mainActivity.viewModel)
        )
    }

    override fun loadData() {
        lifecycleScope.launch {
            (activity as? MainActivity)?.run {
                val users = viewModel.getLoadedUsers()
                if(users.isNotEmpty()) {
                    failedLoad = false
                    handler.post {
                        recyclerAdapter.add(users)
                        view?.findViewById<ProgressBar>(R.id.profileProgressBar)?.visibility =
                            View.GONE
                        recyclerView.visibility = View.VISIBLE
                    }
                }
                else {
                    Toast.makeText(context, "Unable to load users.", Toast.LENGTH_SHORT).show()
                    failedLoad = true
                    viewModel.setNetworkState(true)
                }
            }
        }
    }

}