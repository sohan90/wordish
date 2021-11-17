package com.conversant.app.wordish

import android.app.Application
import com.conversant.app.wordish.di.component.AppComponent
import com.conversant.app.wordish.di.component.DaggerAppComponent
import com.conversant.app.wordish.di.modules.AppModule

class WordishApp : Application() {
    lateinit var appComponent: AppComponent
    override fun onCreate() {
        super.onCreate()
        appComponent = DaggerAppComponent.builder().appModule(AppModule(this)).build()
    }

}