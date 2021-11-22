package com.conversant.app.wordish.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.conversant.app.wordish.model.WordDefinition
import org.jetbrains.annotations.NotNull

@Dao
interface WordDefinitionSource {
    @Query("SELECT used_words FROM word_definition")
    suspend fun getUsedWordList(): List<String>?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(@NotNull word: List<WordDefinition>)

    @Query("DELETE FROM word_definition")
    suspend fun deleteAll()
}