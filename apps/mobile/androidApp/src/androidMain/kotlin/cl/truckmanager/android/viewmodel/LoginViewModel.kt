package cl.truckmanager.android.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.truckmanager.android.auth.GestorAutenticacion
import cl.truckmanager.android.auth.TokenHolder
import cl.truckmanager.shared.api.BffMobileApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class EstadoLogin {
    object Inactivo  : EstadoLogin()
    object Cargando  : EstadoLogin()
    data class Exitoso(
        val conductorId:     String,
        val nombreConductor: String,
        val tokenAcceso:     String,
    ) : EstadoLogin()
    data class Error(val mensaje: String) : EstadoLogin()
}

class LoginViewModel(
    private val gestorAuth: GestorAutenticacion,
    private val api:        BffMobileApi,
) : ViewModel() {

    private val _estado = MutableStateFlow<EstadoLogin>(EstadoLogin.Inactivo)
    val estado: StateFlow<EstadoLogin> = _estado

    init { verificarSesionExistente() }

    private fun verificarSesionExistente() {
        viewModelScope.launch {
            _estado.value = EstadoLogin.Cargando
            val resultado = gestorAuth.obtenerSesionExistente()
            _estado.value = if (resultado != null) {
                cargarPerfil(resultado.accessToken)
            } else {
                EstadoLogin.Inactivo
            }
        }
    }

    fun iniciarSesion(actividad: Activity) {
        viewModelScope.launch {
            _estado.value = EstadoLogin.Cargando
            runCatching { gestorAuth.iniciarSesion(actividad) }
                .onSuccess { resultado ->
                    _estado.value = cargarPerfil(resultado.accessToken)
                }
                .onFailure {
                    _estado.value = EstadoLogin.Error(it.message ?: "Error de autenticación")
                }
        }
    }

    /** Llama al backend con el access token para obtener nombre e ID del conductor. */
    private suspend fun cargarPerfil(token: String): EstadoLogin {
        TokenHolder.accessToken = token   // disponible para todos los requests HTTP
        return runCatching { api.obtenerPerfil() }
            .fold(
                onSuccess = { perfil ->
                    EstadoLogin.Exitoso(
                        conductorId     = perfil.conductorId,
                        nombreConductor = perfil.nombre,
                        tokenAcceso     = token,
                    )
                },
                onFailure = {
                    EstadoLogin.Error("No se encontró perfil de conductor: ${it.message}")
                }
            )
    }
}
