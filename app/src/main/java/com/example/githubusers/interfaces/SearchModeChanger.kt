package com.example.githubusers.interfaces

import kotlinx.coroutines.flow.StateFlow

interface SearchModeChanger {
    val isSearchMode: StateFlow<Boolean>
    fun setSearchMode(value: Boolean)
}