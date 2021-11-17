package com.conversant.app.wordish.di.modules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.conversant.app.wordish.di.ViewModelFactory
import com.conversant.app.wordish.di.ViewModelKey
import com.conversant.app.wordish.features.gameplay.GamePlayViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class ViewModelModule {
    @Binds
    abstract fun provideViewModelFactory(viewModelFactory: ViewModelFactory): ViewModelProvider.Factory


    @Binds
    @IntoMap
    @ViewModelKey(GamePlayViewModel::class)
    abstract fun gamePlayViewModel(vm: GamePlayViewModel): ViewModel

}