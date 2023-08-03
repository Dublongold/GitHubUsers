package com.example.githubusers.interfaces

interface CanLoadData {
    var failedLoad: Boolean
    fun loadData()
}