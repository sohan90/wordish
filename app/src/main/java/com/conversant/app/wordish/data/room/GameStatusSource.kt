package com.conversant.app.wordish.data.room

import androidx.room.*
import com.conversant.app.wordish.model.GameStatus

@Dao
interface GameStatusSource {
    @Query("SELECT * FROM game_status")
    suspend fun getGameStatus(): List<GameStatus>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(words: List<GameStatus>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(words: GameStatus)

    @Query("DELETE FROM game_status")
    suspend fun deleteAll()
}