package com.conversant.app.wordish.di.modules

import android.content.Context
import com.conversant.app.wordish.data.room.GameDatabase
import com.conversant.app.wordish.data.room.GameThemeDataSource
import com.conversant.app.wordish.data.room.UsedWordDataSource
import com.conversant.app.wordish.data.room.WordDataSource
import com.conversant.app.wordish.data.sqlite.DbHelper
import com.conversant.app.wordish.data.sqlite.GameDataSource
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by abdularis on 18/07/17.
 */
@Module
class DataSourceModule {
    @Provides
    @Singleton
    fun provideGameDatabase(context: Context): GameDatabase {
        return GameDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideDbHelper(context: Context): DbHelper {
        return DbHelper(context)
    }

    @Provides
    @Singleton
    fun provideGameRoundDataSource(
        dbHelper: DbHelper,
        usedWordDataSource: UsedWordDataSource
    ): GameDataSource {
        return GameDataSource(dbHelper, usedWordDataSource)
    }

    @Provides
    @Singleton
    fun provideGameThemeDataSource(gameDatabase: GameDatabase): GameThemeDataSource {
        return gameDatabase.gameThemeDataSource
    }

    @Provides
    @Singleton
    fun provideWordDataSource(gameDatabase: GameDatabase): WordDataSource {
        return gameDatabase.wordDataSource
    }

    @Provides
    @Singleton
    fun provideUsedWordDataSource(gameDatabase: GameDatabase): UsedWordDataSource {
        return gameDatabase.usedWordDataSource
    }
}