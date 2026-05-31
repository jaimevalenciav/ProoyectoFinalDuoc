package cl.fleetmanager.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.fleetmanager.shared.models.VehiculoQr
import cl.fleetmanager.shared.repository.RepositorioAsignaciones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class EstadoEscaneo {
    object Escaneando        : EstadoEscaneo()
    object Cargando          : EstadoEscaneo()
    data class VehiculoDetectado(val vehiculo: VehiculoQr)  : EstadoEscaneo()
    data class Asignado(val idAsignacion: String)            : EstadoEscaneo()
    data class Error(val mensaje: String)                    : EstadoEscaneo()
}

class QrScanViewModel(private val repositorio: RepositorioAsignaciones) : ViewModel() {

    private val _estado = MutableStateFlow<EstadoEscaneo>(EstadoEscaneo.Escaneando)
    val estado: StateFlow<EstadoEscaneo> = _estado

    fun alEscanearQr(codigoQr: String) {
        if (_estado.value is EstadoEscaneo.Cargando) return
        viewModelScope.launch {
            _estado.value = EstadoEscaneo.Cargando
            repositorio.validarQr(codigoQr)
                .onSuccess { _estado.value = EstadoEscaneo.VehiculoDetectado(it) }
                .onFailure { _estado.value = EstadoEscaneo.Error(it.message ?: "QR inválido") }
        }
    }

    fun confirmarAsignacion(idConductor: String, idVehiculo: String) {
        viewModelScope.launch {
            _estado.value = EstadoEscaneo.Cargando
            repositorio.asignar(idConductor, idVehiculo)
                .onSuccess { _estado.value = EstadoEscaneo.Asignado(it.id) }
                .onFailure { _estado.value = EstadoEscaneo.Error(it.message ?: "Error al asignar") }
        }
    }

    fun reiniciar() { _estado.value = EstadoEscaneo.Escaneando }
}
