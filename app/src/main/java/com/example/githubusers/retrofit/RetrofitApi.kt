package com.example.githubusers.retrofit

import com.example.githubusers.models.*
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RetrofitApi {
    @GET("users")
    suspend fun getUsers(@Query("since") since: Int): Response<List<User>>

    @GET("users/{username}")
    suspend fun getUserProfile(@Path("username") username: String): Response<UserProfile>
}