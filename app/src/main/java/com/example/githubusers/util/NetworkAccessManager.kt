package com.example.githubusers.util

import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.drawable.ColorDrawable
import java.net.UnknownHostException

object NetworkAccessManager {
    private val queries: MutableList<StateWithAction> = mutableListOf()
    private var inProcess = false

    suspend fun addQuery(action: suspend () -> Unit) {
        queries.add(StateWithAction(
            state = NetworkAccessState.WAIT,
            action = action))
        performQueries()
    }

    private suspend fun performQueries() {
        if(inProcess) return
        inProcess = true
        while(queries.isNotEmpty()) {
            val query =  queries.first()
            query.state = NetworkAccessState.IN_PROCESS
            try {
                query.action()
                query.state = NetworkAccessState.SUCCESS
                queries.removeFirst()
            }
            catch(e: UnknownHostException) {
                inProcess = false
                query.state = NetworkAccessState.ERROR
                return
            }
        }
        inProcess = false
    }
    data class StateWithAction(
        var state: NetworkAccessState,
        var action: suspend () -> Unit
    )
    enum class NetworkAccessState {
        WAIT,
        IN_PROCESS,
        SUCCESS,
        ERROR
    }
}