package com.example.githubusers.retrofit

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import retrofit2.Retrofit
import retrofit2.create

private val mediaType = MediaType.get("application/json")

val client: RetrofitApi = Retrofit.Builder()
    .baseUrl("https://api.github.com/")
    .addConverterFactory(Json.asConverterFactory(mediaType))
    .build()
    .create(RetrofitApi::class.java)