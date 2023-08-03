package com.example.githubusers.viewModels

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.ViewModel
import com.example.githubusers.interfaces.SearchModeChanger
import com.example.githubusers.models.User
import com.example.githubusers.models.databases.NotesDatabase
import com.example.githubusers.repositories.NetworkRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel: ViewModel(), SearchModeChanger {
    var lifecycleScope: LifecycleCoroutineScope? = null
    var fragmentManager: FragmentManager? = null
    private var networkRepository = NetworkRepository()
    private val _networkLost = MutableStateFlow(false)

    val networkLost: StateFlow<Boolean>
        get() = _networkLost

    fun setNetworkState(value: Boolean) {
        _networkLost.value = value
    }

    var loadMoreUsersCallback: (() -> Unit)? = null

    var database: NotesDatabase?
        get() = networkRepository.database
        set(value) {
            networkRepository.database = value
        }

    private val _isSearchMode = MutableStateFlow(false)
    private val _isDarkTheme = MutableStateFlow(false)

    val isDarkTheme: StateFlow<Boolean>
        get() = _isDarkTheme

    suspend fun getLoadedUsers() = networkRepository.getLoadedUsers()

    fun getMoreUsers(whereAssign: (List<User>) -> Unit, ifFail: (() -> Unit)? = null) {
        lifecycleScope?.launch {
            whereAssign(networkRepository.loadMoreUsers(ifFail))
        }
    }

    suspend fun getUserProfile(username: String) = networkRepository.getUserProfile(username)

    suspend fun getUsersByLogins(usernames: List<String>) = networkRepository.getUsersByLogins(usernames)

    override val isSearchMode: StateFlow<Boolean>
        get() = _isSearchMode

    override fun setSearchMode(value: Boolean) {
        _isSearchMode.value = value
    }

    fun setDarkTheme(value: Boolean) {
        _isDarkTheme.value = value
    }
}