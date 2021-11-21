package com.conversant.app.wordish.di.modules

import android.content.Context
import com.conversant.app.wordish.data.room.*
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class DataSourceModule {
    @Provides
    @Singleton
    fun provideGameDatabase(context: Context): GameDatabase {
        return GameDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideWordDataSource(gameDatabase: GameDatabase): WordDataSource {
        return gameDatabase.wordDataSource
    }



    @Provides
    @Singleton
    fun provideGameStatusSource(gameDatabase: GameDatabase): GameStatusSource {
        return gameDatabase.gameStatusSource
    }

    @Provides
    @Singleton
    fun provideScoreBoardSource(gameDatabase: GameDatabase): ScoreBoardDataSource {
        return gameDatabase.scoreBoard
    }

    @Provides
    @Singleton
    fun provideTopScoreSource(gameDatabase: GameDatabase): TopScoreSource {
        return gameDatabase.topScoreSource
    }
}