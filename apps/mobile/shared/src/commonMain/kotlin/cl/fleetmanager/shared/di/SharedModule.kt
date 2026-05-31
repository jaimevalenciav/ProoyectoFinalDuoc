package cl.fleetmanager.shared.di

import cl.fleetmanager.shared.api.BffMobileApi
import cl.fleetmanager.shared.repository.AsignacionRepository
import cl.fleetmanager.shared.repository.GpsRepository
import cl.fleetmanager.shared.usecase.TrackingUseCase
import org.koin.dsl.module

fun sharedModule(baseUrl: String, tokenProvider: () -> String?) = module {
    single { BffMobileApi(get(), baseUrl) }
    single { GpsRepository(get()) }
    single { AsignacionRepository(get()) }
    single { TrackingUseCase(get()) }
}
