package cl.fleetmanager.android.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.fleetmanager.android.auth.GestorAutenticacion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class EstadoLogin {
    object Inactivo  : EstadoLogin()
    object Cargando  : EstadoLogin()
    data class Exitoso(val nombreConductor: String, val tokenAcceso: String) : EstadoLogin()
    data class Error(val mensaje: String) : EstadoLogin()
}

class LoginViewModel(private val gestorAuth: GestorAutenticacion) : ViewModel() {

    private val _estado = MutableStateFlow<EstadoLogin>(EstadoLogin.Inactivo)
    val estado: StateFlow<EstadoLogin> = _estado

    fun iniciarSesion(actividad: Activity) {
        viewModelScope.launch {
            _estado.value = EstadoLogin.Cargando
            runCatching { gestorAuth.iniciarSesion(actividad) }
                .onSuccess { resultado ->
                    _estado.value = EstadoLogin.Exitoso(
                        nombreConductor = resultado.account.username,
                        tokenAcceso     = resultado.accessToken,
                    )
                }
                .onFailure { _estado.value = EstadoLogin.Error(it.message ?: "Error de autenticación") }
        }
    }
}
