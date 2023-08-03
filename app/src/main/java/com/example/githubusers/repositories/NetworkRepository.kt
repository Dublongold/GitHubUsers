package com.example.githubusers.repositories

import com.example.githubusers.models.*
import com.example.githubusers.models.databases.NotesDatabase
import com.example.githubusers.retrofit.client
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class NetworkRepository {
    private var lastUserId = 0
    private var loadedUsers: List<User>? = null
    private var inProcess: Boolean = false
    private var mutex: Mutex = Mutex()
    var database: NotesDatabase? = null

    suspend fun getLoadedUsers(): List<User> {
        mutex.withLock {
            return try {
                if (loadedUsers == null) {
                    val response = client.getUsers(lastUserId)
                    if (response.isSuccessful) {
                        response.body()!!.apply {
                            lastUserId = this[this.size - 1].id
                            loadedUsers = this@apply
                        }
                    } else {
                        emptyList()
                    }
                } else {
                    loadedUsers!!
                }
            } catch (e: java.net.UnknownHostException) {
                emptyList()
            }
            catch(e: java.net.SocketTimeoutException) {
                emptyList()
            }
        }

    }
    suspend fun loadMoreUsers(ifFail: (() -> Unit)?): List<User> {
        mutex.withLock {
            return try {
                val response = client.getUsers(lastUserId)
                if (response.isSuccessful) {
                    val users = response.body()!!
                    lastUserId = users[users.size - 1].id
                    users
                } else {
                    emptyList()
                }
            } catch (e: java.net.UnknownHostException) {
                ifFail?.invoke()
                emptyList()
            }
            catch(e: java.net.SocketTimeoutException) {
                ifFail?.invoke()
                emptyList()
            }
        }
    }

    suspend fun getUserProfile(username: String, withLock: Boolean = true): UserProfile? {
        val action = suspend {
            try {
                val responseForUser = client.getUserProfile(username)
                if (responseForUser.isSuccessful) responseForUser.body()!! else null
            } catch (e: java.net.UnknownHostException) {
                null
            } catch (e: java.net.SocketTimeoutException) {
                null
            }
        }
        return if(withLock) {
            mutex.withLock {
                action()
            }
        }
        else {
            action()
        }
    }

    suspend fun getUsersByLogins(usernames: List<String>): List<User?> {
        mutex.withLock {
            return try {
                val result = mutableListOf<User?>()

                for (username in usernames) {
                    result.add(getUserProfile(username, withLock = false)?.toUser(),)
                }
                if (result.any { it == null } && result.none { it != null }) {
                    emptyList<User>()
                }
                result
            }
            catch (e: java.net.UnknownHostException) {
                emptyList()
            }
            catch(e: java.net.SocketTimeoutException) {
                emptyList()
            }

        }
    }
}