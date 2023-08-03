package com.example.githubusers.models.databases.daos

import androidx.room.*
import com.example.githubusers.models.Note

@Dao
interface NotesDao {
    @Insert
    fun insert(vararg notes: Note)

    @Update
    fun update(note: Note)

    @Query("select * from notes where login = :login")
    fun getNotesByLogin(login: String): List<Note>

    @Query("select * from notes where lower(login) = :query or lower(text) like '%'||:query||'%'")
    fun getNotesByQuery(query: String): List<Note>
}