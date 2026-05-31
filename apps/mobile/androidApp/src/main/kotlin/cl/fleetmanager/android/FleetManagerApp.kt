package cl.fleetmanager.android

import android.app.Application
import cl.fleetmanager.android.di.todosLosModulos
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class FleetManagerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@FleetManagerApp)
            modules(todosLosModulos { null })
        }
    }
}
