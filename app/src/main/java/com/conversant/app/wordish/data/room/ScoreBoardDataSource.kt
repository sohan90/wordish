package com.conversant.app.wordish.data.room

import androidx.room.*
import com.conversant.app.wordish.model.ScoreBoard

@Dao
interface ScoreBoardDataSource {
    @Query("SELECT * FROM score_board")
    suspend fun getScoreBoard(): ScoreBoard?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(scoreBoard: ScoreBoard)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(scoreBoard: ScoreBoard)

    @Query("DELETE FROM score_board")
    suspend fun deleteAll()
}