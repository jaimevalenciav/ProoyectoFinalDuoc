package cl.truckmanager.shared.di

import cl.truckmanager.shared.api.BffMobileApi
import cl.truckmanager.shared.repository.AsignacionRepository
import cl.truckmanager.shared.repository.GpsRepository
import cl.truckmanager.shared.usecase.TrackingUseCase
import org.koin.dsl.module

fun sharedModule(baseUrl: String, tokenProvider: () -> String?) = module {
    single { BffMobileApi(get(), baseUrl) }
    single { GpsRepository(get()) }
    single { AsignacionRepository(get()) }
    single { TrackingUseCase(get()) }
}
