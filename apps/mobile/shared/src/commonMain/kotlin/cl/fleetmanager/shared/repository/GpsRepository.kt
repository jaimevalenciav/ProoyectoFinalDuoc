package cl.fleetmanager.shared.repository

import cl.fleetmanager.shared.api.BffMobileApi
import cl.fleetmanager.shared.models.GpsTrackRequest
import cl.fleetmanager.shared.models.GpsTrackResponse

class GpsRepository(private val api: BffMobileApi) {

    suspend fun enviarPosicion(
        conductorId: String,
        vehiculoId: String,
        latitud: Double,
        longitud: Double,
        velocidad: Double?,
        precision: Double?,
    ): Result<GpsTrackResponse> = runCatching {
        api.enviarPosicion(
            GpsTrackRequest(
                conductorId = conductorId,
                vehiculoId  = vehiculoId,
                latitud     = latitud,
                longitud    = longitud,
                velocidad   = velocidad,
                precision   = precision,
                recordedAt  = currentIsoTime(),
            )
        )
    }

    private fun currentIsoTime(): String {
        // Platform-specific — implemented via expect/actual or using epochMillis
        return kotlinx.datetime.Clock.System.now().toString()
    }
}
