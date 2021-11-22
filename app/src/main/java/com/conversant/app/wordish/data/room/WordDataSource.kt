package com.conversant.app.wordish.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.conversant.app.wordish.model.Word
import io.reactivex.Maybe

@Dao
interface WordDataSource {
    @Query("SELECT * FROM words")
    fun getWordsMayBe(): Maybe<List<Word>>

    @Query("SELECT * FROM words WHERE LENGTH(string) <= :maxChar")
    suspend fun getWordListForMax(maxChar: Int): List<Word>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAll(words: List<Word>)

    @Query("DELETE FROM words")
    suspend fun deleteAll()
}