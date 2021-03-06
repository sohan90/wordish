package com.conversant.app.wordish.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.conversant.app.wordish.data.xml.WordThemeDataXmlLoader
import com.conversant.app.wordish.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Database(entities = [Word::class, GameStatus::class, ScoreBoard::class, TopScore::class,
    WordDefinition::class], version = 2)

abstract class GameDatabase : RoomDatabase() {
    abstract val wordDataSource: WordDataSource
    abstract val gameStatusSource: GameStatusSource
    abstract val scoreBoard: ScoreBoardDataSource
    abstract val topScoreSource: TopScoreSource
    abstract val wordDefinitionSource: WordDefinitionSource

    companion object {
        private const val DB_NAME = "game_data.db"
        private var INSTANCE: GameDatabase? = null
        fun getInstance(context: Context): GameDatabase {
            if (INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(context, GameDatabase::class.java, DB_NAME).
                    fallbackToDestructiveMigration().
                build()
            }
            return INSTANCE!!
        }

        suspend fun prepopulateDatabase(context: Context) {
            withContext(Dispatchers.IO) {
                val dataXmlLoader = WordThemeDataXmlLoader(context)
                val gameDb = getInstance(context)
                gameDb.wordDataSource.insertAll(dataXmlLoader.words)
                dataXmlLoader.release()
            }
        }

        suspend fun loadDefinition(context: Context) {
            withContext(Dispatchers.IO) {
                val dataXmlLoader = WordThemeDataXmlLoader(context)
                dataXmlLoader.loadDefinition()
            }
        }
    }
}