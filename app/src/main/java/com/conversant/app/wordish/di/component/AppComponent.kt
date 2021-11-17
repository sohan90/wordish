package com.conversant.app.wordish.di.component

import com.conversant.app.wordish.custom.LetterBoard
import com.conversant.app.wordish.di.modules.AppModule
import com.conversant.app.wordish.di.modules.DataSourceModule
import com.conversant.app.wordish.di.modules.ViewModelModule
import com.conversant.app.wordish.features.*
import com.conversant.app.wordish.features.gamehistory.GameHistoryActivity
import com.conversant.app.wordish.features.gameover.GameOverActivity
import com.conversant.app.wordish.features.gameplay.GamePlayActivity
import dagger.Component
import javax.inject.Singleton

/**
 * Created by abdularis on 18/07/17.
 */
@Singleton
@Component(modules = [AppModule::class, DataSourceModule::class, ViewModelModule::class])
interface AppComponent {
    fun inject(activity:SplashScreenActivity)
    fun inject(activity: GamePlayActivity)
    fun inject(activity: GameOverActivity)
    fun inject(activity: FullscreenActivity)
    fun inject(activity: GameHistoryActivity)
    fun inject(letterboard: LetterBoard)
    fun inject(dialog: DefinitionInfoDialog)
    fun inject(dialog: SettingsDialog)
    fun inject(dialog: SettingItemDialog)
    fun inject(dialog: GameOverDialogFragment)
}