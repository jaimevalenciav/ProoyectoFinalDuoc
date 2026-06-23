package cl.truckmanager.shared.models

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(val accessToken: String, val conductorId: String, val nombre: String)

@Serializable
data class PerfilResponse(val conductorId: String, val nombre: String, val email: String)

@Serializable
data class VehiculoQr(val vehiculoId: String, val placa: String, val marca: String, val modelo: String)

@Serializable
data class AsignacionRequest(val conductorId: String, val vehiculoId: String)

@Serializable
data class AsignacionResponse(val id: String, val conductorId: String, val vehiculoId: String, val estado: String)

@Serializable
data class GpsTrackRequest(
    val conductorId: String,
    val vehiculoId: String,
    val latitud: Double,
    val longitud: Double,
    val velocidad: Double?,
    val precision: Double?,
    val recordedAt: String,
)

@Serializable
data class GpsTrackResponse(
    val id:         String = "",
    val recordedAt: String = "",
)

@Serializable
data class ApiError(val message: String, val code: String? = null)

@Serializable
data class VehiculoResumen(
    val id: String = "",
    val patente: String = "",
    val marca: String = "",
    val modelo: String = "",
)

@Serializable
data class ServicioMobile(
    val id: String,
    val numServicio: String? = null,
    val origen: String = "",
    val destino: String = "",
    val estado: String = "",
    val vehiculoId: String? = null,
    val conductorId: String? = null,
    val fechaServicio: String? = null,
    val tipoServicio: String? = null,
    val clienteNombre: String? = null,
    val clienteRut: String? = null,
)
