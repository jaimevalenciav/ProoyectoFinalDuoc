package cl.fleetmanager.android.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import cl.fleetmanager.android.auth.GestorAutenticacion
import cl.fleetmanager.android.service.ServicioRastreoGps
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class EstadoInicio(
    val nombreConductor: String = "",
    val placaVehiculo:   String = "",
    val idAsignacion:    String = "",
    val rastreando:      Boolean = false,
)

class HomeViewModel(private val gestorAuth: GestorAutenticacion) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoInicio())
    val estado: StateFlow<EstadoInicio> = _estado

    fun configurar(nombreConductor: String, placaVehiculo: String, idAsignacion: String) {
        _estado.value = _estado.value.copy(
            nombreConductor = nombreConductor,
            placaVehiculo   = placaVehiculo,
            idAsignacion    = idAsignacion,
        )
    }

    fun iniciarRastreo(contexto: Context, idConductor: String, idVehiculo: String) {
        val intent = Intent(contexto, ServicioRastreoGps::class.java).apply {
            putExtra(ServicioRastreoGps.EXTRA_CONDUCTOR, idConductor)
            putExtra(ServicioRastreoGps.EXTRA_VEHICULO,  idVehiculo)
        }
        contexto.startForegroundService(intent)
        _estado.value = _estado.value.copy(rastreando = true)
    }

    fun detenerRastreo(contexto: Context) {
        contexto.stopService(Intent(contexto, ServicioRastreoGps::class.java))
        _estado.value = _estado.value.copy(rastreando = false)
    }

    fun cerrarSesion() { gestorAuth.cerrarSesion() }
}
