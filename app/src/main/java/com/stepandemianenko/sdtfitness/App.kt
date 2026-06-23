package com.stepandemianenko.sdtfitness

import android.app.Application
import com.stepandemianenko.sdtfitness.data.AppContainer

/**
 * Application entry point. Owns the [AppContainer] holding the app's shared singletons,
 * built once here at process startup.
 */
class App : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
