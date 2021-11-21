package com.conversant.app.wordish.data.room

import androidx.room.*
import com.conversant.app.wordish.model.TopScore

@Dao
interface TopScoreSource {
    @Query("SELECT * FROM top_score")
    suspend fun getTopScore(): TopScore?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(topScore: TopScore)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(topScore: TopScore)

    @Query("DELETE FROM top_score")
    suspend fun deleteAll()
}