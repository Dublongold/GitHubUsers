package com.example.githubusers.models.databases

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.githubusers.models.Note
import com.example.githubusers.models.databases.daos.NotesDao

@Database(entities = [Note::class], version = 1)
abstract class NotesDatabase: RoomDatabase() {
    abstract fun getNoteDao(): NotesDao
}