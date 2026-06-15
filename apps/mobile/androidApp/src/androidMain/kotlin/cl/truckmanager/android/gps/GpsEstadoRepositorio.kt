package cl.truckmanager.android.gps

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class UltimaUbicacion(
    val latitud:        Double,
    val longitud:       Double,
    val velocidad:      Double?,
    val precision:      Double?,
    val horaEnvio:      String,
    val recibidoEnBff:  Boolean,
    val errorMensaje:   String? = null,
    val intentos:       Int     = 1,      // contador acumulado de envíos
    val respuestaBff:   String? = null,   // id del track o descripción del error HTTP
)

class GpsEstadoRepositorio {
    private val _ultimaUbicacion = MutableStateFlow<UltimaUbicacion?>(null)
    val ultimaUbicacion: StateFlow<UltimaUbicacion?> = _ultimaUbicacion

    private var contadorEnvios = 0

    fun actualizar(ubicacion: UltimaUbicacion) {
        contadorEnvios++
        _ultimaUbicacion.value = ubicacion.copy(intentos = contadorEnvios)
    }

    fun resetContador() { contadorEnvios = 0 }
}
