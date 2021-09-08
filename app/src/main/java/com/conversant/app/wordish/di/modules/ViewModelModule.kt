package com.conversant.app.wordish.di.modules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.conversant.app.wordish.di.ViewModelFactory
import com.conversant.app.wordish.di.ViewModelKey
import com.conversant.app.wordish.features.gamehistory.GameHistoryViewModel
import com.conversant.app.wordish.features.gameover.GameOverViewModel
import com.conversant.app.wordish.features.gameplay.GamePlayViewModel
import com.conversant.app.wordish.features.gamethemeselector.ThemeSelectorViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class ViewModelModule {
    @Binds
    abstract fun provideViewModelFactory(viewModelFactory: ViewModelFactory): ViewModelProvider.Factory

    @Binds
    @IntoMap
    @ViewModelKey(GameHistoryViewModel::class)
    abstract fun gameHistoryViewModel(vm: GameHistoryViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(GameOverViewModel::class)
    abstract fun gameOverViewModel(vm: GameOverViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(GamePlayViewModel::class)
    abstract fun gamePlayViewModel(vm: GamePlayViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ThemeSelectorViewModel::class)
    abstract fun themeSelectorViewModel(vm: ThemeSelectorViewModel): ViewModel
}