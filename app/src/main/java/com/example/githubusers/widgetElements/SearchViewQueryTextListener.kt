package com.example.githubusers.widgetElements

import android.widget.SearchView
import com.example.githubusers.interfaces.SearchModeChanger
import com.example.githubusers.models.databases.NotesDatabase
import com.example.githubusers.recyclerList.MainRecyclerListAdapter

class SearchViewQueryTextListener(
    private val recyclerAdapter: MainRecyclerListAdapter,
    private val searchModeChanger: SearchModeChanger
): SearchView.OnQueryTextListener {

    override fun onQueryTextSubmit(query: String?): Boolean {
        searchModeChanger.setSearchMode(true)
        if(!query.isNullOrEmpty())
            recyclerAdapter.filter(query)
        else
            recyclerAdapter.setDefaultList()
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        if(newText.isNullOrEmpty()) {
            recyclerAdapter.setDefaultList()
            searchModeChanger.setSearchMode(false)
        }
        return true
    }
}