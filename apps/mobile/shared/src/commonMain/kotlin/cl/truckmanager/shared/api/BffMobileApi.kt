package cl.truckmanager.shared.api

import cl.truckmanager.shared.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

class BffMobileApi(private val client: HttpClient, private val baseUrl: String) {

    suspend fun validarQr(qrCode: String): VehiculoQr {
        val response = client.get("$baseUrl/vehiculos/qr/validar") {
            parameter("qrCode", qrCode)
        }
        if (!response.status.isSuccess()) throw Exception("HTTP ${response.status.value}: ${response.bodyAsText()}")
        return response.body()
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

    suspend fun obtenerPerfil(): PerfilResponse {
        val response = client.get("$baseUrl/mobile/perfil")
        if (!response.status.isSuccess()) {
            val body = response.bodyAsText()
            throw Exception("HTTP ${response.status.value} en /mobile/perfil: $body")
        }
        return response.body()
    }

    suspend fun obtenerVehiculo(vehiculoId: String): VehiculoResumen {
        val response = client.get("$baseUrl/vehiculos/$vehiculoId")
        if (!response.status.isSuccess()) throw Exception("HTTP ${response.status.value}")
        return response.body()
    }

    suspend fun obtenerServiciosConductor(conductorId: String): List<ServicioMobile> {
        val response = client.get("$baseUrl/servicios/conductor/$conductorId")
        if (!response.status.isSuccess()) throw Exception("HTTP ${response.status.value}: ${response.bodyAsText()}")
        return response.body()
    }

    suspend fun iniciarServicio(servicioId: String): ServicioMobile {
        val response = client.patch("$baseUrl/servicios/$servicioId/iniciar")
        if (!response.status.isSuccess()) throw Exception("HTTP ${response.status.value}: ${response.bodyAsText()}")
        return response.body()
    }

    suspend fun enviarPosicion(track: GpsTrackRequest): GpsTrackResponse {
        return client.post("$baseUrl/mobile/gps/track") {
            contentType(ContentType.Application.Json)
            setBody(track)
        }.body()
    }
}
