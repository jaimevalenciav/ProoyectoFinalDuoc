package cl.fleetmanager.android.di

import cl.fleetmanager.android.auth.GestorAutenticacion
import cl.fleetmanager.android.viewmodel.HomeViewModel
import cl.fleetmanager.android.viewmodel.LoginViewModel
import cl.fleetmanager.android.viewmodel.QrScanViewModel
import cl.fleetmanager.shared.api.createHttpClient
import cl.fleetmanager.shared.di.sharedModule
import io.ktor.client.engine.android.*
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

private const val URL_BFF = "http://10.0.2.2:8081/api/v1"  // emulador → localhost

val moduloApp = module {
    single { GestorAutenticacion(androidContext()) }
    single {
        val gestorAuth: GestorAutenticacion = get()
        createHttpClient(Android.create()) { null }
    }
    viewModel { LoginViewModel(get()) }
    viewModel { HomeViewModel(get()) }
    viewModel { QrScanViewModel(get()) }
}

fun todosLosModulos(proveedorToken: () -> String?) = listOf(
    moduloApp,
    sharedModule(URL_BFF, proveedorToken),
)
