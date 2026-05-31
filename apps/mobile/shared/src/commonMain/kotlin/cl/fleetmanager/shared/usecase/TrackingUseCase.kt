package cl.fleetmanager.shared.usecase

import cl.fleetmanager.shared.models.GpsTrackResponse
import cl.fleetmanager.shared.repository.GpsRepository

class TrackingUseCase(private val repo: GpsRepository) {

    suspend fun track(
        conductorId: String,
        vehiculoId: String,
        latitud: Double,
        longitud: Double,
        velocidad: Double? = null,
        precision: Double? = null,
    ): Result<GpsTrackResponse> =
        repo.enviarPosicion(conductorId, vehiculoId, latitud, longitud, velocidad, precision)
}
