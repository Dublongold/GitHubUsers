package com.example.githubusers.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note (
    @PrimaryKey val login: String,
    @ColumnInfo("text") val text: String)