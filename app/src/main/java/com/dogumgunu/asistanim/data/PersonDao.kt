package com.dogumgunu.asistanim.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {
    @Query("SELECT * FROM people ORDER BY firstName COLLATE NOCASE, lastName COLLATE NOCASE")
    fun observeAll(): Flow<List<Person>>

    @Query("SELECT * FROM people")
    suspend fun getAll(): List<Person>

    @Query("SELECT * FROM people WHERE id = :id")
    suspend fun getById(id: Long): Person?

    @Insert
    suspend fun insert(person: Person): Long

    @Insert
    suspend fun insertAll(people: List<Person>)

    @Update
    suspend fun update(person: Person)

    @Delete
    suspend fun delete(person: Person)
}
