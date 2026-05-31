package cl.fleetmanager.shared.models

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(val accessToken: String, val conductorId: String, val nombre: String)

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
data class GpsTrackResponse(val id: String, val recordedAt: String)

@Serializable
data class ApiError(val message: String, val code: String? = null)
