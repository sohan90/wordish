package com.conversant.app.wordish.data.room

import androidx.room.*
import com.conversant.app.wordish.model.Word
import io.reactivex.Flowable
import io.reactivex.Maybe

@Dao
interface WordDataSource {
    @Query("SELECT * FROM words WHERE LENGTH(string) <= :maxChar")
    fun getWords(maxChar: Int): Flowable<List<Word>>


    @Query("SELECT * FROM words")
    fun getWordsMayBe(): Maybe<List<Word>>

    @Query("SELECT * FROM words")
    fun getWords(): Flowable<List<Word>>

    @Query("SELECT * FROM words WHERE LENGTH(string) <= :maxChar")
    suspend fun getWordListForMax(maxChar: Int): List<Word>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAll(words: List<Word>)

    @Query("DELETE FROM words")
    suspend fun deleteAll()

    @Query("SELECT string FROM words WHERE used_word")
    suspend fun getUsedWordsList(): List<String>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateWordForUsedWord(list: List<Word>)

    @Query("SELECT * FROM words WHERE used_word")
    suspend fun getUsedWords(): List<Word>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun resetUserWord(word:List<Word>)

    @Query("SELECT * FROM words WHERE string like :answer" )
    suspend fun getValidWord(answer:String):Word?
}