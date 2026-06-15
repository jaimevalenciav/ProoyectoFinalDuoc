package cl.truckmanager.shared.repository

import cl.truckmanager.shared.api.BffMobileApi
import cl.truckmanager.shared.models.AsignacionRequest
import cl.truckmanager.shared.models.AsignacionResponse
import cl.truckmanager.shared.models.VehiculoQr

class AsignacionRepository(private val api: BffMobileApi) {

    suspend fun validarQr(qrCode: String): Result<VehiculoQr> =
        runCatching { api.validarQr(qrCode) }

    suspend fun asignar(conductorId: String, vehiculoId: String): Result<AsignacionResponse> =
        runCatching { api.crearAsignacion(AsignacionRequest(conductorId, vehiculoId)) }

    suspend fun finalizar(asignacionId: String): Result<AsignacionResponse> =
        runCatching { api.finalizarAsignacion(asignacionId) }
}
