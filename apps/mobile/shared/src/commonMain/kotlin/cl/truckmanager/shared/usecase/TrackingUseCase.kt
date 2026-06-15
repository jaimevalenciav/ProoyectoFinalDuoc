package cl.truckmanager.shared.usecase

import cl.truckmanager.shared.models.GpsTrackResponse
import cl.truckmanager.shared.repository.GpsRepository

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
