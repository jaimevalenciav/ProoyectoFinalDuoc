package cl.fleetmanager.shared.repository

import cl.fleetmanager.shared.api.BffMobileApi
import cl.fleetmanager.shared.models.AsignacionRequest
import cl.fleetmanager.shared.models.AsignacionResponse
import cl.fleetmanager.shared.models.VehiculoQr

class AsignacionRepository(private val api: BffMobileApi) {

    suspend fun validarQr(qrCode: String): Result<VehiculoQr> =
        runCatching { api.validarQr(qrCode) }

    suspend fun asignar(conductorId: String, vehiculoId: String): Result<AsignacionResponse> =
        runCatching { api.crearAsignacion(AsignacionRequest(conductorId, vehiculoId)) }

    suspend fun finalizar(asignacionId: String): Result<AsignacionResponse> =
        runCatching { api.finalizarAsignacion(asignacionId) }
}
