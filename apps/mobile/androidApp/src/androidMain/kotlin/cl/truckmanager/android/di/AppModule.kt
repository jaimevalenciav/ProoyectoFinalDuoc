package cl.truckmanager.android.di

import cl.truckmanager.android.auth.GestorAutenticacion
import cl.truckmanager.android.auth.TokenHolder
import cl.truckmanager.android.gps.GpsEstadoRepositorio
import cl.truckmanager.android.viewmodel.HomeViewModel
import cl.truckmanager.android.viewmodel.LoginViewModel
import cl.truckmanager.android.viewmodel.QrScanViewModel
import cl.truckmanager.shared.api.BffMobileApi
import cl.truckmanager.shared.api.createHttpClient
import cl.truckmanager.shared.di.sharedModule
import io.ktor.client.engine.android.*
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

private const val URL_BFF = "http://localhost:8080/api/v1"  // túnel USB vía adb reverse

val moduloApp = module {
    single { GestorAutenticacion(androidContext()) }
    single {
        createHttpClient(
            engine        = Android.create(),
            tokenProvider = { TokenHolder.accessToken.takeIf { it.isNotBlank() } },
        )
    }
    single { BffMobileApi(get(), URL_BFF) }
    single { GpsEstadoRepositorio() }
    viewModel { LoginViewModel(get(), get()) }
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { QrScanViewModel(get()) }
}

fun todosLosModulos(proveedorToken: () -> String?) = listOf(
    moduloApp,
    sharedModule(URL_BFF, proveedorToken),
)
