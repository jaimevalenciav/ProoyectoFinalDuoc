package cl.truckmanager.android

import android.app.Application
import cl.truckmanager.android.di.todosLosModulos
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class TruckManagerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@TruckManagerApp)
            modules(todosLosModulos { null })
        }
    }
}
