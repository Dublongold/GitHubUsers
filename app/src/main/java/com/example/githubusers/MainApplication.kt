package com.example.githubusers

import android.app.Application
import androidx.room.Room
import com.example.githubusers.models.databases.NotesDatabase

class MainApplication: Application() {
    val database: NotesDatabase by lazy {
        Room.databaseBuilder(
            this,
            NotesDatabase::class.java,
            "notes.db"
        ).allowMainThreadQueries().build()
    }
}