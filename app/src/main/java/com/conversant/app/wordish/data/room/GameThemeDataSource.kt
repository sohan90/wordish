package com.conversant.app.wordish.data.room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.conversant.app.wordish.features.gamethemeselector.GameThemeItem
import com.conversant.app.wordish.model.GameTheme

@Dao
interface GameThemeDataSource {
    @Query("SELECT *, (SELECT COUNT(*) FROM words WHERE game_theme_id=game_themes.id) as words_count FROM game_themes")
    fun getThemeItemList(): LiveData<List<GameThemeItem>>

    @Insert
    fun insertAll(gameThemes: List<GameTheme>)

    @Query("DELETE FROM game_themes")
    fun deleteAll()
}