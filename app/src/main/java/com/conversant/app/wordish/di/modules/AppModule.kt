package com.conversant.app.wordish.di.modules

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.conversant.app.wordish.features.SoundPlayer
import com.conversant.app.wordish.features.settings.Preferences
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class AppModule(private val app: Application) {
    @Provides
    @Singleton
    fun provideApp(): Application {
        return app
    }

    @Provides
    @Singleton
    fun provideContext(): Context {
        return app
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    @Provides
    @Singleton
    fun providePreference(context: Context, sharedPreferences: SharedPreferences): Preferences {
        return Preferences(context, sharedPreferences)
    }

    @Provides
    @Singleton
    fun provideSoundPool(context: Context, preferences: Preferences): SoundPlayer {
        return SoundPlayer(context, preferences)
    }

}