package cl.fleetmanager.shared.api

import cl.fleetmanager.shared.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class BffMobileApi(private val client: HttpClient, private val baseUrl: String) {

    suspend fun validarQr(qrCode: String): VehiculoQr {
        return client.get("$baseUrl/mobile/qr/validar") {
            parameter("qrCode", qrCode)
        }.body()
    }

    suspend fun crearAsignacion(req: AsignacionRequest): AsignacionResponse {
        return client.post("$baseUrl/mobile/asignaciones") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }.body()
    }

    suspend fun finalizarAsignacion(asignacionId: String): AsignacionResponse {
        return client.post("$baseUrl/mobile/asignaciones/$asignacionId/finalizar").body()
    }

    suspend fun enviarPosicion(track: GpsTrackRequest): GpsTrackResponse {
        return client.post("$baseUrl/mobile/gps/track") {
            contentType(ContentType.Application.Json)
            setBody(track)
        }.body()
    }
}
