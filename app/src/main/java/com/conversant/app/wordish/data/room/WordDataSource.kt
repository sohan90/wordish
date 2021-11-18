package com.conversant.app.wordish.data.room

import androidx.room.*
import com.conversant.app.wordish.model.Word
import io.reactivex.Flowable
import io.reactivex.Maybe

@Dao
interface WordDataSource {
    @Query("SELECT * FROM words WHERE LENGTH(string) < :maxChar")
    fun getWords(maxChar: Int): Flowable<List<Word>>

    @Query("SELECT * FROM words WHERE LENGTH(string) <= :maxChar")
    fun getWordsMayBe(maxChar: Int): Maybe<List<Word>>

    @Query("SELECT * FROM words WHERE game_theme_id=:themeId AND LENGTH(string) < :maxChar")
    fun getWords(themeId: Int, maxChar: Int): Flowable<List<Word>>

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

}