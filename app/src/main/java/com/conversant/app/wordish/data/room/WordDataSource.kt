package com.conversant.app.wordish.data.room

import androidx.room.*
import com.conversant.app.wordish.model.Word
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single

@Dao
interface WordDataSource {
    @Query("SELECT * FROM words WHERE LENGTH(string) < :maxChar")
    fun getWords(maxChar: Int): Flowable<List<Word>>

    @Query("SELECT * FROM words WHERE LENGTH(string) <= :maxChar")
    fun getWordsMayBe(maxChar: Int): Maybe<List<Word>>

    @Query("SELECT * FROM words WHERE game_theme_id=:themeId AND LENGTH(string) < :maxChar")
    fun getWords(themeId: Int, maxChar: Int): Flowable<List<Word>>

    @Query("SELECT count(*) FROM words WHERE length(string) < :maxChar")
    fun getWordsCount(maxChar: Int): Single<Int>

    @Query("SELECT count(*) FROM words WHERE game_theme_id=:themeId AND length(string) < :maxChar")
    fun getWordsCount(themeId: Int, maxChar: Int): Single<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAll(words: List<Word>)

    @Query("DELETE FROM words")
    suspend fun deleteAll()


    @Query("SELECT string FROM words WHERE used_word")
    suspend fun getUsedWordsList(): List<String>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateWordForUsedWord(list: List<Word>)
}