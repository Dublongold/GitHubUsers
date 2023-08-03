package com.example.githubusers.tests

import android.util.Log
import com.example.githubusers.repositories.NetworkRepository
import kotlinx.coroutines.runBlocking
import org.junit.Test

class TestFetchUsersData() {
    @Test
    fun Test1() {
        runBlocking {
            val repository = NetworkRepository()
            val users = repository.getLoadedUsers()
            val usersToo = repository.getLoadedUsers()
            assert(users == usersToo) {
                "Try to get same users from repository, but it is different:\n\nusers: ${users.joinToString()}\nusersToo:${usersToo.joinToString()}"
            }
        }
    }

    @Test
    fun Test2() {
        runBlocking {
            val repository = NetworkRepository()
            val users = repository.getLoadedUsers()
            val nextUsers = repository.loadMoreUsers(null)
            if(users.isEmpty())
                assert(false) {
                    "Users is null"
                }
            assert(users != nextUsers && users.size == nextUsers.size) {
                "Try to load more users, but new users it's the same as loaded users.\n\nusers: ${users.joinToString()}\nnewUsers: ${nextUsers.joinToString()}"
            }
        }
    }
    @Test
    fun Test3() {
        runBlocking {
            val repository = NetworkRepository()
            val users = repository.getLoadedUsers()
            assert(
                try {
                    for (u in users) {
                        repository.getUserProfile(u.login)
                    }
                    true
                } catch (e: Exception) {
                    Log.i("Error", e.toString())
                    false
                }
            ) {
                "Error while trying to get user's profiles"
            }
        }
    }
}