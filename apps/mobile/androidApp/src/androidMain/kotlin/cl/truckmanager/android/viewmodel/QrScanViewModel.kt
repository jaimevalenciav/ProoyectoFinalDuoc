package cl.truckmanager.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.truckmanager.shared.models.VehiculoQr
import cl.truckmanager.shared.repository.AsignacionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class EstadoEscaneo {
    object Escaneando                                   : EstadoEscaneo()
    object Cargando                                     : EstadoEscaneo()
    data class VehiculoDetectado(val vehiculo: VehiculoQr) : EstadoEscaneo()
    data class VehiculoConfirmado(val vehiculo: VehiculoQr): EstadoEscaneo()
    data class Error(val mensaje: String)               : EstadoEscaneo()
}

class QrScanViewModel(private val repositorio: AsignacionRepository) : ViewModel() {

    private val _estado = MutableStateFlow<EstadoEscaneo>(EstadoEscaneo.Escaneando)
    val estado: StateFlow<EstadoEscaneo> = _estado

    /** ID del vehículo esperado según el servicio asignado. Si está vacío, acepta cualquiera. */
    private var vehiculoEsperadoId: String = ""

    fun preparar(vehiculoEsperadoId: String) {
        this.vehiculoEsperadoId = vehiculoEsperadoId
        _estado.value = EstadoEscaneo.Escaneando
    }

    fun alEscanearQr(codigoQr: String) {
        if (_estado.value is EstadoEscaneo.Cargando) return
        viewModelScope.launch {
            _estado.value = EstadoEscaneo.Cargando
            repositorio.validarQr(codigoQr)
                .onSuccess { vehiculo ->
                    if (vehiculoEsperadoId.isNotBlank() && vehiculo.vehiculoId != vehiculoEsperadoId) {
                        _estado.value = EstadoEscaneo.Error(
                            "Este QR no corresponde al camión asignado.\nEsperado: camión del servicio.\nDetectado: ${vehiculo.placa}"
                        )
                    } else {
                        _estado.value = EstadoEscaneo.VehiculoDetectado(vehiculo)
                    }
                }
                .onFailure { _estado.value = EstadoEscaneo.Error(it.message ?: "QR inválido") }
        }
    }

    /** El conductor confirma que el vehículo detectado es el correcto. */
    fun confirmarVehiculo(vehiculo: VehiculoQr) {
        _estado.value = EstadoEscaneo.VehiculoConfirmado(vehiculo)
    }

    fun reiniciar() { _estado.value = EstadoEscaneo.Escaneando }
}
