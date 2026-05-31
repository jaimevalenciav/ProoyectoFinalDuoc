package cl.fleetmanager.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import cl.fleetmanager.android.ui.home.PantallaInicio
import cl.fleetmanager.android.ui.login.PantallaLogin
import cl.fleetmanager.android.ui.qrscan.PantallaEscaneoQr
import cl.fleetmanager.android.ui.theme.TemaFleetManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TemaFleetManager { NavegacionApp() }
        }
    }
}

@Composable
private fun NavegacionApp() {
    var pantalla      by remember { mutableStateOf("login") }
    var idConductor   by remember { mutableStateOf("") }
    var nombreConductor by remember { mutableStateOf("") }
    var tokenAcceso   by remember { mutableStateOf("") }
    var idVehiculo    by remember { mutableStateOf("") }
    var placaVehiculo by remember { mutableStateOf("") }
    var idAsignacion  by remember { mutableStateOf("") }

    when (pantalla) {
        "login" -> PantallaLogin(
            alIniciarSesion = { nombre, token ->
                nombreConductor = nombre
                tokenAcceso     = token
                pantalla = "inicio"
            }
        )
        "inicio" -> PantallaInicio(
            idConductor     = idConductor,
            nombreConductor = nombreConductor,
            idVehiculo      = idVehiculo,
            placaVehiculo   = placaVehiculo,
            idAsignacion    = idAsignacion,
            alEscanearQr    = { pantalla = "escaneo_qr" },
            alCerrarSesion  = { idConductor = ""; idVehiculo = ""; pantalla = "login" },
        )
        "escaneo_qr" -> PantallaEscaneoQr(
            idConductor = idConductor,
            alAsignar   = { aId, placa ->
                idAsignacion  = aId
                placaVehiculo = placa
                pantalla = "inicio"
            }
        )
    }
}
