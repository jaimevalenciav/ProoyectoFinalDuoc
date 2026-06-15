package cl.truckmanager.android.viewmodel

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.truckmanager.android.auth.GestorAutenticacion
import cl.truckmanager.android.gps.GpsEstadoRepositorio
import cl.truckmanager.android.gps.UltimaUbicacion
import cl.truckmanager.android.service.ServicioRastreoGps
import cl.truckmanager.shared.api.BffMobileApi
import cl.truckmanager.shared.models.ServicioMobile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val TAG      = "HomeViewModel"
private const val PREFS    = "tm_sesion"
private const val KEY_COND = "conductorId"
private const val KEY_SERV = "servicioId"
private const val KEY_VEH  = "vehiculoId"
private const val KEY_EN_CURSO = "enCurso"

data class EstadoInicio(
    val cargando: Boolean           = true,
    val error: String?              = null,
    val nombreConductor: String     = "",
    val servicio: ServicioMobile?   = null,
    val placaAsignada: String       = "",
    val marcaModeloAsignado: String = "",
    val placaVehiculo: String       = "",
    val vehiculoValidado: Boolean   = false,
    val rastreando: Boolean         = false,
    val servicioEnCurso: Boolean    = false,
    val ultimaUbicacion: UltimaUbicacion? = null,
)

class HomeViewModel(
    private val gestorAuth:           GestorAutenticacion,
    private val api:                  BffMobileApi,
    private val gpsEstadoRepositorio: GpsEstadoRepositorio,
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoInicio())
    val estado: StateFlow<EstadoInicio> = _estado

    init {
        viewModelScope.launch {
            gpsEstadoRepositorio.ultimaUbicacion.collect { ubicacion ->
                _estado.value = _estado.value.copy(ultimaUbicacion = ubicacion)
            }
        }
    }

    fun cargar(conductorId: String, nombre: String, contexto: Context) {
        viewModelScope.launch {
            _estado.value = _estado.value.copy(cargando = true, error = null, nombreConductor = nombre)

            // Verificar si el servicio GPS ya está corriendo
            val rastreandoActual = servicioGpsCorriendo(contexto)

            runCatching { api.obtenerServiciosConductor(conductorId) }
                .onSuccess { lista ->
                    Log.d(TAG, "Servicios recibidos: ${lista.map { "${it.id}=${it.estado}" }}")

                    // Priorizar EN_CURSO sobre APROBADO
                    val enCurso  = lista.firstOrNull { it.estado == "EN_CURSO" }
                    val aprobado = lista.firstOrNull { it.estado == "APROBADO" }
                    val servicio = enCurso ?: aprobado
                    val esEnCurso = enCurso != null

                    var placa = ""
                    var marcaModelo = ""
                    val vehiculoId = servicio?.vehiculoId
                    if (vehiculoId != null) {
                        runCatching { api.obtenerVehiculo(vehiculoId) }
                            .onSuccess { v ->
                                placa = v.patente
                                marcaModelo = "${v.marca} ${v.modelo}".trim()
                            }
                    }

                    _estado.value = _estado.value.copy(
                        cargando            = false,
                        servicio            = servicio,
                        placaAsignada       = placa,
                        marcaModeloAsignado = marcaModelo,
                        servicioEnCurso     = esEnCurso,
                        // Si el servicio ya estaba corriendo en background, restaurar rastreando
                        rastreando          = rastreandoActual && esEnCurso,
                    )
                }
                .onFailure { e ->
                    Log.e(TAG, "Error al cargar servicios: ${e.message}")
                    _estado.value = _estado.value.copy(cargando = false, error = e.message)
                }
        }
    }

    fun vehiculoConfirmado(placa: String) {
        _estado.value = _estado.value.copy(placaVehiculo = placa, vehiculoValidado = true)
    }

    fun iniciarServicioYRastreo(contexto: Context, conductorId: String) {
        val servicioId = _estado.value.servicio?.id ?: return
        val vehiculoId = _estado.value.servicio?.vehiculoId ?: return
        viewModelScope.launch {
            runCatching { api.iniciarServicio(servicioId) }
                .onSuccess {
                    _estado.value = _estado.value.copy(servicioEnCurso = true)

                    // Guardar sesión persistente
                    guardarSesion(contexto, conductorId, servicioId, vehiculoId)

                    val intent = Intent(contexto, ServicioRastreoGps::class.java).apply {
                        putExtra(ServicioRastreoGps.EXTRA_CONDUCTOR, conductorId)
                        putExtra(ServicioRastreoGps.EXTRA_VEHICULO, vehiculoId)
                    }
                    contexto.startForegroundService(intent)
                    _estado.value = _estado.value.copy(rastreando = true)
                    Log.d(TAG, "Servicio GPS iniciado")
                }
                .onFailure { e ->
                    _estado.value = _estado.value.copy(error = "Error al iniciar servicio: ${e.message}")
                }
        }
    }

    fun detenerRastreo(contexto: Context) {
        contexto.stopService(Intent(contexto, ServicioRastreoGps::class.java))
        _estado.value = _estado.value.copy(rastreando = false)
        gpsEstadoRepositorio.resetContador()
    }

    fun cerrarSesion(contexto: Context) {
        limpiarSesion(contexto)
        gestorAuth.cerrarSesion()
    }

    // ── SharedPreferences ─────────────────────────────────────────────────────

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun guardarSesion(ctx: Context, conductorId: String, servicioId: String, vehiculoId: String) {
        prefs(ctx).edit()
            .putString(KEY_COND, conductorId)
            .putString(KEY_SERV, servicioId)
            .putString(KEY_VEH, vehiculoId)
            .putBoolean(KEY_EN_CURSO, true)
            .apply()
    }

    private fun limpiarSesion(ctx: Context) {
        prefs(ctx).edit().clear().apply()
    }

    // ── Utilidades ────────────────────────────────────────────────────────────

    @Suppress("DEPRECATION")
    private fun servicioGpsCorriendo(ctx: Context): Boolean {
        val manager = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Int.MAX_VALUE)
            .any { it.service.className == ServicioRastreoGps::class.java.name }
    }
}
