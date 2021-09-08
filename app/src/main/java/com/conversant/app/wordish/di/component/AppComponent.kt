package com.conversant.app.wordish.di.component

import com.conversant.app.wordish.di.modules.AppModule
import com.conversant.app.wordish.di.modules.DataSourceModule
import com.conversant.app.wordish.di.modules.ViewModelModule
import com.conversant.app.wordish.features.FullscreenActivity
import com.conversant.app.wordish.features.gamehistory.GameHistoryActivity
import com.conversant.app.wordish.features.gameover.GameOverActivity
import com.conversant.app.wordish.features.gameplay.GamePlayActivity
import com.conversant.app.wordish.features.gamethemeselector.ThemeSelectorActivity
import com.conversant.app.wordish.features.mainmenu.MainMenuActivity
import dagger.Component
import javax.inject.Singleton

/**
 * Created by abdularis on 18/07/17.
 */
@Singleton
@Component(modules = [AppModule::class, DataSourceModule::class, ViewModelModule::class])
interface AppComponent {
    fun inject(activity: GamePlayActivity)
    fun inject(activity: MainMenuActivity)
    fun inject(activity: GameOverActivity)
    fun inject(activity: FullscreenActivity)
    fun inject(activity: GameHistoryActivity)
    fun inject(activity: ThemeSelectorActivity)
}